package org.ecnumc.clarinet.convert;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.ecnumc.clarinet.ClarinetLogger;
import org.ecnumc.clarinet.repository.CustomerRepository;

import java.io.InputStream;

@SuppressWarnings("java:S2187")
public class ContentConvertTest {
	public static void main(String[] args) {
		SqlSessionFactory sqlSessionFactory;
		try {
			String resource = "mybatis-config.xml";
			InputStream inputStream = Resources.getResourceAsStream(resource);
			sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
		} catch (Exception e) {
			throw new IllegalStateException("初始化 MyBatis 失败", e);
		}
		CustomerRepository customerRepository = new CustomerRepository(sqlSessionFactory);
		test1(customerRepository);
	}

	private static void test1(CustomerRepository customerRepository) {
		ClarinetLogger.info(ContentConvert.encode("[CQ:at,qq=1124171308] 这个问题怎么解决？", customerRepository));
		ClarinetLogger.info(ContentConvert.encode("[CQ:at,qq=2080659695] 和 [CQ:at,qq=460110340] 是这个群的管理。", customerRepository));
		ClarinetLogger.info(ContentConvert.encode("请问一下……", customerRepository));
		ClarinetLogger.info(ContentConvert.encode("[CQ:at,qq=3950262771]", customerRepository));
	}
}
