package org.ecnumc.clarinet.tool;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.permission.PermissionBehavior;
import io.agentscope.core.permission.PermissionDecision;
import io.agentscope.core.tool.ToolCallParam;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.ecnumc.clarinet.mapper.AssistanceMapper;
import org.ecnumc.clarinet.po.Assistance;
import org.ecnumc.clarinet.support.ToolResults;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * 获取帮助历史工具单元测试
 *
 * @author liudongyu
 */
@ExtendWith(MockitoExtension.class)
class RecentAssistanceToolTest {
	@Mock
	private SqlSessionFactory sqlSessionFactory;

	@Mock
	private SqlSession sqlSession;

	@Mock
	private AssistanceMapper assistanceMapper;

	private RecentAssistanceTool tool;

	/**
	 * 将数据访问链路整体 mock 掉，避免真实访问数据库
	 */
	@BeforeEach
	void setUp() {
		lenient().when(this.sqlSessionFactory.openSession()).thenReturn(this.sqlSession);
		lenient().when(this.sqlSession.getMapper(AssistanceMapper.class)).thenReturn(this.assistanceMapper);
		this.tool = new RecentAssistanceTool(this.sqlSessionFactory);
	}

	/**
	 * 查询到历史记录时应输出 JSON 列表
	 */
	@Test
	void testQueryByUid() {
		when(this.assistanceMapper.queryByUid("uid-1", 5)).thenReturn(List.of(this.buildAssistance()));
		ToolResultBlock result = this.tool.callAsync(
				ToolCallParam.builder().input(Map.of("uid", "uid-1")).build()
		).block();
		String text = ToolResults.text(result);
		assertTrue(text.contains("查询到 1 条记录"));
		assertTrue(text.contains("\"uid\":\"uid-1\""));
		assertTrue(text.contains("\"dealt\":true"));
	}

	/**
	 * 未查询到记录时应输出空结果说明
	 */
	@Test
	void testQueryWithCustomLimit() {
		when(this.assistanceMapper.queryByUid("uid-2", 3)).thenReturn(List.of());
		ToolResultBlock result = this.tool.callAsync(
				ToolCallParam.builder().input(Map.of("uid", "uid-2", "limit", 3)).build()
		).block();
		assertEquals("查询到 0 条记录。", ToolResults.text(result));
	}

	/**
	 * 下游异常时应返回错误结果
	 */
	@Test
	void testQueryFailureReturnsError() {
		when(this.assistanceMapper.queryByUid("uid-3", 5)).thenThrow(new RuntimeException("数据库连接失败"));
		ToolResultBlock result = this.tool.callAsync(
				ToolCallParam.builder().input(Map.of("uid", "uid-3")).build()
		).block();
		assertTrue(ToolResults.text(result).startsWith("Error:"));
	}

	/**
	 * 查询历史始终无需授权
	 */
	@Test
	void testCheckPermissionsAllowed() {
		PermissionDecision decision = this.tool.checkPermissions(Map.of("uid", "uid-1"), null).block();
		assertEquals(PermissionBehavior.ALLOW, decision.getBehavior());
	}

	private Assistance buildAssistance() {
		Assistance assistance = new Assistance();
		assistance.setSessionId("session-1");
		assistance.setUid("uid-1");
		assistance.setFilename("crash-2026-01-01.txt");
		assistance.setDealt(true);
		assistance.setDescription("启动时报错");
		assistance.setCreatedAt(new Date(1_700_000_000_000L));
		assistance.setUpdatedAt(new Date(1_700_000_000_000L));
		return assistance;
	}
}
