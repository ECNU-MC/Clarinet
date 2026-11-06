package org.ecnumc.clarinet.repository;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.ecnumc.clarinet.bo.Permission;
import org.ecnumc.clarinet.mapper.CustomerMapper;
import org.ecnumc.clarinet.po.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 用户仓库单元测试，通过 mock 掉 SqlSession 链路避免访问真实数据库
 *
 * @author liudongyu
 */
@ExtendWith(MockitoExtension.class)
class CustomerRepositoryTest {
	@Mock
	private SqlSessionFactory sqlSessionFactory;

	@Mock
	private SqlSession sqlSession;

	@Mock
	private CustomerMapper customerMapper;

	private CustomerRepository repository;

	@BeforeEach
	void setUp() {
		when(this.sqlSessionFactory.openSession()).thenReturn(this.sqlSession);
		when(this.sqlSession.getMapper(CustomerMapper.class)).thenReturn(this.customerMapper);
		this.repository = new CustomerRepository(this.sqlSessionFactory);
	}

	/**
	 * 用户已存在时直接返回，不再插入
	 */
	@Test
	void testGetExistingCustomer() {
		Customer existing = new Customer();
		existing.setUid("uid-1");
		existing.setQq("1001");
		when(this.customerMapper.queryByQq("1001")).thenReturn(existing);

		Customer customer = this.repository.getOrCreateCustomerByQq("1001");

		assertSame(existing, customer);
		verify(this.customerMapper, never()).insert(any(Customer.class));
	}

	/**
	 * 用户不存在时应创建新用户并插入
	 */
	@Test
	void testCreateNewCustomer() {
		when(this.customerMapper.queryByQq("1002")).thenReturn(null);

		Customer customer = this.repository.getOrCreateCustomerByQq("1002");

		assertEquals("1002", customer.getQq());
		assertEquals(Permission.NORMAL.name(), customer.getPermission());
		assertEquals(UUID.nameUUIDFromBytes("1002".getBytes()).toString(), customer.getUid());
		verify(this.customerMapper).insert(customer);
	}
}
