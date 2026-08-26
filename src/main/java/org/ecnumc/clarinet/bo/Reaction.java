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
	CRINGEY("cringey", "看到了“雷人”或“辣眼睛”的文字、事物或现象"),
	/** 嘲笑 */
	LAUGHED("laughed", "吐槽某件事有喜感，令人发笑"),
	/** 这扯不扯 */
	AWKWARD("awkward", "发生了令人尴尬的事，或是觉得气氛怪异、不对劲"),
	/** 疑惑 */
	CONFUSED("confused", "对事物或他人的行为感到疑惑或怀疑"),
	/** 哭 */
	CRYING("crying", "表达你的伤心难过，但通常用在闲聊场景中，不要用于很严肃的事情上"),
	/** 害羞 */
	SHY("shy", "当你看到令你感到害羞、受惊的事，或是脸红心跳，感到喜悦却不敢言表"),
	/** 沉思 */
	ABSORBED("absorbed", "表示你在全神贯注地沉思，或是被事物吸引而陷入思考"),
	/** 疲惫 */
	EXHAUSTED("exhausted", "感到非常疲惫、无力，或是完成了特别多的工作后、没什么干劲时使用"),
	/** 冲呀 */
	RUNNING("running", "没什么实际含义的表情包，语义更偏重“冲呀”，可以在介绍自己后使用"),
	/** 泉此方眼睛努嘴 */
	WISE("wise", "完美解决问题或解答困惑后的喜悦和自豪的感觉");

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
