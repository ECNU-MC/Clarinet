package org.ecnumc.clarinet.prompt;

import org.apache.ibatis.io.Resources;

import java.io.InputStream;

/**
 * 柯拉妮默认系统提示词
 *
 * @author liudongyu
 */
public interface DefaultSystemPrompt {
	String PATTERN_WORKSPACE = "${workspace}";

	static byte[] getDefaultSystemPrompt() {
		try(InputStream is = Resources.getResourceAsStream("prompts/default-system-prompt.md")) {
			return is.readAllBytes();
		} catch (Exception e) {
			throw new IllegalStateException("初始化系统提示词失败", e);
		}
	}
}
