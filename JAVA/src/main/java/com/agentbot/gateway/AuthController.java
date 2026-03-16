package com.agentbot.gateway;

import com.agentbot.config.AgentbotProperties;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final AgentbotProperties properties;

  public AuthController(AgentbotProperties properties) {
    this.properties = properties;
  }

  @PostMapping("/login")
  public Map<String, Object> login(@RequestBody LoginRequest request, HttpSession session) {
    if (!properties.isAuthEnabled()) {
      return Map.of("enabled", false, "authenticated", true, "username", properties.getAuthUsername());
    }
    String username = request == null ? "" : safe(request.getUsername());
    String password = request == null ? "" : safe(request.getPassword());
    if (properties.getAuthUsername().equals(username) && properties.getAuthPassword().equals(password)) {

      session.setAttribute("AUTH_USER", username);
      return Map.of("enabled", true, "authenticated", true, "username", username);
    }
    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid credentials");
  }

  @PostMapping("/logout")
  public Map<String, Object> logout(HttpSession session) {
    if (session != null) {
      session.invalidate();
    }
    return Map.of("ok", true);
  }

  @GetMapping("/me")
  public Map<String, Object> me(HttpSession session) {
    if (!properties.isAuthEnabled()) {
      return Map.of("enabled", false, "authenticated", true, "username", properties.getAuthUsername());
    }

    Object user = session == null ? null : session.getAttribute("AUTH_USER");
    boolean authed = user != null;
    Map<String, Object> payload = new java.util.HashMap<>();
    payload.put("enabled", true);
    payload.put("authenticated", authed);
    payload.put("username", authed ? String.valueOf(user) : null);
    return payload;
  }


  private String safe(String value) {
    return value == null ? "" : value.trim();
  }

  public static class LoginRequest {
    private String username;
    private String password;

    public String getUsername() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }
  }
}
