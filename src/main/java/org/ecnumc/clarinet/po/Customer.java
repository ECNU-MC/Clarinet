package org.ecnumc.clarinet.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * C 端客户持久化对象
 *
 * @author liudongyu
 */
@TableName("customer")
@Data
public class Customer {
	@TableId
	private Integer id;

	@TableField
	private String uid;

	@TableField
	private String qq;

	@TableField
	private Date createdAt;
}
