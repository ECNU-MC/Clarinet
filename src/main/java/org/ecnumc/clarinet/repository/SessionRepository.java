package org.ecnumc.clarinet.repository;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.api.client.util.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import org.ecnumc.clarinet.ClarinetLogger;
import org.ecnumc.clarinet.data.Message;
import org.ecnumc.clarinet.data.Session;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 会话仓库
 * <br/>
 * 不同于其它仓库，这里使用 JSON 文件而非数据库，对会话进行持久化
 *
 * @author liudongyu
 */
public class SessionRepository {
	private static final int MAX_QUEUE_SIZE = 100;

	/** 存储位置 */
	private final Path sessionFolder;

	/** 由 agent 发送的消息 ID 反查会话 ID */
	private final Cache<String, String> messageSessionLookup = Caffeine.newBuilder()
			.maximumSize(1024)
			.expireAfterAccess(24, TimeUnit.HOURS)
			.build();

	/** 会话 ID 查找会话内容 */
	private final Cache<String, Session> sessions = Caffeine.newBuilder()
			.maximumSize(1024)
			.expireAfterAccess(12, TimeUnit.HOURS)
			.build();

	/** 消息上下文 */
	private final Map<String, PriorityQueue<Message>> messages = Maps.newHashMap();

	/** 随机数发生器（线程安全，可以随意调用） */
	private final SecureRandom random = new SecureRandom();

	/**
	 * 构造函数
	 * @param sessionFolder 会话存储位置
	 */
	public SessionRepository(Path sessionFolder) {
		this.sessionFolder = sessionFolder;
	}

	/**
	 * 收到群聊消息，存入上下文。<br/>
	 * 会阻塞消息队列，避免并发修改。
	 *
	 * @param groupId 群号
	 * @param id 消息 ID
	 * @param role 消息角色
	 * @param type 消息类型
	 * @param uid 用户 ID
	 * @param nickname 用户昵称
	 * @param content 消息内容
	 * @param sentAt 发送时间
	 *
	 * @return 消息
	 */
	public Message addMessage(String groupId, String id, String role, String type,
							  @Nullable String uid, @Nullable String nickname, String content, Date sentAt) {
		Message message = new Message(id, role, type, uid, nickname, content, sentAt);
		synchronized (this.messages) {
			PriorityQueue<Message> pq = this.messages.computeIfAbsent(groupId, ignored -> Queues.newPriorityQueue());
			pq.add(message);
			while(pq.size() > MAX_QUEUE_SIZE) {
				pq.poll();
			}
		}
		return message;
	}

	/**
	 * 获取消息上下文（最近 100 条）
	 * @param groupId 群号
	 * @return 消息上下文
	 */
	public List<Message> getMessageContext(String groupId) {
		PriorityQueue<Message> pq;
		synchronized (this.messages) {
			pq = this.messages.get(groupId);
			if(pq == null) {
				pq = Queues.newPriorityQueue();
			} else {
				pq = Queues.newPriorityQueue(pq);
			}
		}
		List<Message> ret = Lists.newArrayList();
		while(!pq.isEmpty()) {
			ret.add(pq.poll());
		}
		return ret;
	}

	/**
	 * 根据 agent 曾发送的 message id 反查 session<br/>
	 * 如果缓存已过期，或不存在对应的 session，则返回 null
	 *
	 * @param messageId 消息 ID
	 * @return session
	 */
	@Nullable
	public Session getSessionFromMessageId(String messageId) {
		String sessionId = this.messageSessionLookup.getIfPresent(messageId);
		if(sessionId == null) {
			return null;
		}
		return this.getSession(sessionId);
	}

	/**
	 * 根据 session id 获取会话详情<br/>
	 * 如果缓存不存在，且持久化文件均未匹配，则返回 null
	 *
	 * @param sessionId 会话 ID
	 * @return session
	 */
	@Nullable
	public Session getSession(String sessionId) {
		Session session = this.sessions.getIfPresent(sessionId);
		if(session != null) {
			return session;
		}
		Path filePath = this.sessionFolder.resolve(sessionId + ".json");
		if (!Files.exists(filePath)) {
			ClarinetLogger.error("会话不存在！会话 ID：" + sessionId);
			return null;
		}
		try {
			String json = Files.readString(filePath);
			session = JSON.parseObject(json, Session.class);
			this.sessions.put(sessionId, session);
			return session;
		} catch (IOException e) {
			ClarinetLogger.error("加载会话失败！会话 ID：" + sessionId, e);
		} catch (JSONException e) {
			ClarinetLogger.error("解析会话失败！会话 ID：" + sessionId, e);
		}
		return null;
	}

	/**
	 * 更新会话，收到新消息或 agent 回复时触发<br/>
	 * 仅更新持久化文件即可，对象不变时，缓存会自动更新
	 *
	 * @param session 会话
	 */
	public void updateSession(Session session) {
		Path filePath = this.sessionFolder.resolve(session.getId() + ".json");
		try {
			// 确保目录存在
			if (!Files.exists(this.sessionFolder)) {
				Files.createDirectories(this.sessionFolder);
			}
			// 将 Session 转换为持久化 JSON 字符串
			String json = JSON.toJSONString(session);
			// 写入文件（覆盖原文件）
			Files.writeString(filePath, json, java.nio.charset.StandardCharsets.UTF_8);
		} catch (IOException e) {
			ClarinetLogger.error("更新会话文件失败！会话 ID：" + session.getId(), e);
		}
	}

	/**
	 * 添加 agent 回复 message id 到 session id 映射到缓存
	 * @param messageId 智能体回复消息 ID
	 * @param sessionId 会话 ID
	 */
	public void addAgentMessageIdToCache(String messageId, String sessionId) {
		this.messageSessionLookup.put(messageId, sessionId);
	}


	/**
	 * 创建空会话
	 *
	 * @return 空会话
	 */
	public Session createEmptySession() {
		Date date = new Date();
		byte[] bytes = ByteBuffer.allocate((Long.SIZE + Integer.SIZE) / Byte.SIZE)
				.putLong(date.getTime())
				.putInt(this.random.nextInt())
				.array();
		Session session = Session.builder()
				.id(String.valueOf(UUID.nameUUIDFromBytes(bytes)))
				.messages(Lists.newArrayList())
				.createdAt(date)
				.build();
		this.sessions.put(session.getId(), session);
		return session;
	}
}
