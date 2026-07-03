package org.ecnumc.clarinet;

import com.alibaba.fastjson.JSON;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.ecnumc.clarinet.repository.AssistanceQueryRepository;

import java.io.InputStream;

@SuppressWarnings("java:S2187")
public class DatabaseTest {
	public static void main(String[] args) {
		test1();
	}

	private static void test1() {
		SqlSessionFactory sqlSessionFactory;
		try {
			String resource = "mybatis-config.xml";
			InputStream inputStream = Resources.getResourceAsStream(resource);
			sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
		} catch (Exception e) {
			throw new IllegalStateException("初始化 MyBatis 失败", e);
		}
		AssistanceQueryRepository repository = new AssistanceQueryRepository(sqlSessionFactory);
		ClarinetLogger.info(JSON.toJSONString(repository.queryByUid("00000000-0000-0000-0000-000000000000", 3)));
	}
}
