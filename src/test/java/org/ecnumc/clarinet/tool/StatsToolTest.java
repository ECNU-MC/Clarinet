package org.ecnumc.clarinet.tool;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.permission.PermissionBehavior;
import io.agentscope.core.permission.PermissionDecision;
import io.agentscope.core.tool.ToolCallParam;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.ecnumc.clarinet.mapper.AssistanceMapper;
import org.ecnumc.clarinet.support.ToolResults;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * 求助统计工具单元测试
 *
 * @author liudongyu
 */
@ExtendWith(MockitoExtension.class)
class StatsToolTest {
	@Mock
	private SqlSessionFactory sqlSessionFactory;

	@Mock
	private SqlSession sqlSession;

	@Mock
	private AssistanceMapper assistanceMapper;

	private StatsTool tool;

	/**
	 * 将数据访问链路整体 mock 掉，避免真实访问数据库
	 */
	@BeforeEach
	void setUp() {
		lenient().when(this.sqlSessionFactory.openSession()).thenReturn(this.sqlSession);
		lenient().when(this.sqlSession.getMapper(AssistanceMapper.class)).thenReturn(this.assistanceMapper);
		this.tool = new StatsTool(this.sqlSessionFactory);
	}

	/**
	 * 默认查询当日全部求助总数
	 */
	@Test
	void testCountAll() {
		when(this.assistanceMapper.countAllBetween(any(), any())).thenReturn(7L);
		ToolResultBlock result = this.tool.callAsync(
				ToolCallParam.builder().input(Map.of()).build()
		).block();
		assertEquals("7", ToolResults.text(result));
	}

	/**
	 * 按用户统计求助总数
	 */
	@Test
	void testCountByUid() {
		when(this.assistanceMapper.countByUidBetween(eq("uid-1"), any(), any())).thenReturn(2L);
		ToolResultBlock result = this.tool.callAsync(
				ToolCallParam.builder().input(Map.of("uid", "uid-1")).build()
		).block();
		assertEquals("2", ToolResults.text(result));
	}

	/**
	 * 按处理状态统计当日求助总数
	 */
	@Test
	void testCountAllDealt() {
		when(this.assistanceMapper.countAllDealtBetween(any(), any(), eq(0))).thenReturn(0L);
		ToolResultBlock result = this.tool.callAsync(
				ToolCallParam.builder().input(Map.of("dealt", 0)).build()
		).block();
		assertEquals("0", ToolResults.text(result));
	}

	/**
	 * 按用户与处理状态统计求助总数
	 */
	@Test
	void testCountByUidDealt() {
		when(this.assistanceMapper.countByUidDealtBetween(eq("uid-1"), any(), any(), eq(1))).thenReturn(1L);
		ToolResultBlock result = this.tool.callAsync(
				ToolCallParam.builder().input(Map.of("uid", "uid-1", "dealt", 1)).build()
		).block();
		assertEquals("1", ToolResults.text(result));
	}

	/**
	 * 下游异常时应返回错误结果
	 */
	@Test
	void testQueryFailureReturnsError() {
		when(this.assistanceMapper.countAllBetween(any(), any())).thenThrow(new RuntimeException("数据库连接失败"));
		ToolResultBlock result = this.tool.callAsync(
				ToolCallParam.builder().input(Map.of()).build()
		).block();
		assertTrue(ToolResults.text(result).startsWith("Error:"));
	}

	/**
	 * 统计查询始终无需授权
	 */
	@Test
	void testCheckPermissionsAllowed() {
		PermissionDecision decision = this.tool.checkPermissions(Map.of(), null).block();
		assertEquals(PermissionBehavior.ALLOW, decision.getBehavior());
	}
}
