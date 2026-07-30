package org.ecnumc.clarinet.convert;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.ImmutableMap;
import org.ecnumc.clarinet.data.Message;
import org.ecnumc.clarinet.data.Session;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 会话转换器
 *
 * @author liudongyu
 */
public final class SessionConvert {
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	/**
	 * 将单条消息转化为 JSON 字符串供大模型读取
	 * @param message 单条消息
	 * @return 结构化 JSON 字符串
	 */
	public static String toJsonString(Message message) {
		ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
		builder.put("role", message.getRole())
				.put("type", message.getType())
				.put("content", message.getContent())
				.put("sentAt", message.getSentAt().toInstant().atZone(ZoneId.systemDefault()).format(FORMATTER));
		if(message.getUid() != null) {
			builder.put("uid", message.getUid());
		}
		return JSON.toJSONString(builder.build());
	}

	/**
	 * 将会话转化为 JSON 字符串供大模型读取
	 * @param session 单条消息
	 * @return 结构化 JSON 字符串
	 */
	public static String toJsonString(Session session) {
		return JSON.toJSONString(session.getMessages());
	}

	/**
	 * 将单条消息转化为 JSON 字符串落盘
	 * @param message 单条消息
	 * @return 结构化 JSON 字符串
	 */
	public static String toJsonPersistentString(Message message) {
		ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
		builder.put("id", message.getId())
				.put("role", message.getRole())
				.put("type", message.getType())
				.put("content", message.getContent())
				.put("sentAt", String.valueOf(message.getSentAt().getTime()));
		if(message.getUid() != null) {
			builder.put("uid", message.getUid());
		}
		return JSON.toJSONString(builder.build());
	}

	/**
	 * 将会话转化为 JSON 字符串落盘
	 * @param session 单条消息
	 * @return 结构化 JSON 字符串
	 */
	public static String toJsonPersistentString(Session session) {
		return JSON.toJSONString(Map.of(
				"id", session.getId(),
				"messages", session.getMessages(),
				"createdAt", String.valueOf(session.getCreatedAt().getTime())
		));
	}

	private SessionConvert() {
	}
}
