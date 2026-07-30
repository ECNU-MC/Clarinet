package org.ecnumc.clarinet.tool;

import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.permission.PermissionDecision;
import io.agentscope.core.tool.ToolBase;
import io.agentscope.core.tool.ToolCallParam;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSessionFactory;
import org.ecnumc.clarinet.ClarinetLogger;
import org.ecnumc.clarinet.convert.AssistanceConvert;
import org.ecnumc.clarinet.repository.AssistanceQueryRepository;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * 获取帮助历史
 *
 * @author liudongyu
 */
public final class RecentAssistanceTool extends ToolBase {
	private static final String PARAM_UID = "uid";
	private static final String PARAM_LIMIT = "limit";

	private final AssistanceQueryRepository assistanceQueryRepository;

	/**
	 * 构造函数
	 * @param sqlSessionFactory SQL 上下文
	 */
	public RecentAssistanceTool(SqlSessionFactory sqlSessionFactory) {
		super(
				ToolBase.builder().name("recent_assistance").description("获取用户近期提问历史")
						.inputSchema(Map.of(
								"type", "object",
								"properties", Map.of(
										PARAM_UID, Map.of(
												"type", "string",
												"description", "用户 ID"
										),
										PARAM_LIMIT, Map.of(
												"type", "integer",
												"default", 5,
												"minimum", 1,
												"maximum", 100,
												"description", "查询的历史记录最大条数"
										)
								),
								"required", List.of(PARAM_UID)
						))
						.readOnly(true)
						.concurrencySafe(true)
		);
		this.assistanceQueryRepository = new AssistanceQueryRepository(sqlSessionFactory);
	}

	@Override
	public Mono<PermissionDecision> checkPermissions(Map<String, Object> toolInput, PermissionContextState context) {
		return Mono.just(PermissionDecision.allow("查询用户历史提问无需授权"));
	}

	@Override
	public Mono<ToolResultBlock> callAsync(ToolCallParam param) {
		String uid = (String) param.getInput().getOrDefault(PARAM_UID, "");
		Integer limit = (Integer) param.getInput().getOrDefault(PARAM_LIMIT, 5);
		ClarinetLogger.debug("查询用户 " + uid + " 的最近 " + limit + " 条提问历史");

		try {
			List<String> list = this.assistanceQueryRepository.queryByUid(uid, limit)
					.stream()
					.map(AssistanceConvert::toJsonString)
					.toList();
			return Mono.just(ToolResultBlock.of(
					TextBlock.builder().text(
							"查询到 " + list.size() + " 条记录" + (
									list.isEmpty() ?
											"。" :
											"：\n\n" + StringUtils.join(list, ",\n")
							)
					).build()
			));
		} catch (Exception e) {
			ClarinetLogger.warn("Failed to query user \"" + uid + "\": ", e);
			return Mono.just(ToolResultBlock.error(e.getMessage()));
		}
	}
}
