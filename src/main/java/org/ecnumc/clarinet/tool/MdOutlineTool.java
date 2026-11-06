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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 获取 Markdown 文档骨架（标题大纲）
 * <br/>
 * 输出格式为“行号: 标题”，忽略代码块（```）内的标题，末尾输出文档总行数
 *
 * @author liudongyu
 */
public final class MdOutlineTool extends ToolBase {
	private static final String PARAM_FILE = "file";
	private static final String PARAM_LINE_NUMBER = "line_number";
	private static final String PARAM_MAX_LEVEL = "max_level";
	private static final int MIN_LEVEL = 1;
	private static final int MAX_LEVEL = 6;
	private static final int DEFAULT_MAX_LEVEL = MAX_LEVEL;
	private static final String FENCE = "```";

	/**
	 * 构造函数
	 */
	public MdOutlineTool() {
		super(
				ToolBase.builder().name("md_outline")
						.description("获取 Markdown 文档的骨架（标题大纲），逐行输出“行号: 标题”，自动忽略代码块（```）内的标题，并在末尾输出文档总行数")
						.inputSchema(Map.of(
								"type", "object",
								"properties", Map.of(
										PARAM_FILE, Map.of(
												"type", "string",
												"description", "Markdown 文档文件路径"
										),
										PARAM_LINE_NUMBER, Map.of(
												"type", "boolean",
												"description", "是否包含行号，默认为 true"
										),
										PARAM_MAX_LEVEL, Map.of(
												"type", "integer",
												"description", "最高标题级数，默认为 6，可选 1 到 6，超出范围按 6 处理"
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
			Objects.requireNonNull((Boolean) toolInput.getOrDefault(PARAM_LINE_NUMBER, Boolean.TRUE), PARAM_LINE_NUMBER);
			Objects.requireNonNull((Integer) toolInput.getOrDefault(PARAM_MAX_LEVEL, DEFAULT_MAX_LEVEL), PARAM_MAX_LEVEL);
			if (StringUtils.isEmpty(filename)) {
				return Mono.just(PermissionDecision.deny("文件路径不能为空"));
			}
			Path path = Paths.get(filename);
			File file = path.toFile();
			if (!file.exists()) {
				return Mono.just(PermissionDecision.deny("文件不存在"));
			}
			if (!file.canRead()) {
				return Mono.just(PermissionDecision.deny("文件不可读取"));
			}
			if (path.startsWith(Clarinet.WORKSPACE)) {
				return Mono.just(PermissionDecision.allow("工作区内文件的阅读无需授权"));
			}
			return Mono.just(PermissionDecision.ask("非工作区内文件阅读需要授权"));
		} catch (ClassCastException e) {
			return Mono.just(PermissionDecision.deny("参数类型错误：" + e.getMessage()));
		} catch (NullPointerException e) {
			return Mono.just(PermissionDecision.deny("参数值非法：" + e.getMessage()));
		}
	}

	@Override
	public Mono<ToolResultBlock> callAsync(ToolCallParam param) {
		String filename = (String) param.getInput().getOrDefault(PARAM_FILE, "");
		Boolean lineNumberObj = (Boolean) param.getInput().getOrDefault(PARAM_LINE_NUMBER, Boolean.TRUE);
		Integer maxLevelObj = (Integer) param.getInput().getOrDefault(PARAM_MAX_LEVEL, DEFAULT_MAX_LEVEL);
		boolean lineNumber = lineNumberObj == null || lineNumberObj;
		int maxLevel = maxLevelObj == null ? DEFAULT_MAX_LEVEL : Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, maxLevelObj));
		ClarinetLogger.debug("获取 Markdown 骨架 " + filename);

		try {
			List<String> lines = Files.readAllLines(Paths.get(filename), StandardCharsets.UTF_8);
			List<String> outline = Lists.newArrayList();
			boolean fence = false;
			for(int i = 0; i < lines.size(); ++i) {
				String line = lines.get(i);
				if(line.startsWith(FENCE)) {
					fence = !fence;
					continue;
				}
				if(fence) {
					continue;
				}
				int level = this.headingLevel(line);
				if(level >= MIN_LEVEL && level <= maxLevel) {
					outline.add(lineNumber ? (i + 1) + ": " + line : line);
				}
			}
			outline.add("文档共 " + lines.size() + " 行");
			return Mono.just(ToolResultBlock.text(StringUtils.join(outline, "\n")));
		} catch (IOException e) {
			ClarinetLogger.warn("Failed to analyze markdown " + filename + ": ", e);
			return Mono.just(ToolResultBlock.error(e.getMessage()));
		}
	}

	/**
	 * 计算标题级别，非标题返回 0
	 * <br/>
	 * 与 awk 脚本 `^#{1,6} ` 的匹配语义一致：井号后必须跟空格，最多 6 级
	 *
	 * @param line 待判断的行
	 * @return 标题级别，非标题返回 0
	 */
	private int headingLevel(String line) {
		int level = 0;
		while(level < line.length() && line.charAt(level) == '#') {
			level += 1;
		}
		if(level >= MIN_LEVEL && level <= MAX_LEVEL && line.length() > level && line.charAt(level) == ' ') {
			return level;
		}
		return 0;
	}
}
