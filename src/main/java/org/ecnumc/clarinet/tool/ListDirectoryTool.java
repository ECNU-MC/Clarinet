package org.ecnumc.clarinet.tool;

import com.google.common.collect.Lists;
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
import java.util.*;
import java.util.stream.Stream;

/**
 * 列举目录内容，输出与 tree 命令一致的树状结构
 *
 * @author liudongyu
 */
public final class ListDirectoryTool extends ToolBase {
	private static final String PARAM_DIRECTORY = "directory";
	private static final String PARAM_RECURSIVE = "recursive";
	private static final String PARAM_EXTENSION = "extension";

	/**
	 * 构造函数
	 */
	public ListDirectoryTool() {
		super(
				ToolBase.builder().name("list_directory")
						.description("以 tree 命令风格列举目录下的所有文件和子目录，可选是否递归与按扩展名筛选文件")
						.inputSchema(Map.of(
								"type", "object",
								"properties", Map.of(
										PARAM_DIRECTORY, Map.of(
												"type", "string",
												"description", "要列举的目录路径"
										),
										PARAM_RECURSIVE, Map.of(
												"type", "boolean",
												"description", "是否递归列举子目录，默认为 true"
										),
										PARAM_EXTENSION, Map.of(
												"type", "string",
												"description", "只列举指定扩展名的文件（不含点，不区分大小写），默认为空表示不筛选，目录始终列举"
										)
								),
								"required", List.of(PARAM_DIRECTORY)
						))
						.readOnly(true)
						.concurrencySafe(true)
		);
	}

	@Override
	public Mono<PermissionDecision> checkPermissions(Map<String, Object> toolInput, PermissionContextState context) {
		try {
			String directory = (String) toolInput.getOrDefault(PARAM_DIRECTORY, "");
			Objects.requireNonNull((Boolean) toolInput.getOrDefault(PARAM_RECURSIVE, Boolean.TRUE), PARAM_RECURSIVE);
			Objects.requireNonNull((String) toolInput.getOrDefault(PARAM_EXTENSION, ""), PARAM_EXTENSION);
			if (StringUtils.isEmpty(directory)) {
				return Mono.just(PermissionDecision.deny("目录路径不能为空"));
			}
			Path path = Paths.get(directory);
			File file = path.toFile();
			if (!file.exists()) {
				return Mono.just(PermissionDecision.deny("目录不存在"));
			}
			if (!file.isDirectory()) {
				return Mono.just(PermissionDecision.deny("不是目录"));
			}
			if (!file.canRead()) {
				return Mono.just(PermissionDecision.deny("目录不可读取"));
			}
			if (path.startsWith(Clarinet.WORKSPACE)) {
				return Mono.just(PermissionDecision.allow("工作区内目录的列举无需授权"));
			}
			return Mono.just(PermissionDecision.ask("非工作区内目录列举需要授权"));
		} catch (ClassCastException e) {
			return Mono.just(PermissionDecision.deny("参数类型错误：" + e.getMessage()));
		} catch (NullPointerException e) {
			return Mono.just(PermissionDecision.deny("参数值非法：" + e.getMessage()));
		}
	}

	@Override
	public Mono<ToolResultBlock> callAsync(ToolCallParam param) {
		String directory = (String) param.getInput().getOrDefault(PARAM_DIRECTORY, "");
		Boolean recursiveObj = (Boolean) param.getInput().getOrDefault(PARAM_RECURSIVE, Boolean.TRUE);
		String extension = (String) param.getInput().getOrDefault(PARAM_EXTENSION, "");
		boolean recursive = recursiveObj == null || recursiveObj;
		String normalizedExtension = this.normalizeExtension(extension);
		ClarinetLogger.debug("列举目录 " + directory);

		try {
			List<String> lines = Lists.newArrayList();
			lines.add(directory);
			this.appendChildren(lines, Paths.get(directory), "", recursive, normalizedExtension);
			return Mono.just(ToolResultBlock.text(StringUtils.join(lines, "\n")));
		} catch (IOException e) {
			ClarinetLogger.warn("Failed to list directory " + directory + ": ", e);
			return Mono.just(ToolResultBlock.error(e.getMessage()));
		}
	}

	/**
	 * 递归追加子条目
	 *
	 * @param lines 输出行
	 * @param directory 当前目录
	 * @param prefix 当前层级前缀
	 * @param recursive 是否递归
	 * @param extension 扩展名筛选（已归一化，空串表示不筛选）
	 * @throws IOException 目录读取失败
	 */
	private void appendChildren(List<String> lines, Path directory, String prefix, boolean recursive, String extension)
			throws IOException {
		List<Path> children;
		try (Stream<Path> stream = Files.list(directory)) {
			children = stream
					.filter(child -> Files.isDirectory(child) && !Files.isSymbolicLink(child)
							|| this.matchesExtension(child, extension))
					.sorted(Comparator.comparing((Path child) -> child.getFileName().toString(), String.CASE_INSENSITIVE_ORDER)
							.thenComparing(child -> child.getFileName().toString()))
					.toList();
		}
		for(int i = 0; i < children.size(); ++i) {
			Path child = children.get(i);
			boolean last = i == children.size() - 1;
			boolean isDirectory = Files.isDirectory(child) && !Files.isSymbolicLink(child);
			lines.add(prefix + (last ? "└── " : "├── ") + child.getFileName());
			if(recursive && isDirectory) {
				this.appendChildren(lines, child, prefix + (last ? "    " : "│   "), true, extension);
			}
		}
	}

	/**
	 * 判断文件是否匹配扩展名筛选
	 *
	 * @param child 文件
	 * @param extension 扩展名筛选（已归一化，空串表示不筛选）
	 * @return 是否匹配
	 */
	private boolean matchesExtension(Path child, String extension) {
		if(extension.isEmpty()) {
			return true;
		}
		return child.getFileName().toString().toLowerCase(Locale.ROOT).endsWith("." + extension);
	}

	/**
	 * 归一化扩展名筛选条件：去空白、去前导点、统一小写
	 *
	 * @param extension 原始扩展名
	 * @return 归一化后的扩展名
	 */
	private String normalizeExtension(String extension) {
		String normalized = StringUtils.trimToEmpty(extension).toLowerCase(Locale.ROOT);
		if(normalized.startsWith(".")) {
			normalized = normalized.substring(1);
		}
		return normalized;
	}
}
