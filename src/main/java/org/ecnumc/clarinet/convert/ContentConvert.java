package org.ecnumc.clarinet.convert;

import org.ecnumc.clarinet.repository.CustomerRepository;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 脱敏消息送入大模型，再将生成内容加回敏感信息返回
 *
 * @author liudongyu
 */
public final class ContentConvert {
	private static final Pattern PATTERN = Pattern.compile("\\[CQ:at,qq=(\\d+)]");

	/**
	 * 脱敏过程
	 * @param message 原始消息
	 * @param customerRepository 用户仓库
	 * @return 脱敏后的消息
	 */
	public static String encode(String message, CustomerRepository customerRepository) {
		Matcher matcher = PATTERN.matcher(message);
		StringBuilder sb = new StringBuilder();
		while (matcher.find()) {
			String qq = matcher.group(1);
			String uid = customerRepository.getOrCreateCustomerByQq(qq).getUid();
			matcher.appendReplacement(sb, "[at, uid=" + uid + "]");
		}
		matcher.appendTail(sb);
		return sb.toString();
	}

	private ContentConvert() {
	}
}
