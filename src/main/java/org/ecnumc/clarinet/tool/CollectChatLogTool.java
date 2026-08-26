package org.ecnumc.clarinet.tool;

import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.permission.PermissionDecision;
import io.agentscope.core.tool.ToolBase;
import io.agentscope.core.tool.ToolCallParam;
import org.apache.commons.lang3.StringUtils;
import org.ecnumc.clarinet.data.Message;
import org.ecnumc.clarinet.repository.SessionRepository;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * 收集最近聊天记录
 *
 * @author liudongyu
 */
public class CollectChatLogTool extends ToolBase {
	private static final String PARAM_LIMIT = "limit";

	private final SessionRepository sessionRepository;

	/**
	 * 构造函数
	 * @param sessionRepository 上下文仓库
	 */
	public CollectChatLogTool(SessionRepository sessionRepository) {
		super(
				ToolBase.builder().name("collect_chat_log").description("收集群聊中最近若干条聊天记录")
						.inputSchema(Map.of(
								"type", "object",
								"properties", Map.of(
										PARAM_LIMIT, Map.of(
												"type", "integer",
												"default", 100,
												"minimum", 1,
												"maximum", 100,
												"description", "收集最近聊天记录最大条数"
										)
								),
								"required", List.of()
						))
						.readOnly(true)
		);
		this.sessionRepository = sessionRepository;
	}

	@Override
	public Mono<PermissionDecision> checkPermissions(Map<String, Object> toolInput, PermissionContextState context) {
		return Mono.just(PermissionDecision.allow("获取聊天记录无需授权"));
	}

	@Override
	public Mono<ToolResultBlock> callAsync(ToolCallParam param) {
		String groupId = param.getRuntimeContext().get("groupId");
		if(StringUtils.isEmpty(groupId)) {
			return Mono.just(ToolResultBlock.error("内部错误：未指定 groupId。这不是参数错误，请立刻联系 Agent Harness 开发者。"));
		}
		Integer limit = (Integer) param.getInput().getOrDefault(PARAM_LIMIT, 100);
		List<Message> list = this.sessionRepository.getMessageContext(groupId);
		int size = list.size();
		if(size > limit) {
			list = list.subList(size - limit, size);
		}
		return Mono.just(ToolResultBlock.of(list.stream().<ContentBlock>map(
				message -> TextBlock.builder()
						.text(message.buildTextContent())
						.build()
		).toList()));
	}
}
