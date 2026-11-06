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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

/**
 * 在目录或文件中按正则表达式搜索匹配行
 * <br/>
 * 目录模式下输出“文件路径:行号: 内容”，单文件模式下输出“行号: 内容”，匹配行数超过上限时直接报错
 *
 * @author liudongyu
 */
public final class SearchTool extends ToolBase {
	private static final String PARAM_PATTERN = "pattern";
	private static final String PARAM_DIRECTORY = "directory";
	private static final String PARAM_FILE = "file";
	private static final String PARAM_MAX_MATCHES = "max_matches";
	private static final int DEFAULT_MAX_MATCHES = 2000;

	/**
	 * 构造函数
	 */
	public SearchTool() {
		super(
				ToolBase.builder().name("search")
						.description("在目录（默认整个工作区）或指定文件中按正则表达式查找匹配行，返回文件名（目录搜索时）、行号和匹配行内容；匹配行数超过上限时直接报错")
						.inputSchema(Map.of(
								"type", "object",
								"properties", Map.of(
										PARAM_PATTERN, Map.of(
												"type", "string",
												"description", "正则表达式匹配模式"
										),
										PARAM_DIRECTORY, Map.of(
												"type", "string",
												"description", "搜索目录，默认为整个工作区"
										),
										PARAM_FILE, Map.of(
												"type", "string",
												"description", "指定文件名，填写后将忽略目录，只搜索该文件"
										),
										PARAM_MAX_MATCHES, Map.of(
												"type", "integer",
												"description", "最大匹配行数，默认为 2000，超出则直接报错"
										)
								),
								"required", List.of(PARAM_PATTERN)
						))
						.readOnly(true)
						.concurrencySafe(true)
		);
	}

	@Override
	public Mono<PermissionDecision> checkPermissions(Map<String, Object> toolInput, PermissionContextState context) {
		try {
			String pattern = (String) toolInput.getOrDefault(PARAM_PATTERN, "");
			String directory = (String) toolInput.getOrDefault(PARAM_DIRECTORY, "");
			String file = (String) toolInput.getOrDefault(PARAM_FILE, "");
			Integer maxMatches = (Integer) toolInput.getOrDefault(PARAM_MAX_MATCHES, DEFAULT_MAX_MATCHES);
			if (StringUtils.isEmpty(pattern)) {
				return Mono.just(PermissionDecision.deny("pattern 不能为空"));
			}
			if (maxMatches == null || maxMatches < 1) {
				return Mono.just(PermissionDecision.deny("max_matches 必须大于 0"));
			}
			if (StringUtils.isNotEmpty(file)) {
				Path path = Paths.get(file);
				File target = path.toFile();
				if (!target.exists()) {
					return Mono.just(PermissionDecision.deny("文件不存在"));
				}
				if (target.isDirectory()) {
					return Mono.just(PermissionDecision.deny("不是文件"));
				}
				if (!target.canRead()) {
					return Mono.just(PermissionDecision.deny("文件不可读取"));
				}
				return this.permissionFor(path);
			}
			String targetDirectory = StringUtils.isEmpty(directory) ? Clarinet.WORKSPACE : directory;
			Path path = Paths.get(targetDirectory);
			File target = path.toFile();
			if (!target.exists()) {
				return Mono.just(PermissionDecision.deny("目录不存在"));
			}
			if (!target.isDirectory()) {
				return Mono.just(PermissionDecision.deny("不是目录"));
			}
			if (!target.canRead()) {
				return Mono.just(PermissionDecision.deny("目录不可读取"));
			}
			return this.permissionFor(path);
		} catch (ClassCastException e) {
			return Mono.just(PermissionDecision.deny("参数类型错误：" + e.getMessage()));
		} catch (NullPointerException e) {
			return Mono.just(PermissionDecision.deny("参数值非法：" + e.getMessage()));
		}
	}

	@Override
	public Mono<ToolResultBlock> callAsync(ToolCallParam param) {
		String pattern = (String) param.getInput().getOrDefault(PARAM_PATTERN, "");
		String directory = (String) param.getInput().getOrDefault(PARAM_DIRECTORY, "");
		String file = (String) param.getInput().getOrDefault(PARAM_FILE, "");
		Integer maxMatchesObj = (Integer) param.getInput().getOrDefault(PARAM_MAX_MATCHES, DEFAULT_MAX_MATCHES);
		int maxMatches = maxMatchesObj == null ? DEFAULT_MAX_MATCHES : maxMatchesObj;
		ClarinetLogger.debug("搜索 " + pattern);

		Pattern compiled;
		try {
			compiled = Pattern.compile(pattern);
		} catch (PatternSyntaxException e) {
			return Mono.just(ToolResultBlock.error("正则表达式不合法：" + e.getMessage()));
		}

		List<String> matches = Lists.newArrayList();
		try {
			boolean exceeded;
			if (StringUtils.isNotEmpty(file)) {
				exceeded = this.searchFile(Paths.get(file), false, compiled, matches, maxMatches);
			} else {
				String targetDirectory = StringUtils.isEmpty(directory) ? Clarinet.WORKSPACE : directory;
				exceeded = this.searchDirectory(Paths.get(targetDirectory), compiled, matches, maxMatches);
			}
			if (exceeded) {
				return Mono.just(ToolResultBlock.error(
						"匹配行数超过上限（max_matches=" + maxMatches + "），请缩小搜索范围后重试"));
			}
		} catch (IOException e) {
			ClarinetLogger.warn("Failed to search " + pattern + ": ", e);
			return Mono.just(ToolResultBlock.error(e.getMessage()));
		}
		if (matches.isEmpty()) {
			return Mono.just(ToolResultBlock.text("未找到匹配。"));
		}
		return Mono.just(ToolResultBlock.text(StringUtils.join(matches, "\n")));
	}

	/**
	 * 递归搜索目录内所有文件
	 *
	 * @param directory 搜索目录
	 * @param pattern 匹配模式
	 * @param matches 匹配结果
	 * @param maxMatches 最大匹配行数
	 * @return 匹配行数是否超限
	 * @throws IOException 目录遍历失败
	 */
	private boolean searchDirectory(Path directory, Pattern pattern, List<String> matches, int maxMatches) throws IOException {
		List<Path> files;
		try (Stream<Path> stream = Files.walk(directory)) {
			files = stream.filter(Files::isRegularFile)
					.sorted(Comparator.comparing(Path::toString, String.CASE_INSENSITIVE_ORDER)
							.thenComparing(Path::toString))
					.toList();
		}
		for(Path file : files) {
			try {
				if(this.searchFile(file, true, pattern, matches, maxMatches)) {
					return true;
				}
			} catch (IOException e) {
				ClarinetLogger.debug("跳过无法读取的文件 " + file + ": " + e.getMessage());
			}
		}
		return false;
	}

	/**
	 * 搜索单个文件
	 *
	 * @param file 文件
	 * @param withFileName 是否在结果前输出文件名
	 * @param pattern 匹配模式
	 * @param matches 匹配结果
	 * @param maxMatches 最大匹配行数
	 * @return 匹配行数是否超限
	 * @throws IOException 文件读取失败
	 */
	private boolean searchFile(Path file, boolean withFileName, Pattern pattern, List<String> matches, int maxMatches)
			throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			int lineNumber = 0;
			String line;
			while((line = reader.readLine()) != null) {
				lineNumber += 1;
				if(pattern.matcher(line).find()) {
					matches.add(withFileName
							? file + ":" + lineNumber + ": " + line
							: lineNumber + ": " + line);
					if(matches.size() > maxMatches) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * 根据工作区判定权限
	 *
	 * @param path 目标路径
	 * @return 权限决策
	 */
	private Mono<PermissionDecision> permissionFor(Path path) {
		if(path.startsWith(Clarinet.WORKSPACE)) {
			return Mono.just(PermissionDecision.allow("工作区内文件的阅读无需授权"));
		}
		return Mono.just(PermissionDecision.ask("非工作区内文件阅读需要授权"));
	}
}
