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
 * Base64 解码工具
 *
 * @author liudongyu
 */
public final class Base64DecodeTool extends ToolBase {
	/**
	 * 构造函数
	 */
	public Base64DecodeTool() {
		super(
				ToolBase.builder()
						.name("base64_decode")
						.description("将 Base64 编码的字符串解码为原始文本")
						.inputSchema(Map.of(
								"type", "object",
								"properties", Map.of(
										"text", Map.of(
												"type", "string",
												"description", "待解码的 Base64 字符串"
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
			return Mono.just(PermissionDecision.deny("待解码文本不能为空"));
		}
		// 解码操作不涉及外部资源，直接允许
		return Mono.just(PermissionDecision.allow("Base64 解码无需授权"));
	}

	@Override
	public Mono<ToolResultBlock> callAsync(ToolCallParam param) {
		String text = (String) param.getInput().getOrDefault("text", "");
		ClarinetLogger.debug("Base64 解码文本，长度：" + text.length());

		try {
			byte[] decodedBytes = Base64.getDecoder().decode(text);
			String decoded = new String(decodedBytes, StandardCharsets.UTF_8);
			return Mono.just(ToolResultBlock.text(decoded));
		} catch (IllegalArgumentException e) {
			ClarinetLogger.warn("Base64 解码失败，输入格式无效：", e);
			return Mono.just(ToolResultBlock.error("解码失败，请检查输入是否为有效的 Base64 字符串：" + e.getMessage()));
		} catch (Exception e) {
			ClarinetLogger.warn("Base64 解码异常：", e);
			return Mono.just(ToolResultBlock.error("解码失败：" + e.getMessage()));
		}
	}
}