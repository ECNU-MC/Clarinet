package org.ecnumc.clarinet.tool;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.permission.PermissionDecision;
import io.agentscope.core.tool.ToolBase;
import io.agentscope.core.tool.ToolCallParam;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.ecnumc.clarinet.bo.Reaction;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 发送表情包
 *
 * @author liudongyu
 */
public class SendReactionTool extends ToolBase {
	private static final String PARAM_REACTION = "reaction";

	/** 在模型调用以外，不执行发送表情包 */
	public static final Consumer<Reaction> EMPTY = ignored -> {};

	@Setter
	private Consumer<Reaction> sender = EMPTY;

	/**
	 * 构造函数
	 */
	public SendReactionTool() {
		super(
				ToolBase.builder().name("send_reaction").description("发送表情包。在闲聊场景中使用，可以独立发送也可以配合 send_message 使用。不必发送地过于频繁，偶尔使用可以活跃气氛，避免冰冷的“人机感”")
						.inputSchema(Map.of(
								"type", "object",
								"properties", Map.of(
										PARAM_REACTION, Map.of(
												"type", "string",
												"description", "表情包编码。可供选择的有：\n\n" + Reaction.buildDescription() + "\n"
										)
								),
								"required", List.of(PARAM_REACTION)
						))
		);
	}

	@Override
	public Mono<PermissionDecision> checkPermissions(Map<String, Object> toolInput, PermissionContextState context) {
		String reaction = (String) toolInput.getOrDefault(PARAM_REACTION, "");
		if (StringUtils.isEmpty(reaction)) {
			return Mono.just(PermissionDecision.deny("消息内容不能为空"));
		}
		if(reaction.length() > 4095) {
			return Mono.just(PermissionDecision.deny("消息过长"));
		}
		return Mono.just(PermissionDecision.allow("发送消息无需授权"));
	}

	@Override
	public Mono<ToolResultBlock> callAsync(ToolCallParam param) {
		String reaction = (String) param.getInput().getOrDefault(PARAM_REACTION, "");
		try {
			this.sender.accept(Reaction.valueOf(reaction));
		} catch (Exception e) {
			return Mono.just(ToolResultBlock.error(e.getMessage()));
		}
		return Mono.just(ToolResultBlock.text("发送成功！消息 ID："));
	}
}
