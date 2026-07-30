package org.ecnumc.clarinet.tool;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.permission.PermissionDecision;
import io.agentscope.core.tool.ToolBase;
import io.agentscope.core.tool.ToolCallParam;
import org.apache.commons.lang3.StringUtils;
import org.ecnumc.clarinet.ClarinetLogger;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Base64 编码工具
 *
 * @author liudongyu
 */
public final class Base64EncodeTool extends ToolBase {
	/**
	 * 构造函数
	 */
	public Base64EncodeTool() {
		super(
				ToolBase.builder()
						.name("base64_encode")
						.description("将输入文本进行 Base64 编码")
						.inputSchema(Map.of(
								"type", "object",
								"properties", Map.of(
										"text", Map.of(
												"type", "string",
												"description", "待编码的原始文本"
										)
								),
								"required", List.of("text")
						))
						.readOnly(true)
						.concurrencySafe(true)
		);
	}

	@Override
	public Mono<PermissionDecision> checkPermissions(Map<String, Object> toolInput, PermissionContextState context) {
		String text = (String) toolInput.getOrDefault("text", "");
		if (StringUtils.isEmpty(text)) {
			return Mono.just(PermissionDecision.deny("待编码文本不能为空"));
		}
		// 编码操作不涉及外部资源，直接允许
		return Mono.just(PermissionDecision.allow("Base64 编码无需授权"));
	}

	@Override
	public Mono<ToolResultBlock> callAsync(ToolCallParam param) {
		String text = (String) param.getInput().getOrDefault("text", "");
		ClarinetLogger.debug("Base64 编码文本，长度：" + text.length());

		try {
			String encoded = Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
			return Mono.just(ToolResultBlock.text(encoded));
		} catch (Exception e) {
			ClarinetLogger.warn("Base64 编码失败：", e);
			return Mono.just(ToolResultBlock.error("编码失败：" + e.getMessage()));
		}
	}
}