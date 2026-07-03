package org.ecnumc.clarinet.tool;

import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.permission.PermissionDecision;
import io.agentscope.core.tool.ToolBase;
import io.agentscope.core.tool.ToolCallParam;
import org.apache.commons.lang3.StringUtils;
import org.ecnumc.clarinet.Clarinet;
import org.ecnumc.clarinet.ClarinetLogger;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * 读文件
 *
 * @author liudongyu
 */
public final class ReadTool extends ToolBase {
	/**
	 * 构造函数
	 */
	public ReadTool() {
		super(
				ToolBase.builder().name("read").description("阅读文件内容")
						.inputSchema(Map.of(
								"type", "object",
								"properties", Map.of(
										"file", Map.of(
												"type", "string",
												"description", "文件路径"
										)
								),
								"required", List.of("file")
						))
						.readOnly(true)
						.concurrencySafe(true)
		);
	}

	@Override
	public Mono<PermissionDecision> checkPermissions(Map<String, Object> toolInput, PermissionContextState context) {
		String filename = (String) toolInput.getOrDefault("file", "");
		if(StringUtils.isEmpty(filename)) {
			return Mono.just(PermissionDecision.deny("文件路径不能为空"));
		}
		Path path = Paths.get(filename);
		File file = path.toFile();
		if(!file.exists()) {
			return Mono.just(PermissionDecision.deny("文件不存在"));
		}
		if(!file.canRead()) {
			return Mono.just(PermissionDecision.deny("文件不可读取"));
		}
		if(path.startsWith(Clarinet.WORKSPACE)) {
			return Mono.just(PermissionDecision.allow("工作区内文件的阅读无需授权"));
		}
		return Mono.just(PermissionDecision.ask("非工作区内文件阅读需要授权"));
	}

	@Override
	public Mono<ToolResultBlock> callAsync(ToolCallParam param) {
		String filename = (String) param.getInput().getOrDefault("file", "");
		ClarinetLogger.debug("阅读文件 " + filename);

		try {
			return Mono.just(ToolResultBlock.of(
					TextBlock.builder()
							.text(StringUtils.join(Files.readAllLines(Paths.get(filename)), "\n"))
							.build()
			));
		} catch (IOException e) {
			ClarinetLogger.warn("Failed to read file " + filename + ": ", e);
			return Mono.just(ToolResultBlock.error(e.getMessage()));
		}
	}
}
