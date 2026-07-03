package org.ecnumc.clarinet.convert;

import com.alibaba.fastjson2.JSON;
import org.ecnumc.clarinet.po.Assistance;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 求助历史转换器
 *
 * @author liudongyu
 */
public final class AssistanceConvert {
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	/**
	 * 将求助记录转化为 JSON 字符串供大模型读取
	 * @param assistance 求助记录
	 * @return 结构化 JSON 字符串
	 */
	public static String toJsonString(Assistance assistance) {
		return JSON.toJSONString(Map.of(
				"uid", assistance.getUid(),
				"filename", assistance.getFilename(),
				"dealt", assistance.getDealt(),
				"description", assistance.getDescription(),
				"createdAt", assistance.getCreatedAt().toInstant().atZone(ZoneId.systemDefault()).format(FORMATTER),
				"updatedAt", assistance.getUpdatedAt().toInstant().atZone(ZoneId.systemDefault()).format(FORMATTER)
		));
	}

	private AssistanceConvert() {
	}
}
