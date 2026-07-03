package org.ecnumc.clarinet.tool;

import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.permission.PermissionDecision;
import io.agentscope.core.tool.ToolBase;
import io.agentscope.core.tool.ToolCallParam;
import org.apache.ibatis.session.SqlSessionFactory;
import org.ecnumc.clarinet.ClarinetLogger;
import org.ecnumc.clarinet.repository.AssistanceQueryRepository;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 获取帮助历史数量
 *
 * @author liudongyu
 */
public final class CountDateAssistanceTool extends ToolBase {
	private final AssistanceQueryRepository assistanceQueryRepository;

	/**
	 * 构造函数
	 * @param sqlSessionFactory SQL 上下文
	 */
	public CountDateAssistanceTool(SqlSessionFactory sqlSessionFactory) {
		super(
				ToolBase.builder().name("count_date_assistance").description("获取当日用户求助总数")
						.inputSchema(Map.of(
								"type", "object",
								"properties", Map.of(
										"date", Map.of(
												"type", "string",
												"description", "日期（格式“yyyy-MM-dd”，默认今天）"
										),
										"user_id", Map.of(
												"type", "string",
												"description", "用户 ID（不传则查询所有用户的求助总数）"
										),
										"dealt", Map.of(
												"type", "integer",
												"description", "是否已被解决（1 表示查询解决的数量，0 表示查询未解决的数量，不传则查询所有求助总数）"
										)
								),
								"required", List.of()
						))
						.readOnly(true)
						.concurrencySafe(true)
		);
		this.assistanceQueryRepository = new AssistanceQueryRepository(sqlSessionFactory);
	}

	@Override
	public Mono<PermissionDecision> checkPermissions(Map<String, Object> toolInput, PermissionContextState context) {
		return Mono.just(PermissionDecision.allow("查询当日帮助总数无需授权"));
	}

	@Override
	public Mono<ToolResultBlock> callAsync(ToolCallParam param) {
		String uid = (String) param.getInput().get("uid");
		String date = (String) param.getInput().getOrDefault("date", LocalDate.now().toString());
		Integer dealt = (Integer) param.getInput().get("dealt");
		ClarinetLogger.debug("查询日期 " + date + " 的 " + this.getDealtName(dealt) + "用户求助总数");

		try {
			return Mono.just(ToolResultBlock.of(
					TextBlock.builder().text(String.valueOf(this.assistanceQueryRepository.query(uid, date, dealt))).build()
			));
		} catch (Exception e) {
			ClarinetLogger.warn("Failed to query user \"" + uid + "\": ", e);
			return Mono.just(ToolResultBlock.error(e.getMessage()));
		}
	}

	private String getDealtName(@Nullable Integer dealt) {
		if(dealt == null) {
			return "所有";
		}
		return dealt == 0 ? "未解决的" : "已解决的";
	}
}
