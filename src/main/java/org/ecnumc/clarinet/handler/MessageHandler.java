package org.ecnumc.clarinet.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.harness.agent.HarnessAgent;
import org.ecnumc.clarinet.ClarinetLogger;
import org.ecnumc.clarinet.bo.MessageType;
import org.ecnumc.clarinet.convert.ContentConvert;
import org.ecnumc.clarinet.data.Message;
import org.ecnumc.clarinet.data.Session;
import org.ecnumc.clarinet.po.Customer;
import org.ecnumc.clarinet.repository.CustomerRepository;
import org.ecnumc.clarinet.repository.SessionRepository;
import org.ecnumc.clarinet.tool.SendMessageTool;
import org.ecnumc.clarinet.tool.SendReactionTool;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * 处理消息
 * @author liudongyu
 */
public record MessageHandler(HarnessAgent agent,
							 SendMessageTool sendMessageTool,
							 SendReactionTool sendReactionTool,
							 CustomerRepository customerRepository,
							 SessionRepository sessionRepository) implements HttpHandler {
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		JSONObject ret = new JSONObject();
		int code = 200;
		byte[] response;
		try {
			String req = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
			ClarinetLogger.info(req);
			JSONObject json = JSON.parseObject(req);
			String groupId = json.getString("group_id");
			String messageId = json.getString("message_id");
			String role = json.getString("role");
			String type = json.getString("type");
			String qq = json.getString("user_id");
			String nickname = json.getString("user_name");
			String content = json.getString("content");
			content = ContentConvert.encode(content, this.customerRepository);
			Date sentAt = Date.from(LocalDateTime.parse(json.getString("sent_at"), FORMATTER).atZone(ZoneId.systemDefault()).toInstant());

			JSONObject repliedMessageJson = json.getJSONObject("replied_message");
			if(repliedMessageJson == null) {
				repliedMessageJson = new JSONObject();
			}
			String repliedMessageId = repliedMessageJson.getString("message_id");
			String repliedMessageRole = repliedMessageJson.getString("role");
			String repliedMessageType = repliedMessageJson.getString("type");
			String repliedMessageQq = repliedMessageJson.getString("user_id");
			String repliedMessageNickname = repliedMessageJson.getString("user_name");
			String repliedMessageContent = repliedMessageJson.getString("content");
			Date repliedSentAt = Date.from(LocalDateTime.parse(json.getString("sent_at"), FORMATTER).atZone(ZoneId.systemDefault()).toInstant());

			Customer customer = this.customerRepository.getOrCreateCustomerByQq(qq);
			Message message = this.sessionRepository.addMessage(groupId, messageId, role, type, customer.getUid(), customer.getAppellation(), nickname, content, sentAt);
			Session session = repliedMessageId == null ? null : this.sessionRepository.getSessionFromMessageId(repliedMessageId);
			if(session == null && MessageType.byCode(type).canStartSession(content, customer)) {
				session = this.sessionRepository.createEmptySession();
				if(repliedMessageId != null && repliedMessageRole != null &&
						repliedMessageQq != null && repliedMessageContent != null) {
					Customer repliedCustomer = this.customerRepository.getOrCreateCustomerByQq(repliedMessageQq);
					Message repliedMessage = new Message(
							repliedMessageId, repliedMessageRole, repliedMessageType,
							repliedCustomer.getUid(), repliedCustomer.getAppellation(), repliedMessageNickname, repliedMessageContent, repliedSentAt
					);
					session.addMessage(repliedMessage);
				}
			}
			if(session == null) {
				return;
			}

			session.addMessage(message);
			this.sessionRepository.updateSession(session);

			synchronized (this.agent) {
				JSONArray jsonArray = new JSONArray();
				this.sendMessageTool.setSender(jsonArray::add);
				RuntimeContext context = RuntimeContext.builder()
						.put("groupId", groupId)
						.build();
				Msg result = this.agent.call(session.cast(), context).block();
				if(result != null) {
					ClarinetLogger.info(result.getTextContent());
				}
				this.sendMessageTool.setSender(SendMessageTool.EMPTY);
				this.sendReactionTool.setSender(SendReactionTool.EMPTY);
				ret.put("message", jsonArray);
			}
		} catch(Exception e) {
			ClarinetLogger.error("处理消息失败：", e);
			String error = e.getMessage();
			ret.put("error", error);
			code = 500;
		} finally {
			exchange.getResponseHeaders().set("Content-Type", "application/json");
			response = ret.toJSONString().getBytes();
			exchange.sendResponseHeaders(code, response.length);
			try (OutputStream os = exchange.getResponseBody()) {
				os.write(response);
			} catch (Exception e) {
				ClarinetLogger.error("回复响应失败：", e);
			}
		}
	}
}
