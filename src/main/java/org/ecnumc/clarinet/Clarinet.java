package org.ecnumc.clarinet;

import com.sun.net.httpserver.HttpServer;
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
import org.ecnumc.clarinet.handler.MessageHandler;
import org.ecnumc.clarinet.repository.CustomerRepository;
import org.ecnumc.clarinet.repository.SessionRepository;
import org.ecnumc.clarinet.tool.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

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
		toolkit.registerAgentTool(new Base64DecodeTool());
		toolkit.registerAgentTool(new Base64EncodeTool());
		toolkit.registerAgentTool(new ReadTool());
		toolkit.registerAgentTool(new RecentAssistanceTool(sqlSessionFactory));
		toolkit.registerAgentTool(new SendMessageTool());
		toolkit.registerAgentTool(new StatsTool(sqlSessionFactory));
		try(HarnessAgent agent = HarnessAgent.builder()
				.name("clarinet")
				.model(getModel())
				.sysPrompt(getSysPrompt())
				.workspace(Paths.get(WORKSPACE))
				.toolkit(toolkit)
				.compaction(CompactionConfig.builder().triggerTokens(160_000).build())
				.build()) {
			HttpServer server = HttpServer.create(new InetSocketAddress(SYSTEM_CONFIG.getPort()), 0);
			server.createContext("/api/msg", new MessageHandler(
					agent,
					new CustomerRepository(sqlSessionFactory),
					new SessionRepository(Paths.get(WORKSPACE, "sessions"))
			));
			AtomicInteger counter = new AtomicInteger(0);
			server.setExecutor(Executors.newFixedThreadPool(10, job -> new Thread(
					job, "clarinet-http-handler-" + counter.getAndIncrement()
			)));
			final CountDownLatch latch = new CountDownLatch(1);
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				ClarinetLogger.info("Shutting down...");
				server.stop(0);
				latch.countDown();
			}));
			server.start();
			ClarinetLogger.info("Server started.");
			latch.await();
		} catch (IOException e) {
			ClarinetLogger.error(e);
		} catch (InterruptedException e) {
			ClarinetLogger.error(e);
			Thread.currentThread().interrupt();
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
		try {
			Files.createDirectories(Paths.get(WORKSPACE));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		SYSTEM_CONFIG = SystemConfig.parse(Paths.get(WORKSPACE, "system.json"));
	}
}
