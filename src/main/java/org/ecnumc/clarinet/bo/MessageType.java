package org.ecnumc.clarinet.bo;

import com.google.common.collect.ImmutableMap;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.ecnumc.clarinet.po.Customer;

import java.util.Map;

import static org.ecnumc.clarinet.Clarinet.SYSTEM_CONFIG;

/**
 * 消息类型
 *
 * @author liudongyu
 */
@Getter
@AllArgsConstructor
public enum MessageType {
	DIRECTORY("directory"),
	FILE("file"),
	IMAGE("image"),
	PLAIN("plain");

	private static final Map<String, MessageType> BY_CODE;

	private final String code;

	/**
	 * 根据 Code 获取消息类型
	 * @param code 编码
	 * @return 消息类型
	 */
	public static MessageType byCode(String code) {
		return BY_CODE.get(code);
	}

	/**
	 * 判断消息是否会发生新的会话
	 * @param messageContent 消息内容
	 * @param customer 用户
	 * @return 是否可以发起新会话
	 */
	public boolean canStartSession(String messageContent, Customer customer) {
		return this == DIRECTORY || (messageContent.contains(SYSTEM_CONFIG.getWakeupWord()) && Permission.canStartSession(customer.getPermission()));
	}

	static {
		ImmutableMap.Builder<String, MessageType> builder = ImmutableMap.builder();
		for(MessageType type: values()) {
			builder.put(type.getCode(), type);
		}
		BY_CODE = builder.build();
	}
}
