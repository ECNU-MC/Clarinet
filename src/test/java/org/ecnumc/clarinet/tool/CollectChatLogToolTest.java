package org.ecnumc.clarinet.tool;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.ToolCallParam;
import org.ecnumc.clarinet.data.Message;
import org.ecnumc.clarinet.repository.SessionRepository;
import org.ecnumc.clarinet.support.ToolResults;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * 收集群聊记录工具单元测试
 *
 * @author liudongyu
 */
@ExtendWith(MockitoExtension.class)
class CollectChatLogToolTest {
	@Mock
	private SessionRepository sessionRepository;

	private CollectChatLogTool tool;

	@BeforeEach
	void setUp() {
		this.tool = new CollectChatLogTool(this.sessionRepository);
	}

	/**
	 * 未指定条数时返回全部聊天记录
	 */
	@Test
	void testCollectAllMessages() {
		List<Message> messages = List.of(
				this.message("1", "第一条", 1_000L),
				this.message("2", "第二条", 2_000L),
				this.message("3", "第三条", 3_000L)
		);
		when(this.sessionRepository.getMessageContext("group-1")).thenReturn(messages);
		ToolResultBlock result = this.tool.callAsync(this.param("group-1", Map.of())).block();
		assertEquals(
				messages.stream().map(Message::buildTextContent).collect(Collectors.joining("\n")),
				ToolResults.text(result)
		);
	}

	/**
	 * 指定条数时只返回最近的若干条
	 */
	@Test
	void testCollectWithLimit() {
		List<Message> messages = List.of(
				this.message("1", "m1", 1_000L),
				this.message("2", "m2", 2_000L),
				this.message("3", "m3", 3_000L),
				this.message("4", "m4", 4_000L),
				this.message("5", "m5", 5_000L)
		);
		when(this.sessionRepository.getMessageContext("group-1")).thenReturn(messages);
		ToolResultBlock result = this.tool.callAsync(this.param("group-1", Map.of("limit", 2))).block();
		assertEquals(
				messages.subList(3, 5).stream().map(Message::buildTextContent).collect(Collectors.joining("\n")),
				ToolResults.text(result)
		);
	}

	/**
	 * 缺少 groupId 时应返回错误结果
	 */
	@Test
	void testMissingGroupIdReturnsError() {
		ToolResultBlock result = this.tool.callAsync(
				ToolCallParam.builder()
						.input(Map.of())
						.runtimeContext(RuntimeContext.empty())
						.build()
		).block();
		assertTrue(ToolResults.text(result).contains("groupId"));
	}

	private ToolCallParam param(String groupId, Map<String, Object> input) {
		return ToolCallParam.builder()
				.input(input)
				.runtimeContext(RuntimeContext.builder().put("groupId", groupId).build())
				.build();
	}

	private Message message(String id, String content, long epochMillis) {
		return Message.builder()
				.id(id)
				.role("user")
				.type("plain")
				.uid("uid-" + id)
				.nickname("nick-" + id)
				.content(content)
				.sentAt(new Date(epochMillis))
				.build();
	}
}
