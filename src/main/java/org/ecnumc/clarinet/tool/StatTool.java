package org.ecnumc.clarinet.tool;

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
import java.util.Locale;
import java.util.Map;

/**
 * 获取文件大小
 *
 * @author liudongyu
 */
public final class StatTool extends ToolBase {
	private static final String PARAM_FILE = "file";

	/**
	 * 构造函数
	 */
	public StatTool() {
		super(
				ToolBase.builder().name("stat")
						.description("获取文件大小（字节数，并附带人类可读的近似大小）")
						.inputSchema(Map.of(
								"type", "object",
								"properties", Map.of(
										PARAM_FILE, Map.of(
												"type", "string",
												"description", "文件路径"
										)
								),
								"required", List.of(PARAM_FILE)
						))
						.readOnly(true)
						.concurrencySafe(true)
		);
	}

	@Override
	public Mono<PermissionDecision> checkPermissions(Map<String, Object> toolInput, PermissionContextState context) {
		try {
			String filename = (String) toolInput.getOrDefault(PARAM_FILE, "");
			if (StringUtils.isEmpty(filename)) {
				return Mono.just(PermissionDecision.deny("文件路径不能为空"));
			}
			Path path = Paths.get(filename);
			File file = path.toFile();
			if (!file.exists()) {
				return Mono.just(PermissionDecision.deny("文件不存在"));
			}
			if (file.isDirectory()) {
				return Mono.just(PermissionDecision.deny("不是文件"));
			}
			if (!file.canRead()) {
				return Mono.just(PermissionDecision.deny("文件不可读取"));
			}
			if (path.startsWith(Clarinet.WORKSPACE)) {
				return Mono.just(PermissionDecision.allow("工作区内文件的读取无需授权"));
			}
			return Mono.just(PermissionDecision.ask("非工作区内文件读取需要授权"));
		} catch (ClassCastException e) {
			return Mono.just(PermissionDecision.deny("参数类型错误：" + e.getMessage()));
		} catch (NullPointerException e) {
			return Mono.just(PermissionDecision.deny("参数值非法：" + e.getMessage()));
		}
	}

	@Override
	public Mono<ToolResultBlock> callAsync(ToolCallParam param) {
		String filename = (String) param.getInput().getOrDefault(PARAM_FILE, "");
		ClarinetLogger.debug("获取文件大小 " + filename);

		try {
			long size = Files.size(Paths.get(filename));
			return Mono.just(ToolResultBlock.text(this.formatSize(size)));
		} catch (IOException e) {
			ClarinetLogger.warn("Failed to stat " + filename + ": ", e);
			return Mono.just(ToolResultBlock.error(e.getMessage()));
		}
	}

	/**
	 * 格式化文件大小
	 *
	 * @param bytes 字节数
	 * @return 格式化后的大小
	 */
	private String formatSize(long bytes) {
		if(bytes < 1024) {
			return bytes + " 字节";
		}
		String[] units = { "KB", "MB", "GB", "TB" };
		double value = bytes;
		int unit = -1;
		while(value >= 1024 && unit < units.length - 1) {
			value /= 1024;
			unit += 1;
		}
		return bytes + " 字节（" + String.format(Locale.ROOT, "%.2f %s", value, units[unit]) + "）";
	}
}
