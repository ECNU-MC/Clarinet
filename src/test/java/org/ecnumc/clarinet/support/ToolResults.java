package org.ecnumc.clarinet.support;

import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolResultBlock;

/**
 * 工具结果断言辅助工具，将工具输出的文本内容块拼接为字符串供测试断言
 *
 * @author liudongyu
 */
public final class ToolResults {
	/**
	 * 提取工具结果中的全部文本内容，多个内容块之间以换行拼接
	 *
	 * @param result 工具结果
	 * @return 拼接后的文本内容
	 */
	public static String text(ToolResultBlock result) {
		StringBuilder sb = new StringBuilder();
		for(ContentBlock block : result.getOutput()) {
			if(block instanceof TextBlock textBlock) {
				if(!sb.isEmpty()) {
					sb.append('\n');
				}
				sb.append(textBlock.getText());
			}
		}
		return sb.toString();
	}

	private ToolResults() {
	}
}
