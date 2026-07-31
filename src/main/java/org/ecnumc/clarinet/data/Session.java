package org.ecnumc.clarinet.data;

import io.agentscope.core.message.Msg;
import lombok.Builder;
import lombok.Data;
import org.ecnumc.clarinet.bo.MessageRole;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

/**
 * 完整会话内容
 * @author liudongyu
 */
@Data
@Builder
public class Session {
	private static final DateTimeFormatter TIMESTAMP_FORMATTER =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

	/** 会话 ID */
	private final String id;

	/** 全部消息 */
	private final List<Message> messages;

	/** 创建时间 */
	private final Date createdAt;

	/**
	 * 增加消息
	 * @param message 新消息
	 */
	public void addMessage(Message message) {
		synchronized (this.messages) {
			this.messages.add(message);
		}
	}

	/**
	 * 转化为 agentscope 支持的 Message List 格式
	 *
	 * @return 消息列表
	 */
	public List<Msg> cast() {
		synchronized (this.messages) {
			return this.messages.stream().map(message ->
					Msg.builder()
							.id(message.getId())
							.role(MessageRole.byCode(message.getRole()).cast())
							.textContent("<message uid=\"" + message.getUid() + "\" nickname=\"" + message.getNickname() + "\">" + message.getContent() + "</message>")
							.timestamp(TIMESTAMP_FORMATTER.format(message.getSentAt().toInstant()))
							.build()
			).toList();
		}
	}
}
