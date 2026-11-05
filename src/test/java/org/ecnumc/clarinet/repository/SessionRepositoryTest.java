package org.ecnumc.clarinet.repository;

import org.ecnumc.clarinet.data.Message;
import org.ecnumc.clarinet.data.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 会话仓库单元测试，基于临时目录的 JSON 文件持久化，不依赖数据库
 *
 * @author liudongyu
 */
class SessionRepositoryTest {
	@TempDir
	private Path sessionFolder;

	/**
	 * 创建空会话应生成 ID 与创建时间
	 */
	@Test
	void testCreateEmptySession() {
		SessionRepository repository = new SessionRepository(this.sessionFolder);
		Session session = repository.createEmptySession();
		assertNotNull(session.getId());
		assertNotNull(session.getCreatedAt());
		assertTrue(session.getMessages().isEmpty());
	}

	/**
	 * 群消息上下文按时间升序保留最近 100 条
	 */
	@Test
	void testMessageContextKeepsLatestHundredInOrder() {
		SessionRepository repository = new SessionRepository(this.sessionFolder);
		for(int i = 1; i <= 105; i++) {
			repository.addMessage(
					"group-1", "id-" + i, "user", "plain",
					"uid-1", null, "nick", "m" + i, new Date(1_000L * i)
			);
		}
		List<Message> context = repository.getMessageContext("group-1");
		assertEquals(100, context.size());
		assertEquals("m6", context.get(0).getContent());
		assertEquals("m105", context.get(99).getContent());
	}

	/**
	 * 未知群的消息上下文为空
	 */
	@Test
	void testMessageContextForUnknownGroup() {
		SessionRepository repository = new SessionRepository(this.sessionFolder);
		assertTrue(repository.getMessageContext("unknown").isEmpty());
	}

	/**
	 * 已登记的回复消息可反查会话
	 */
	@Test
	void testGetSessionFromMessageId() {
		SessionRepository repository = new SessionRepository(this.sessionFolder);
		Session session = repository.createEmptySession();
		repository.addAgentMessageIdToCache("msg-1", session.getId());
		assertSame(session, repository.getSessionFromMessageId("msg-1"));
	}

	/**
	 * 未登记的回复消息反查会话返回 null
	 */
	@Test
	void testGetSessionFromMessageIdMissing() {
		SessionRepository repository = new SessionRepository(this.sessionFolder);
		assertNull(repository.getSessionFromMessageId("msg-missing"));
	}

	/**
	 * 会话文件不存在时返回 null
	 */
	@Test
	void testGetSessionMissingFile() {
		SessionRepository repository = new SessionRepository(this.sessionFolder);
		assertNull(repository.getSession("missing-session"));
	}

	/**
	 * 更新会话时应写入 JSON 持久化文件
	 */
	@Test
	void testUpdateSessionWritesFile() throws IOException {
		SessionRepository repository = new SessionRepository(this.sessionFolder);
		Session session = repository.createEmptySession();
		repository.updateSession(session);
		Path file = this.sessionFolder.resolve(session.getId() + ".json");
		assertTrue(Files.exists(file));
		assertTrue(Files.readString(file).contains(session.getId()));
	}
}
