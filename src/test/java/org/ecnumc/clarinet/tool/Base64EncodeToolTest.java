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

/**
 * Base64 编码工具单元测试
 *
 * @author liudongyu
 */
class Base64EncodeToolTest {
	private final Base64EncodeTool tool = new Base64EncodeTool();

	/**
	 * 正常编码 UTF-8 文本
	 */
	@Test
	void testEncodeUtf8Text() {
		String plain = "柯拉妮 你好！";
		ToolResultBlock result = this.tool.callAsync(
				ToolCallParam.builder().input(Map.of("text", plain)).build()
		).block();
		assertEquals(
				Base64.getEncoder().encodeToString(plain.getBytes(StandardCharsets.UTF_8)),
				ToolResults.text(result)
		);
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
