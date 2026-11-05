package org.ecnumc.clarinet.convert;

import org.ecnumc.clarinet.po.Customer;
import org.ecnumc.clarinet.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * 消息脱敏转换单元测试
 *
 * @author liudongyu
 */
@ExtendWith(MockitoExtension.class)
class ContentConvertTest {
	@Mock
	private CustomerRepository customerRepository;

	/**
	 * 单个 at 引用应被替换为不含 QQ 号的 uid 形式
	 */
	@Test
	void testReplaceSingleAt() {
		when(this.customerRepository.getOrCreateCustomerByQq("1124171308"))
				.thenReturn(this.customer("uid-1124"));
		assertEquals(
				"[at, uid=uid-1124] 这个问题怎么解决？",
				ContentConvert.encode("[CQ:at,qq=1124171308] 这个问题怎么解决？", this.customerRepository)
		);
	}

	/**
	 * 多个 at 引用应全部替换
	 */
	@Test
	void testReplaceMultipleAts() {
		when(this.customerRepository.getOrCreateCustomerByQq("2080659695"))
				.thenReturn(this.customer("uid-2080"));
		when(this.customerRepository.getOrCreateCustomerByQq("460110340"))
				.thenReturn(this.customer("uid-4601"));
		assertEquals(
				"[at, uid=uid-2080] 和 [at, uid=uid-4601] 是这个群的管理。",
				ContentConvert.encode(
						"[CQ:at,qq=2080659695] 和 [CQ:at,qq=460110340] 是这个群的管理。",
						this.customerRepository
				)
		);
	}

	/**
	 * 普通消息不应被修改
	 */
	@Test
	void testPlainTextUnchanged() {
		assertEquals(
				"请问一下……",
				ContentConvert.encode("请问一下……", this.customerRepository)
		);
	}

	/**
	 * 仅含 at 引用、无正文的消息也应被替换
	 */
	@Test
	void testAtWithoutContent() {
		when(this.customerRepository.getOrCreateCustomerByQq("3950262771"))
				.thenReturn(this.customer("uid-3950"));
		assertEquals(
				"[at, uid=uid-3950]",
				ContentConvert.encode("[CQ:at,qq=3950262771]", this.customerRepository)
		);
	}

	private Customer customer(String uid) {
		Customer customer = new Customer();
		customer.setUid(uid);
		return customer;
	}
}
