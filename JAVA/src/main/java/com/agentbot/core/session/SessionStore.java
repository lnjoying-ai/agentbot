package com.agentbot.core.session;

import java.util.List;

public interface SessionStore {
  void append(String sessionKey, SessionMessage message);

  List<SessionMessage> loadRecent(String sessionKey, int limit);

  void clear(String sessionKey);
}
