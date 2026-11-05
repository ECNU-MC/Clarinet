package org.ecnumc.clarinet.tool;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.permission.PermissionBehavior;
import io.agentscope.core.permission.PermissionDecision;
import io.agentscope.core.tool.ToolCallParam;
import org.ecnumc.clarinet.support.ToolResults;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 发送消息工具单元测试
 *
 * @author liudongyu
 */
class SendMessageToolTest {
	private final SendMessageTool tool = new SendMessageTool();

	/**
	 * 空消息应被拒绝
	 */
	@Test
	void testEmptyMessageDenied() {
		PermissionDecision decision = this.tool.checkPermissions(Map.of("message", ""), null).block();
		assertEquals(PermissionBehavior.DENY, decision.getBehavior());
	}

	/**
	 * 超长消息应被拒绝
	 */
	@Test
	void testTooLongMessageDenied() {
		PermissionDecision decision = this.tool.checkPermissions(Map.of("message", "a".repeat(4096)), null).block();
		assertEquals(PermissionBehavior.DENY, decision.getBehavior());
	}

	/**
	 * 长度达到上限的消息应放行
	 */
	@Test
	void testMaxLengthMessageAllowed() {
		PermissionDecision decision = this.tool.checkPermissions(Map.of("message", "a".repeat(4095)), null).block();
		assertEquals(PermissionBehavior.ALLOW, decision.getBehavior());
	}

	/**
	 * 发送消息时应将内容交给发送器
	 */
	@Test
	void testSendMessageInvokesSender() {
		AtomicReference<String> sent = new AtomicReference<>();
		this.tool.setSender(sent::set);
		ToolResultBlock result = this.tool.callAsync(
				ToolCallParam.builder().input(Map.of("message", "你好，主人")).build()
		).block();
		assertEquals("你好，主人", sent.get());
		assertEquals("发送成功。", ToolResults.text(result));
	}
}
