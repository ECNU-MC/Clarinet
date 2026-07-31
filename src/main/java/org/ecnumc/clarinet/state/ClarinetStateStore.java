package org.ecnumc.clarinet.state;

import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.state.State;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 禁用原版 state store
 *
 * @author liudongyu
 */
public final class ClarinetStateStore implements AgentStateStore {
	@Override
	public void save(String userId, String sessionId, String key, State value) {
		// do nothing
	}

	@Override
	public void save(String userId, String sessionId, String key, List<? extends State> values) {
		// do nothing
	}

	@Override
	public <T extends State> Optional<T> get(String userId, String sessionId, String key, Class<T> type) {
		return Optional.empty();
	}

	@Override
	public <T extends State> List<T> getList(String userId, String sessionId, String key, Class<T> itemType) {
		return List.of();
	}

	@Override
	public boolean exists(String userId, String sessionId) {
		return false;
	}

	@Override
	public void delete(String userId, String sessionId) {
		// do nothing
	}

	@Override
	public Set<String> listSessionIds(String userId) {
		return Set.of();
	}
}
