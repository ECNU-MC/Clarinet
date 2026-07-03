package org.ecnumc.clarinet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.ecnumc.clarinet.po.Customer;

/**
 * C 端客户表 Mapper
 *
 * @author liudongyu
 */
public interface CustomerMapper extends BaseMapper<Customer> {
	/**
	 * 根据 ID 查询用户
	 * @param uid 用户 ID
	 * @return 用户
	 */
	Customer queryByUid(@Param("uid") String uid);

	/**
	 * 根据 QQ 查询用户
	 * @param qq 用户 QQ
	 * @return 用户
	 */
	Customer queryByQq(@Param("qq") String qq);
}
