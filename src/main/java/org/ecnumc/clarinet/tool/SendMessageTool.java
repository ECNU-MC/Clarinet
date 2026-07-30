package org.ecnumc.clarinet.tool;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.permission.PermissionDecision;
import io.agentscope.core.tool.ToolBase;
import io.agentscope.core.tool.ToolCallParam;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * 发送解答消息
 *
 * @author liudongyu
 */
public class SendMessageTool extends ToolBase {
	private static final String PARAM_MESSAGE = "message";

	/**
	 * 构造函数
	 */
	public SendMessageTool() {
		super(
				ToolBase.builder().name("send_message").description("发送消息，这是和提问者交互的唯一途径，如果你想让他们看到你的消息，必须调用这个工具")
						.inputSchema(Map.of(
								"type", "object",
								"properties", Map.of(
										PARAM_MESSAGE, Map.of(
												"type", "string",
												"description", "消息内容，请避免发送敏感信息"
										)
								),
								"required", List.of(PARAM_MESSAGE)
						))
		);
	}

	@Override
	public Mono<PermissionDecision> checkPermissions(Map<String, Object> toolInput, PermissionContextState context) {
		String message = (String) toolInput.getOrDefault(PARAM_MESSAGE, "");
		if (StringUtils.isEmpty(message)) {
			return Mono.just(PermissionDecision.deny("消息内容不能为空"));
		}
		if(message.length() > 4095) {
			return Mono.just(PermissionDecision.deny("消息过长"));
		}
		return Mono.just(PermissionDecision.allow("发送消息无需授权"));
	}

	@Override
	public Mono<ToolResultBlock> callAsync(ToolCallParam param) {
		String message = (String) param.getInput().getOrDefault(PARAM_MESSAGE, "");
		// TODO
		return Mono.just(ToolResultBlock.text("发送成功！消息 ID："));
	}
}
