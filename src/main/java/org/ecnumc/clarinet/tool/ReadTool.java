package org.ecnumc.clarinet.tool;

import com.google.common.collect.Lists;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.permission.PermissionDecision;
import io.agentscope.core.tool.ToolBase;
import io.agentscope.core.tool.ToolCallParam;
import org.apache.commons.io.input.ReversedLinesFileReader;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 读文件
 *
 * @author liudongyu
 */
public final class ReadTool extends ToolBase {
	private static final String PARAM_FILE = "file";
	private static final String PARAM_TAIL = "tail";
	private static final String PARAM_BEGIN = "begin";
	private static final String PARAM_END = "end";

	/**
	 * 构造函数
	 */
	public ReadTool() {
		super(
				ToolBase.builder().name("read").description("阅读文件内容，支持全量读取、末尾读取和随机连续读取")
						.inputSchema(Map.of(
								"type", "object",
								"properties", Map.of(
										PARAM_FILE, Map.of(
												"type", "string",
												"description", "文件路径"
										),
										PARAM_TAIL, Map.of(
												"type", "integer",
												"description", "只返回文件末尾若干行"
										),
										PARAM_BEGIN, Map.of(
												"type", "integer",
												"description", "随机读取的起始行"
										),
										PARAM_END, Map.of(
												"type", "integer",
												"description", "随机读取的终止行"
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
			if (!file.canRead()) {
				return Mono.just(PermissionDecision.deny("文件不可读取"));
			}

			Integer tail = (Integer)toolInput.get(PARAM_TAIL);
			Integer begin = (Integer)toolInput.get(PARAM_BEGIN);
			Integer end = (Integer)toolInput.get(PARAM_END);

			if(begin != null && end != null) {
				if(begin > end) {
					return Mono.just(PermissionDecision.deny("begin 应小于等于 end"));
				}
				if(tail != null) {
					return Mono.just(PermissionDecision.deny("不能同时随机读取和尾部读取"));
				}
			} else if(begin != null || end != null) {
				return Mono.just(PermissionDecision.deny("begin 和 end 参数应同时有值或同时为空"));
			}

			if (path.startsWith(Clarinet.WORKSPACE)) {
				return Mono.just(PermissionDecision.allow("工作区内文件的阅读无需授权"));
			}
			return Mono.just(PermissionDecision.ask("非工作区内文件阅读需要授权"));
		} catch (ClassCastException e) {
			return Mono.just(PermissionDecision.deny("参数类型错误：" + e.getMessage()));
		}
	}

	@Override
	public Mono<ToolResultBlock> callAsync(ToolCallParam param) {
		String filename = (String) param.getInput().getOrDefault(PARAM_FILE, "");
		Integer tail = (Integer) param.getInput().get(PARAM_TAIL);
		Integer begin = (Integer) param.getInput().get(PARAM_BEGIN);
		Integer end = (Integer) param.getInput().get(PARAM_END);
		ClarinetLogger.debug("阅读文件 " + filename);

		try {
			if(tail == null) {
				Path path = Paths.get(filename);
				if(begin == null || end == null) {
					return Mono.just(ToolResultBlock.of(
							TextBlock.builder()
									.text(StringUtils.join(Files.readAllLines(path), "\n"))
									.build()
					));
				}
				try (Stream<String> lines = Files.lines(path)) {
					return Mono.just(ToolResultBlock.of(
							TextBlock.builder().text(
									lines.skip(begin - 1L)
											.limit(end - begin + 1L)
											.collect(Collectors.joining("\n"))
							).build()
					));
				}
			}
			List<String> lines = Lists.newLinkedList();
			try (ReversedLinesFileReader reader = ReversedLinesFileReader.builder()
					.setFile(filename)
					.setCharset(StandardCharsets.UTF_8)
					.get()) {
				String line;
				while (lines.size() < tail && (line = reader.readLine()) != null) {
					lines.add(0, line);
				}
			}
			return Mono.just(ToolResultBlock.of(
					TextBlock.builder()
							.text(StringUtils.join(lines, "\n"))
							.build()
			));
		} catch (IOException e) {
			ClarinetLogger.warn("Failed to read file " + filename + ": ", e);
			return Mono.just(ToolResultBlock.error(e.getMessage()));
		}
	}
}
