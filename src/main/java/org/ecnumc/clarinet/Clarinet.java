package org.ecnumc.clarinet;

import io.agentscope.core.model.AnthropicChatModel;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.OpenAIChatModel;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.memory.compaction.CompactionConfig;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.ecnumc.clarinet.config.SystemConfig;
import org.ecnumc.clarinet.tool.ReadTool;
import org.ecnumc.clarinet.tool.RecentHistoryTool;

import java.io.InputStream;
import java.nio.file.Paths;

/**
 * 柯拉妮 - 入口
 *
 * @author liudongyu
 */
public final class Clarinet {
	/** 系统配置 */
	public static final SystemConfig SYSTEM_CONFIG;
	public static final String WORKSPACE = System.getProperty("user.home") + "\\.agentscope\\workspaces\\clarinet";

	/**
	 * 主函数
	 *
	 * @param args	参数
	 */
	public static void main(String[] args) {
		SqlSessionFactory sqlSessionFactory;
		try {
			String resource = "mybatis-config.xml";
			InputStream inputStream = Resources.getResourceAsStream(resource);
			sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
		} catch (Exception e) {
			throw new IllegalStateException("初始化 MyBatis 失败", e);
		}
		Toolkit toolkit = new Toolkit();
		toolkit.registerAgentTool(new ReadTool());
		toolkit.registerAgentTool(new RecentHistoryTool(sqlSessionFactory));
		try(HarnessAgent agent = HarnessAgent.builder()
				.name("clarinet")
				.model(getModel())
				.sysPrompt(getSysPrompt())
				.workspace(Paths.get(WORKSPACE))
				.toolkit(toolkit)
				.compaction(CompactionConfig.builder().triggerTokens(160_000).build())
				.build()) {
			// TODO
		}
	}

	private static Model getModel() {
		return switch(SYSTEM_CONFIG.getModelProtocol()) {
			case "openai" -> OpenAIChatModel.builder()
					.apiKey(SYSTEM_CONFIG.getApiKey())
					.baseUrl(SYSTEM_CONFIG.getBaseUrl())
					.modelName(SYSTEM_CONFIG.getModelName())
					.stream(false)
					.build();
			case "anthropic" -> AnthropicChatModel.builder()
					.apiKey(SYSTEM_CONFIG.getApiKey())
					.baseUrl(SYSTEM_CONFIG.getBaseUrl())
					.modelName(SYSTEM_CONFIG.getModelName())
					.stream(false)
					.build();
			default -> throw new IllegalStateException(
					"Unknown protocol \"" + SYSTEM_CONFIG.getModelProtocol() + "\", " +
					"please check your system config."
			);
		};
	}

	private static String getSysPrompt() {
		return "";
	}

	private Clarinet() {
	}

	static {
		SYSTEM_CONFIG = SystemConfig.parse(Paths.get(WORKSPACE, "system.json"));
	}
}
