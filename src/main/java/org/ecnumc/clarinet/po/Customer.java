package org.ecnumc.clarinet.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import javax.annotation.Nullable;
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

	@TableField @Nullable
	private String appellation;

	@TableField
	private String permission;

	@TableField
	private Date createdAt;
}
