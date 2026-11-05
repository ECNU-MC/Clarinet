package org.ecnumc.clarinet.repository;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.ecnumc.clarinet.mapper.AssistanceMapper;
import org.ecnumc.clarinet.po.Assistance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 求助历史仓库单元测试，通过 mock 掉 SqlSession 链路避免访问真实数据库
 *
 * @author liudongyu
 */
@ExtendWith(MockitoExtension.class)
class AssistanceQueryRepositoryTest {
	private static final String DATE = "2026-01-02";

	@Mock
	private SqlSessionFactory sqlSessionFactory;

	@Mock
	private SqlSession sqlSession;

	@Mock
	private AssistanceMapper assistanceMapper;

	private AssistanceQueryRepository repository;

	@BeforeEach
	void setUp() {
		when(this.sqlSessionFactory.openSession()).thenReturn(this.sqlSession);
		when(this.sqlSession.getMapper(AssistanceMapper.class)).thenReturn(this.assistanceMapper);
		this.repository = new AssistanceQueryRepository(this.sqlSessionFactory);
	}

	/**
	 * 按用户查询最近求助记录
	 */
	@Test
	void testQueryByUid() {
		Assistance assistance = new Assistance();
		assistance.setUid("uid-1");
		assistance.setCreatedAt(new Date(1_700_000_000_000L));
		assistance.setUpdatedAt(new Date(1_700_000_000_000L));
		when(this.assistanceMapper.queryByUid("uid-1", 5)).thenReturn(List.of(assistance));

		assertEquals(List.of(assistance), this.repository.queryByUid("uid-1", 5));
	}

	/**
	 * 统计全部用户在指定时间范围内的求助总数
	 */
	@Test
	void testCountAllBetween() {
		when(this.assistanceMapper.countAllBetween(any(), any())).thenReturn(7L);

		assertEquals(7L, this.repository.query(null, DATE, null));
	}

	/**
	 * 统计指定用户在指定时间范围内的求助总数
	 */
	@Test
	void testCountByUidBetween() {
		when(this.assistanceMapper.countByUidBetween(eq("uid-1"), any(), any())).thenReturn(2L);

		assertEquals(2L, this.repository.query("uid-1", DATE, null));
	}

	/**
	 * 统计全部用户指定处理状态的求助总数
	 */
	@Test
	void testCountAllDealtBetween() {
		when(this.assistanceMapper.countAllDealtBetween(any(), any(), eq(0))).thenReturn(0L);

		assertEquals(0L, this.repository.query(null, DATE, 0));
	}

	/**
	 * 统计指定用户指定处理状态的求助总数
	 */
	@Test
	void testCountByUidDealtBetween() {
		when(this.assistanceMapper.countByUidDealtBetween(eq("uid-1"), any(), any(), eq(1))).thenReturn(1L);

		assertEquals(1L, this.repository.query("uid-1", DATE, 1));
	}

	/**
	 * 正向关键词检索
	 */
	@Test
	void testQueryByKeywords() {
		when(this.assistanceMapper.queryByKeywords("报错", 5)).thenReturn(List.of());

		assertEquals(List.of(), this.repository.queryByKeywords(List.of("报错"), List.of(), 5));
	}

	/**
	 * 正负向关键词检索应使用布尔模式
	 */
	@Test
	void testQueryByKeywordsWithExclude() {
		when(this.assistanceMapper.queryByKeywordsBooleanMode("+报错 -手动", 5)).thenReturn(List.of());

		assertEquals(List.of(), this.repository.queryByKeywords(List.of("报错"), List.of("手动"), 5));
	}
}
