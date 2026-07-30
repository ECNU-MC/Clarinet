package org.ecnumc.clarinet.repository;

import org.apache.ibatis.session.SqlSessionFactory;
import org.ecnumc.clarinet.ClarinetLogger;
import org.ecnumc.clarinet.bo.Permission;
import org.ecnumc.clarinet.mapper.CustomerMapper;
import org.ecnumc.clarinet.po.Customer;

import java.util.UUID;

/**
 * 用户仓库
 *
 * @author liudongyu
 */
public class CustomerRepository extends BaseRepository<CustomerMapper> {
	/**
	 * 构造函数
	 * @param sqlSessionFactory SQL 上下文
	 */
	public CustomerRepository(SqlSessionFactory sqlSessionFactory) {
		super(sqlSessionFactory, CustomerMapper.class);
	}

	/**
	 * 查询或新增用户
	 * @param qq 用户 QQ
	 * @return 用户
	 */
	public Customer getOrCreateCustomerByQq(String qq) {
		return this.execute(mapper -> {
			Customer customer = mapper.queryByQq(qq);
			if(customer == null) {
				ClarinetLogger.info("Adding a new customer " + qq);
				customer = new Customer();
				customer.setQq(qq);
				customer.setPermission(Permission.NORMAL.name());
				customer.setUid(UUID.nameUUIDFromBytes(qq.getBytes()).toString());
				mapper.insert(customer);
			}
			return customer;
		});
	}
}
