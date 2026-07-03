package org.ecnumc.clarinet.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import java.util.function.Function;

/**
 * 仓库类基类
 * @param <T> Mapper 类型
 *
 * @author liudongyu
 */
public abstract class BaseRepository<T extends BaseMapper<?>> {
	protected SqlSessionFactory sqlSessionFactory;
	protected Class<T> mapperClass;

	protected BaseRepository(SqlSessionFactory sqlSessionFactory, Class<T> mapperClass) {
		this.sqlSessionFactory = sqlSessionFactory;
		this.mapperClass = mapperClass;
	}

	// 模板方法，封装 Mapper 的获取和会话管理
	protected <R> R execute(MapperFunction<T, R> action) {
		try (SqlSession session = this.sqlSessionFactory.openSession()) {
			T mapper = session.getMapper(this.mapperClass);
			return action.apply(mapper);
		}
	}

	/**
	 * 通过 Mapper 获取对象的方法
	 * @param <M> Mapper 类型
	 * @param <R> 结果类型
	 */
	@FunctionalInterface
	public interface MapperFunction<M, R> extends Function<M, R> {
		@Override
		R apply(M mapper);
	}
}
