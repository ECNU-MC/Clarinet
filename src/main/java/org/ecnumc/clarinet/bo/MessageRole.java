package org.ecnumc.clarinet.bo;

import com.google.common.collect.ImmutableMap;
import io.agentscope.core.message.MsgRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

/**
 * 消息角色
 *
 * @author liudongyu
 */
@Getter
@AllArgsConstructor
public enum MessageRole {
	ASSISTANT("assistant"),
	USER("user"),
	OPERATOR("operator");

	private static final Map<String, MessageRole> BY_CODE;

	private final String code;

	/**
	 * 根据 Code 获取消息类型
	 * @param code 编码
	 * @return 消息类型
	 */
	public static MessageRole byCode(String code) {
		return BY_CODE.get(code);
	}

	/**
	 * 转化为 agentscope 的 role
	 * @return 角色
	 */
	public MsgRole cast() {
		return switch(this) {
			case USER, OPERATOR -> MsgRole.USER;
			case ASSISTANT -> MsgRole.ASSISTANT;
		};
	}

	static {
		ImmutableMap.Builder<String, MessageRole> builder = ImmutableMap.builder();
		for(MessageRole type: values()) {
			builder.put(type.getCode(), type);
		}
		BY_CODE = builder.build();
	}
}
