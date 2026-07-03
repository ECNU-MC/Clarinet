package org.ecnumc.clarinet.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 解答记录
 *
 * @author liudongyu
 */
@TableName("assistance")
@Data
public class Assistance {
	@TableId
	private Integer id;

	@TableField
	private String uid;

	@TableField
	private String filename;

	@TableField
	private Boolean dealt;

	@TableField
	private String description;

	@TableField
	private Date createdAt;

	@TableField
	private Date updatedAt;
}
