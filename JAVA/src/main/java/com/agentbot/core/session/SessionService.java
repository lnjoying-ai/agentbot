package com.agentbot.core.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;



public class SessionService {

  private static final Logger log = LoggerFactory.getLogger(SessionService.class);
  private final SessionStore store;


  public SessionService(SessionStore store) {
    this.store = store;
  }

  public void appendUserMessage(String sessionKey, String content) {
    store.append(sessionKey, new SessionMessage("user", content, Instant.now()));
    log.debug("Session append user: sessionKey={}, contentLen={}", sessionKey, content == null ? 0 : content.length());
  }


  public void appendAssistantMessage(String sessionKey, String content) {
    store.append(sessionKey, new SessionMessage("assistant", content, Instant.now()));
    log.debug("Session append assistant: sessionKey={}, contentLen={}", sessionKey, content == null ? 0 : content.length());
  }




  public List<SessionMessage> getRecent(String sessionKey, int limit) {
    List<SessionMessage> recent = store.loadRecent(sessionKey, limit);
    log.debug("Session get recent: sessionKey={}, limit={}, count={}", sessionKey, limit, recent.size());
    return recent;
  }


  public void clear(String sessionKey) {
    store.clear(sessionKey);
    log.info("Session cleared: sessionKey={}", sessionKey);
  }

}
