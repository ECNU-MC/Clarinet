package org.ecnumc.clarinet.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import javax.annotation.Nullable;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 单条消息内容
 * @author liudongyu
 * @see Session
 */
@Data
@AllArgsConstructor
@Builder
public class Message implements Serializable, Comparable<Message> {
	@Serial
	private static final long serialVersionUID = 1L;

	/** 消息 ID */
	private String id;

	/** 发言人类型：assistant / user / operator */
	private String role;

	/** 消息类型：image / file / plain */
	private String type;

	/** 发言人 ID，当 type 为 assistant 时留空 */
	@Nullable
	private String uid;

	/** 发言内容 */
	private String content;

	/** 发送时间 */
	private Date sentAt;

	@Override
	public int compareTo(Message o) {
		return this.getSentAt().compareTo(o.getSentAt());
	}
}
