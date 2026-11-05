package org.ecnumc.clarinet.tool;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.permission.PermissionBehavior;
import io.agentscope.core.permission.PermissionDecision;
import io.agentscope.core.tool.ToolCallParam;
import org.ecnumc.clarinet.bo.Reaction;
import org.ecnumc.clarinet.support.ToolResults;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 发送表情包工具单元测试
 *
 * @author liudongyu
 */
class SendReactionToolTest {
	private final SendReactionTool tool = new SendReactionTool();

	/**
	 * 空表情包应被拒绝
	 */
	@Test
	void testEmptyReactionDenied() {
		PermissionDecision decision = this.tool.checkPermissions(Map.of("reaction", ""), null).block();
		assertEquals(PermissionBehavior.DENY, decision.getBehavior());
	}

	/**
	 * 超长表情包编码应被拒绝
	 */
	@Test
	void testTooLongReactionDenied() {
		PermissionDecision decision = this.tool.checkPermissions(Map.of("reaction", "a".repeat(4096)), null).block();
		assertEquals(PermissionBehavior.DENY, decision.getBehavior());
	}

	/**
	 * 发送合法表情包时应将枚举交给发送器
	 */
	@Test
	void testSendReactionInvokesSender() {
		AtomicReference<Reaction> sent = new AtomicReference<>();
		this.tool.setSender(sent::set);
		ToolResultBlock result = this.tool.callAsync(
				ToolCallParam.builder().input(Map.of("reaction", Reaction.SHOCKED.name())).build()
		).block();
		assertEquals(Reaction.SHOCKED, sent.get());
		assertEquals("发送成功。", ToolResults.text(result));
	}

	/**
	 * 非法表情包编码应返回错误结果
	 */
	@Test
	void testInvalidReactionReturnsError() {
		ToolResultBlock result = this.tool.callAsync(
				ToolCallParam.builder().input(Map.of("reaction", "NOT_A_REACTION")).build()
		).block();
		assertTrue(ToolResults.text(result).startsWith("Error:"));
	}
}
