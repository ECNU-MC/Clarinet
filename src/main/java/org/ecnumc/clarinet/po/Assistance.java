package org.ecnumc.clarinet.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import javax.annotation.Nullable;
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
	private String sessionId;

	@TableField
	private String uid;

	@TableField
	private String filename;

	@TableField
	private Boolean dealt;

	@TableField
	private String description;

	@TableField @Nullable
	private String parentSessionId;

	@TableField
	private Date createdAt;

	@TableField
	private Date updatedAt;
}
