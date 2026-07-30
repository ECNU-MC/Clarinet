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
@TableName("agendum")
@Data
public class Agendum {
	@TableId
	private Integer id;

	@TableField
	private String sessionId;

	@TableField
	private String content;

	@TableField
	private Boolean dealt;

	@TableField @Nullable
	private Date notificationTime;

	@TableField
	private Date createdAt;

	@TableField
	private Date updatedAt;
}
