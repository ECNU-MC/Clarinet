package org.ecnumc.clarinet.tool;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.permission.PermissionBehavior;
import io.agentscope.core.permission.PermissionDecision;
import io.agentscope.core.tool.ToolCallParam;
import org.ecnumc.clarinet.support.ToolResults;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Base64 解码工具单元测试
 *
 * @author liudongyu
 */
class Base64DecodeToolTest {
	private final Base64DecodeTool tool = new Base64DecodeTool();

	/**
	 * 正常解码 UTF-8 文本
	 */
	@Test
	void testDecodeUtf8Text() {
		String plain = "你好，柯拉妮";
		String encoded = Base64.getEncoder().encodeToString(plain.getBytes(StandardCharsets.UTF_8));
		ToolResultBlock result = this.tool.callAsync(
				ToolCallParam.builder().input(Map.of("text", encoded)).build()
		).block();
		assertEquals(plain, ToolResults.text(result));
	}

	/**
	 * 非法 Base64 输入应返回错误结果
	 */
	@Test
	void testInvalidBase64ReturnsError() {
		ToolResultBlock result = this.tool.callAsync(
				ToolCallParam.builder().input(Map.of("text", "@@not-base64@@")).build()
		).block();
		assertTrue(ToolResults.text(result).startsWith("Error:"));
	}

	/**
	 * 空文本应被拒绝
	 */
	@Test
	void testEmptyTextDenied() {
		PermissionDecision decision = this.tool.checkPermissions(Map.of("text", ""), null).block();
		assertEquals(PermissionBehavior.DENY, decision.getBehavior());
	}
}
