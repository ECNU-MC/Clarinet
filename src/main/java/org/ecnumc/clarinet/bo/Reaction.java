package org.ecnumc.clarinet.bo;

import com.google.common.collect.ImmutableMap;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

/**
 * 表情包枚举
 *
 * @author liudongyu
 */
@Getter
@AllArgsConstructor
public enum Reaction {
	/** 震惊 */
	SHOCKED("shocked", "认为一件事超乎常理、违背认知，或是发生突然、猝不及防"),
	/** 这么强？！ */
	IMPRESSED("impressed", "别人的行为取得了超乎预料的效果，令人赞叹或印象深刻"),
	/** 老人地铁手机 */
	CRINGEY("cringey", "看到了“雷人”或“辣眼睛”的文字、事物或现象");

	private final String code;
	private final String description;

	private static final Map<String, Reaction> BY_CODE;

	/**
	 * 构建描述表格，供大模型使用
	 * @return 描述信息
	 */
	public static String buildDescription() {
		StringBuilder sb = new StringBuilder("| 编码 | 适用场景 |\n| :--- | :--- |\n");
		for(Reaction reaction: values()) {
			sb.append("| ").append(reaction.code).append(" | ").append(reaction.description).append(" |\n");
		}
		return sb.toString();
	}

	static {
		ImmutableMap.Builder<String, Reaction> builder = ImmutableMap.builder();
		for(Reaction reaction: values()) {
			builder.put(reaction.getCode(), reaction);
		}
		BY_CODE = builder.build();
	}
}
