package org.ecnumc.clarinet.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;

/**
 * 处理消息
 * @author liudongyu
 */
public record MessageHandler(HarnessAgent agent, CustomerRepository customerRepository, SessionRepository sessionRepository) implements HttpHandler {
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
			String messageId = json.getString("message_id");
			String role = json.getString("role");
			String type = json.getString("type");
			String qq = json.getString("user_id");
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
			String repliedMessageContent = repliedMessageJson.getString("content");
			Date repliedSentAt = Date.from(LocalDateTime.parse(json.getString("sent_at"), FORMATTER).atZone(ZoneId.systemDefault()).toInstant());

			Customer customer = this.customerRepository.getOrCreateCustomerByQq(qq);
			Message message = this.sessionRepository.addMessage(messageId, role, type, customer.getUid(), content, sentAt);
			Session session = repliedMessageId == null ? null : this.sessionRepository.getSessionFromMessageId(repliedMessageId);
			if(session == null && MessageType.byCode(type).canStartSession(content, customer)) {
				session = this.sessionRepository.createEmptySession();
				if(repliedMessageId != null && repliedMessageRole != null &&
						repliedMessageQq != null && repliedMessageContent != null) {
					Customer repliedCustomer = this.customerRepository.getOrCreateCustomerByQq(repliedMessageQq);
					Message repliedMessage = new Message(
							repliedMessageId, repliedMessageRole, repliedMessageType,
							repliedCustomer.getUid(), repliedMessageContent, repliedSentAt
					);
					session.addMessage(repliedMessage);
				}
			}
			if(session == null) {
				return;
			}

			session.addMessage(message);
			this.sessionRepository.updateSession(session);
			Msg result = this.agent.call(session.cast()).block();
			if(result != null) {
				ClarinetLogger.info(result.getTextContent());
			}
		} catch(Exception e) {
			ClarinetLogger.error("处理消息失败：", e);
			String error = e.getMessage();
			ret = new JSONObject(Map.of(
					"error", error
			));
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
