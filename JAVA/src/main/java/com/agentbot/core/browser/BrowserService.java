package com.agentbot.core.browser;

import com.agentbot.config.AgentbotProperties;
import com.agentbot.core.tools.AntiBotConfig;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Proxy;
import com.microsoft.playwright.options.WaitUntilState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.ArrayList;
import java.util.HashMap;


import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class BrowserService {
  private static final Logger log = LoggerFactory.getLogger(BrowserService.class);
  private static final List<String> DEFAULT_BLOCK_RESOURCE_TYPES = List.of("image", "font", "media");
  private static final List<String> DEFAULT_BLOCK_URL_PATTERNS = List.of("*://*/*analytics*", "*://*/*gtag*", "*://*/*ads*");

  private final Path workspaceDir;
  private final AgentbotProperties.Browser browserConfig;
  private final AntiBotConfig antiBot;
  private final int controlPort;
  private final Random random = new Random();
  private ExtensionRelayServer relayServer;

  private Playwright playwright;

  private BrowserType browserType;
  private final CdpSessionManager cdpSessionManager = new CdpSessionManager();
  private final PlaywrightSessionManager playwrightSessionManager = new PlaywrightSessionManager();
  private final Map<String, ProfileRuntime> runtimes = new ConcurrentHashMap<>();


  public BrowserService(Path workspaceDir, AgentbotProperties.Browser browserConfig, AntiBotConfig antiBot, int basePort) {
    this.workspaceDir = workspaceDir;
    this.browserConfig = browserConfig;
    this.antiBot = antiBot;
    int configuredPort = browserConfig == null ? 0 : browserConfig.getControlPort();
    this.controlPort = configuredPort > 0 ? configuredPort : Math.max(1024, basePort + 2);
  }

  public int getControlPort() {
    return controlPort;
  }

  public synchronized void startExtensionRelayIfConfigured() {
    Integer port = resolveExtensionRelayPort();
    if (port == null || port <= 0) return;
    if (relayServer != null && relayServer.isRunning()) return;
    relayServer = new ExtensionRelayServer(port);
    relayServer.start();
  }

  public synchronized void stopExtensionRelay() {
    if (relayServer != null) {
      relayServer.stop();
      relayServer = null;
    }
  }

  private Integer resolveExtensionRelayPort() {
    Map<String, AgentbotProperties.BrowserProfile> profiles = resolveProfiles();
    Integer resolved = null;
    for (Map.Entry<String, AgentbotProperties.BrowserProfile> entry : profiles.entrySet()) {
      AgentbotProperties.BrowserProfile profile = entry.getValue();
      String driver = safeText(profile.getDriver()).toLowerCase();
      if (!"extension".equals(driver)) continue;
      String cdpUrl = safeText(profile.getCdpUrl());
      if (cdpUrl.isBlank()) continue;
      URI uri = URI.create(cdpUrl);
      String host = uri.getHost();
      if (host == null || !("127.0.0.1".equals(host) || "localhost".equalsIgnoreCase(host))) {
        throw new RuntimeException("Extension relay must bind to loopback cdpUrl (profile=" + entry.getKey() + ")");
      }
      int port = uri.getPort();
      if (port <= 0) continue;
      if (resolved == null) {
        resolved = port;
      } else if (!resolved.equals(port)) {
        throw new RuntimeException("Multiple extension relay ports detected; use a single port for extension profiles.");
      }
    }
    return resolved;
  }

  public boolean isEnabled() {
    return browserConfig == null || browserConfig.isEnabled();
  }

  public String getSandboxBridgeUrl() {
    return browserConfig == null ? "" : safeText(browserConfig.getSandboxBridgeUrl());
  }

  public String getNodeBridgeUrl() {
    return browserConfig == null ? "" : safeText(browserConfig.getNodeBridgeUrl());
  }


  public BrowserStatus status() {
    List<BrowserProfileStatus> profiles = new ArrayList<>();
    Map<String, AgentbotProperties.BrowserProfile> configured = resolveProfiles();
    String defaultProfile = resolveDefaultProfile(configured);
    for (Map.Entry<String, AgentbotProperties.BrowserProfile> entry : configured.entrySet()) {
      String name = entry.getKey();
      ProfileRuntime runtime = runtimes.get(name);
      boolean running = runtime != null && runtime.isRunning();
      int tabCount = runtime == null ? 0 : runtime.listTabs().size();
      profiles.add(new BrowserProfileStatus(name, running, tabCount, name.equals(defaultProfile)));
    }
    return new BrowserStatus(isEnabled(), controlPort, defaultProfile, profiles);
  }

  public void startProfile(String profileName) {
    try {
      ensureProfile(profileName);
      audit("start", resolveProfileName(profileName), null, true, "profile started");
    } catch (RuntimeException e) {
      audit("start", profileName, null, false, e.getMessage());
      throw e;
    }
  }

  public void stopProfile(String profileName) {
    String resolved = resolveProfileName(profileName);
    try {
      ProfileRuntime runtime = runtimes.remove(resolved);
      if (runtime != null) {
        runtime.close();
      }
      audit("stop", resolved, null, true, "profile stopped");
    } catch (RuntimeException e) {
      audit("stop", resolved, null, false, e.getMessage());
      throw e;
    }
  }


  public List<BrowserTabInfo> listTabs(String profileName) {
    String resolved = resolveProfileName(profileName);
    try {
      ProfileRuntime runtime = ensureProfile(resolved);
      List<BrowserTabInfo> result = runtime.listTabs();
      audit("tabs", resolved, null, true, "count=" + result.size());
      return result;
    } catch (RuntimeException e) {
      audit("tabs", resolved, null, false, e.getMessage());
      throw e;
    }
  }

  public BrowserTabInfo openTab(String profileName, String url) {
    String resolved = resolveProfileName(profileName);
    try {
      ProfileRuntime runtime = ensureProfile(resolved);
      BrowserTabInfo tab = runtime.openTab(url);
      audit("open", resolved, tab.targetId(), true, "url=" + safeText(url));
      return tab;
    } catch (RuntimeException e) {
      audit("open", resolved, null, false, e.getMessage());
      throw e;
    }
  }

  public void focusTab(String profileName, String targetId) {
    String resolved = resolveProfileName(profileName);
    try {
      ProfileRuntime runtime = ensureProfile(resolved);
      runtime.focusTab(targetId);
      audit("focus", resolved, targetId, true, "focused");
    } catch (RuntimeException e) {
      audit("focus", resolved, targetId, false, e.getMessage());
      throw e;
    }
  }

  public void closeTab(String profileName, String targetId) {
    String resolved = resolveProfileName(profileName);
    try {
      ProfileRuntime runtime = ensureProfile(resolved);
      runtime.closeTab(targetId);
      audit("close", resolved, targetId, true, "closed");
    } catch (RuntimeException e) {
      audit("close", resolved, targetId, false, e.getMessage());
      throw e;
    }
  }

  public BrowserSnapshot snapshot(String profileName, String targetId, String format) {
    String resolved = resolveProfileName(profileName);
    try {
      ProfileRuntime runtime = ensureProfile(resolved);
      BrowserSnapshot snapshot = runtime.snapshot(targetId, format);
      audit("snapshot", resolved, snapshot.targetId(), true, "format=" + snapshot.format());
      return snapshot;
    } catch (RuntimeException e) {
      audit("snapshot", resolved, targetId, false, e.getMessage());
      throw e;
    }
  }


  public BrowserActionResult act(String profileName, String targetId, String ref, String kind, String text, String key) {
    String resolved = resolveProfileName(profileName);
    try {
      ProfileRuntime runtime = ensureProfile(resolved);
      BrowserActionResult result = runtime.act(targetId, ref, kind, text, key);
      audit("act", resolved, targetId, result.ok(), result.message());
      return result;
    } catch (RuntimeException e) {
      audit("act", resolved, targetId, false, e.getMessage());
      throw e;
    }
  }

  public BrowserActionResult navigate(String profileName, String targetId, String url) {
    String resolved = resolveProfileName(profileName);
    try {
      ProfileRuntime runtime = ensureProfile(resolved);
      BrowserActionResult result = runtime.navigate(targetId, url);
      audit("navigate", resolved, targetId, result.ok(), result.message());
      return result;
    } catch (RuntimeException e) {
      audit("navigate", resolved, targetId, false, e.getMessage());
      throw e;
    }
  }

  public BrowserActionResult click(String profileName, String targetId, String selector) {
    String resolved = resolveProfileName(profileName);
    try {
      ProfileRuntime runtime = ensureProfile(resolved);
      BrowserActionResult result = runtime.click(targetId, selector);
      audit("click", resolved, targetId, result.ok(), result.message());
      return result;
    } catch (RuntimeException e) {
      audit("click", resolved, targetId, false, e.getMessage());
      throw e;
    }
  }

  public BrowserActionResult type(String profileName, String targetId, String selector, String text) {
    String resolved = resolveProfileName(profileName);
    try {
      ProfileRuntime runtime = ensureProfile(resolved);
      BrowserActionResult result = runtime.type(targetId, selector, text);
      audit("type", resolved, targetId, result.ok(), result.message());
      return result;
    } catch (RuntimeException e) {
      audit("type", resolved, targetId, false, e.getMessage());
      throw e;
    }
  }

  public BrowserActionResult upload(String profileName, String targetId, String selector, String filePath, String ref) {
    String resolved = resolveProfileName(profileName);
    try {
      ProfileRuntime runtime = ensureProfile(resolved);
      BrowserActionResult result = runtime.upload(targetId, selector, filePath, ref);
      audit("upload", resolved, targetId, result.ok(), result.message());
      return result;
    } catch (RuntimeException e) {
      audit("upload", resolved, targetId, false, e.getMessage());
      throw e;
    }
  }

  public BrowserActionResult content(String profileName, String targetId) {
    String resolved = resolveProfileName(profileName);
    try {
      ProfileRuntime runtime = ensureProfile(resolved);
      BrowserActionResult result = runtime.content(targetId);
      audit("content", resolved, targetId, result.ok(), result.message());
      return result;
    } catch (RuntimeException e) {
      audit("content", resolved, targetId, false, e.getMessage());
      throw e;
    }
  }

  public BrowserActionResult screenshot(String profileName, String targetId) {
    String resolved = resolveProfileName(profileName);
    try {
      ProfileRuntime runtime = ensureProfile(resolved);
      BrowserActionResult result = runtime.screenshot(targetId);
      audit("screenshot", resolved, targetId, result.ok(), result.message());
      return result;
    } catch (RuntimeException e) {
      audit("screenshot", resolved, targetId, false, e.getMessage());
      throw e;
    }
  }


  private synchronized void ensurePlaywright() {
    if (playwright != null) return;
    playwright = Playwright.create();
    browserType = playwright.chromium();
  }

  private ProfileRuntime ensureProfile(String profileName) {
    if (!isEnabled()) {
      throw new RuntimeException("Browser control is disabled by configuration.");
    }
    String resolved = resolveProfileName(profileName);
    ProfileRuntime existing = runtimes.get(resolved);
    if (existing != null && existing.isRunning()) return existing;
    ProfileRuntime created = createProfileRuntime(resolved);
    runtimes.put(resolved, created);
    return created;
  }

  private ProfileRuntime createProfileRuntime(String profileName) {
    ensurePlaywright();
    Map<String, AgentbotProperties.BrowserProfile> profiles = resolveProfiles();
    AgentbotProperties.BrowserProfile profile = profiles.get(profileName);
    if (profile == null) {
      throw new RuntimeException("Profile not found: " + profileName);
    }

    String driver = safeText(profile.getDriver()).toLowerCase();
    String cdpUrl = safeText(profile.getCdpUrl());
    String userDataDir = safeText(profile.getUserDataDir());
    boolean attachOnly = browserConfig != null && browserConfig.isAttachOnly();
    Path dataDir = userDataDir.isBlank()
        ? workspaceDir.resolve("browser").resolve("profiles").resolve(profileName)
        : Path.of(userDataDir);

    if ("extension".equals(driver) && !cdpUrl.isBlank() && !isLoopbackUrl(cdpUrl)) {
      throw new RuntimeException("extension driver requires loopback cdpUrl for profile=" + profileName);
    }

    if (!cdpUrl.isBlank() || "extension".equals(driver) || "remote".equals(driver)) {
      if (cdpUrl.isBlank()) {
        throw new RuntimeException("Profile " + profileName + " requires cdpUrl for driver=" + driver);
      }
      boolean remoteCdp = isRemoteCdp(cdpUrl);
      int timeoutMs = resolveRemoteCdpTimeoutMs(remoteCdp);
      if (remoteCdp) {
        log.warn("browser_cdp_remote profile={} cdpUrl={} timeoutMs={}", profileName, cdpUrl, timeoutMs);
      }
      CdpSessionManager.CdpSession session = cdpSessionManager.connect(browserType, cdpUrl, timeoutMs);
      BrowserContext context = session.context();
      Browser browser = session.browser();
      ProfileRuntime runtime = new ProfileRuntime(profileName, context, browser, true);
      runtime.applyAntiBot();
      return runtime;
    }

    if (attachOnly) {
      throw new RuntimeException("attachOnly is enabled; provide cdpUrl or use driver=extension/remote.");
    }

    BrowserType.LaunchPersistentContextOptions options = new BrowserType.LaunchPersistentContextOptions();

    if (browserConfig != null) {
      options.setHeadless(browserConfig.isHeadless());
      if (browserConfig.isNoSandbox()) {
        options.setArgs(List.of("--no-sandbox"));
      }
      String executablePath = safeText(browserConfig.getExecutablePath());
      if (!executablePath.isBlank()) {
        options.setExecutablePath(Path.of(executablePath));
      }

    }
    Proxy proxy = selectProxy(false);
    if (proxy != null) {
      options.setProxy(proxy);
    }
    applyContextOptions(options);

    BrowserContext context = playwrightSessionManager.launchPersistent(browserType, dataDir, options);

    ProfileRuntime runtime = new ProfileRuntime(profileName, context, context.browser(), false);
    runtime.applyAntiBot();
    return runtime;
  }

  private void applyContextOptions(BrowserType.LaunchPersistentContextOptions options) {
    String userAgent = safeText(antiBot.getUserAgent());
    if (!userAgent.isBlank()) options.setUserAgent(userAgent);
    String locale = safeText(antiBot.getLocale());
    if (!locale.isBlank()) options.setLocale(locale);
    String timezone = safeText(antiBot.getTimezoneId());
    if (!timezone.isBlank()) options.setTimezoneId(timezone);
    Map<String, String> headers = new HashMap<>(antiBot.getHeaders());
    if (!headers.isEmpty()) options.setExtraHTTPHeaders(headers);
  }

  private Map<String, AgentbotProperties.BrowserProfile> resolveProfiles() {
    Map<String, AgentbotProperties.BrowserProfile> profiles = browserConfig == null
        ? new HashMap<>()
        : new HashMap<>(browserConfig.getProfiles());
    if (!profiles.containsKey("agentbot")) {
      profiles.put("agentbot", new AgentbotProperties.BrowserProfile());
    }
    if (!profiles.containsKey("chrome")) {

      AgentbotProperties.BrowserProfile chrome = new AgentbotProperties.BrowserProfile();
      chrome.setDriver("extension");
      chrome.setCdpUrl("http://127.0.0.1:" + (controlPort + 1));
      chrome.setColor("#00AA00");
      profiles.put("chrome", chrome);
    }
    return profiles;
  }

  private String resolveDefaultProfile(Map<String, AgentbotProperties.BrowserProfile> profiles) {
    String configured = browserConfig == null ? "" : safeText(browserConfig.getDefaultProfile());
    if (!configured.isBlank() && profiles.containsKey(configured)) return configured;
    return profiles.containsKey("agentbot") ? "agentbot" : profiles.keySet().stream().findFirst().orElse("agentbot");

  }

  private String resolveProfileName(String profileName) {
    Map<String, AgentbotProperties.BrowserProfile> profiles = resolveProfiles();
    String configured = resolveDefaultProfile(profiles);
    if (profileName == null || profileName.trim().isEmpty()) return configured;
    String trimmed = profileName.trim();
    if (!profiles.containsKey(trimmed)) {
      throw new RuntimeException("Profile not found: " + trimmed);
    }
    return trimmed;
  }

  private Proxy selectProxy(boolean rotate) {
    if (!isAdvanced()) return null;
    List<String> proxies = antiBot.getProxies();
    if (proxies == null || proxies.isEmpty()) return null;
    return buildProxy(nextProxyUrl(proxies));
  }

  private String nextProxyUrl(List<String> proxies) {
    if (proxies == null || proxies.isEmpty()) return null;
    int idx = random.nextInt(proxies.size());
    return proxies.get(idx);
  }

  private Proxy buildProxy(String proxyUrl) {
    String raw = safeText(proxyUrl);
    if (raw.isBlank()) return null;
    String normalized = raw.contains("://") ? raw : "http://" + raw;
    URI uri = URI.create(normalized);
    String host = uri.getHost();
    int port = uri.getPort();
    if (host == null) return null;
    String server = uri.getScheme() + "://" + host + (port > 0 ? ":" + port : "");
    Proxy proxy = new Proxy(server);
    if (uri.getUserInfo() != null && uri.getUserInfo().contains(":")) {
      String[] parts = uri.getUserInfo().split(":", 2);
      proxy.setUsername(parts[0]);
      proxy.setPassword(parts[1]);
    }
    return proxy;
  }

  private boolean isEnhanced() {
    return antiBot.levelRank() >= 2;
  }

  private boolean isAdvanced() {
    return antiBot.levelRank() >= 3;
  }

  private String safeText(String text) {
    return text == null ? "" : text.trim();
  }

  private boolean isLoopbackUrl(String url) {
    if (url == null || url.isBlank()) return false;
    try {
      URI uri = URI.create(url);
      return isLoopbackHost(uri.getHost());
    } catch (Exception e) {
      return false;
    }
  }

  private boolean isLoopbackHost(String host) {
    if (host == null || host.isBlank()) return false;
    String normalized = host.trim().toLowerCase();
    return "127.0.0.1".equals(normalized) || "localhost".equals(normalized);
  }

  private boolean isRemoteCdp(String cdpUrl) {
    return !cdpUrl.isBlank() && !isLoopbackUrl(cdpUrl);
  }

  private int resolveRemoteCdpTimeoutMs(boolean remote) {
    if (!remote) return 5000;
    int configured = browserConfig == null ? 15000 : browserConfig.getRemoteCdpTimeoutMs();
    return configured > 0 ? configured : 15000;
  }

  private double resolveNavigationTimeoutMs() {
    int configured = browserConfig == null ? 30000 : browserConfig.getNavigationTimeoutMs();
    return configured > 0 ? configured : 30000;
  }

  private Page.NavigateOptions buildNavigateOptions() {
    return new Page.NavigateOptions()
        .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
        .setTimeout(resolveNavigationTimeoutMs());
  }

  private void audit(String action, String profile, String targetId, boolean ok, String detail) {
    log.info("browser_audit action={} profile={} targetId={} ok={} detail={}", action, profile, targetId, ok, detail);
  }


  private String normalizeRef(String ref) {

    String normalized = ref == null ? "" : ref.trim();
    if (normalized.startsWith("ref=")) {
      normalized = normalized.substring(4);
    } else if (normalized.startsWith("@")) {
      normalized = normalized.substring(1);
    }
    return normalized.trim();
  }

  private String normalizeSnapshotFormat(String format) {
    String normalized = safeText(format).toLowerCase();
    if (normalized.equals("aria") || normalized.equals("role") || normalized.equals("ai")) return normalized;
    return "ai";
  }

  private String snapshotScriptFor(String format) {
    return switch (format) {
      case "aria" -> SNAPSHOT_ARIA_SCRIPT;
      case "role" -> SNAPSHOT_ROLE_SCRIPT;
      default -> SNAPSHOT_AI_SCRIPT;
    };
  }

  private String formatSnapshotText(String format, List<Map<String, Object>> items) {
    StringBuilder snapshot = new StringBuilder();
    for (Map<String, Object> item : items) {
      String ref = item.get("ref") == null ? "" : String.valueOf(item.get("ref"));
      String tag = item.get("tag") == null ? "" : String.valueOf(item.get("tag"));
      String text = item.get("text") == null ? "" : String.valueOf(item.get("text"));
      String role = item.get("role") == null ? "" : String.valueOf(item.get("role"));
      String name = item.get("name") == null ? "" : String.valueOf(item.get("name"));
      if ("role".equals(format)) {
        snapshot.append("[ref=").append(ref).append("] ")
            .append("role=").append(role).append(" name=").append(name).append("\n");
      } else if ("aria".equals(format)) {
        snapshot.append("[ref=").append(ref).append("] ")
            .append("role=").append(role).append(" name=").append(name)
            .append(tag.isBlank() ? "" : " tag=" + tag)
            .append(text.isBlank() ? "" : " text=" + text)
            .append("\n");
      } else {
        snapshot.append("[").append(ref).append("] ")
            .append(tag).append(" ")
            .append(text)
            .append("\n");
      }
    }
    return snapshot.toString().trim();
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> toMapList(Object raw) {
    if (raw instanceof List<?> list) {
      return (List<Map<String, Object>>) list;
    }
    return List.of();
  }

  private class ProfileRuntime {


    private final String name;
    private final BrowserContext context;
    private final Browser browser;
    private final boolean cdpMode;
    private final Map<String, Page> tabs = new ConcurrentHashMap<>();
    private final AtomicInteger lastBlockStatus = new AtomicInteger(0);
    private String lastTabId;

    private ProfileRuntime(String name, BrowserContext context, Browser browser, boolean cdpMode) {
      this.name = name;
      this.context = context;
      this.browser = browser;
      this.cdpMode = cdpMode;
      observeExistingPages();
    }

    private void observeExistingPages() {
      for (Page page : context.pages()) {
        registerPage(page);
      }
      context.onPage(this::registerPage);
    }

    private void registerPage(Page page) {
      String id = UUID.randomUUID().toString();
      tabs.put(id, page);
      lastTabId = id;
      page.onClose(p -> tabs.values().remove(p));
      if (isAdvanced() && antiBot.isEnableDetection()) {
        page.onResponse(response -> {
          int status = response.status();
          if (status == 403 || status == 429) {
            lastBlockStatus.set(status);
          }
        });
      }
      if (isEnhanced() && antiBot.isEnableStealth()) {
        context.addInitScript(STEALTH_SCRIPT);
      }
      if (isEnhanced() && antiBot.isEnableResourceBlock()) {
        installResourceBlocking(page);
      }
    }

    boolean isRunning() {
      return context != null && !context.pages().isEmpty();
    }

    void close() {
      try {
        if (context != null) context.close();
      } catch (Exception ignored) {
      }
      try {
        if (browser != null && !cdpMode) browser.close();
      } catch (Exception ignored) {
      }
      tabs.clear();
    }

    List<BrowserTabInfo> listTabs() {
      List<BrowserTabInfo> result = new ArrayList<>();
      for (Map.Entry<String, Page> entry : tabs.entrySet()) {
        Page page = entry.getValue();
        if (page.isClosed()) continue;
        result.add(new BrowserTabInfo(entry.getKey(), safeText(page.title()), safeText(page.url())));
      }
      return result;
    }

    BrowserTabInfo openTab(String url) {
      Page page = context.newPage();
      String targetUrl = safeText(url);
      if (!targetUrl.isBlank()) {
        page.navigate(targetUrl, buildNavigateOptions());
      }

      String id = registerAndGetId(page);
      return new BrowserTabInfo(id, safeText(page.title()), safeText(page.url()));
    }

    void focusTab(String targetId) {
      Page page = resolvePage(targetId);
      if (page == null) {
        throw new RuntimeException("tab not found");
      }
      page.bringToFront();
      lastTabId = targetId;
    }

    void closeTab(String targetId) {
      Page page = resolvePage(targetId);
      if (page == null) {
        throw new RuntimeException("tab not found");
      }
      page.close();
      tabs.remove(targetId);
    }

    BrowserSnapshot snapshot(String targetId, String format) {
      Page page = ensurePage(targetId);
      resetAntiBotSignals();
      String normalizedFormat = normalizeSnapshotFormat(format);
      List<Map<String, Object>> items = toMapList(page.evaluate(snapshotScriptFor(normalizedFormat)));

      String snapshot = formatSnapshotText(normalizedFormat, items);
      return new BrowserSnapshot(name, resolveTabId(page), normalizedFormat, snapshot, items);
    }


    BrowserActionResult act(String targetId, String ref, String kind, String text, String key) {
      Page page = ensurePage(targetId);
      String normalizedKind = safeText(kind).toLowerCase();
      Locator locator = resolveRefLocator(page, ref);
      if (locator == null) {
        return BrowserActionResult.error("ref not found: " + ref);
      }
      switch (normalizedKind) {
        case "click" -> locator.click();
        case "type" -> locator.fill(text == null ? "" : text);
        case "hover" -> locator.hover();
        case "press" -> locator.press(key == null ? "Enter" : key);
        default -> {
          return BrowserActionResult.error("Unsupported act kind: " + kind);
        }
      }
      return BrowserActionResult.ok("Action completed: " + normalizedKind);
    }

    BrowserActionResult navigate(String targetId, String url) {
      Page page = ensurePage(targetId);
      String targetUrl = safeText(url);
      if (targetUrl.isBlank()) return BrowserActionResult.error("url required");
      resetAntiBotSignals();
      page.navigate(targetUrl, buildNavigateOptions());

      return BrowserActionResult.ok("Navigated to " + targetUrl + "\nTitle: " + safeText(page.title()));
    }

    BrowserActionResult click(String targetId, String selector) {
      Page page = ensurePage(targetId);
      if (selector == null || selector.isBlank()) return BrowserActionResult.error("selector required");
      page.click(selector);
      return BrowserActionResult.ok("Clicked: " + selector);
    }

    BrowserActionResult type(String targetId, String selector, String text) {
      Page page = ensurePage(targetId);
      if (selector == null || selector.isBlank()) return BrowserActionResult.error("selector required");
      page.fill(selector, text == null ? "" : text);
      return BrowserActionResult.ok("Typed in: " + selector);
    }

    BrowserActionResult upload(String targetId, String selector, String filePath, String ref) {
      Page page = ensurePage(targetId);
      if (filePath == null || filePath.isBlank()) return BrowserActionResult.error("filePath required");
      if (ref != null && !ref.isBlank()) {
        Locator locator = resolveRefLocator(page, ref);
        if (locator == null) return BrowserActionResult.error("ref not found: " + ref);
        locator.setInputFiles(Path.of(filePath));
      } else if (selector != null && !selector.isBlank()) {
        page.setInputFiles(selector, Path.of(filePath));
      } else {
        return BrowserActionResult.error("selector or ref required");
      }
      return BrowserActionResult.ok("Uploaded file: " + filePath);
    }

    BrowserActionResult content(String targetId) {
      Page page = ensurePage(targetId);
      String body = page.innerText("body");
      return BrowserActionResult.ok("Page Title: " + safeText(page.title()) + "\n\nContent:\n" + body);
    }

    BrowserActionResult screenshot(String targetId) {
      Page page = ensurePage(targetId);
      String filename = "screenshot-" + UUID.randomUUID() + ".png";
      Path tmpDir = workspaceDir.resolve("tmp");
      try {
        Files.createDirectories(tmpDir);
      } catch (Exception e) {
        return BrowserActionResult.error("Failed to create tmp directory: " + e.getMessage());
      }
      Path path = tmpDir.resolve(filename);
      page.screenshot(new Page.ScreenshotOptions().setPath(path));
      return BrowserActionResult.ok("Screenshot saved to workspace/tmp: " + filename);
    }


    private void resetAntiBotSignals() {
      lastBlockStatus.set(0);
    }

    void applyAntiBot() {
      if (context == null) return;
      if (isEnhanced() && antiBot.isEnableStealth()) {
        context.addInitScript(STEALTH_SCRIPT);
      }
    }

    private Page ensurePage(String targetId) {
      Page resolved = resolvePage(targetId);
      if (resolved != null) return resolved;
      Page page = context.newPage();
      String id = registerAndGetId(page);
      lastTabId = id;
      return page;
    }

    private Page resolvePage(String targetId) {
      if (targetId != null && !targetId.isBlank()) {
        Page page = tabs.get(targetId);
        if (page != null && !page.isClosed()) return page;
      }
      if (lastTabId != null) {
        Page last = tabs.get(lastTabId);
        if (last != null && !last.isClosed()) return last;
      }
      for (Page page : tabs.values()) {
        if (!page.isClosed()) return page;
      }
      return null;
    }

    private String registerAndGetId(Page page) {
      String id = UUID.randomUUID().toString();
      tabs.put(id, page);
      lastTabId = id;
      return id;
    }

    private String resolveTabId(Page page) {
      for (Map.Entry<String, Page> entry : tabs.entrySet()) {
        if (entry.getValue() == page) return entry.getKey();
      }
      return lastTabId;
    }

    private Locator resolveRefLocator(Page page, String ref) {
      if (ref == null || ref.isBlank()) return null;
      String normalized = normalizeRef(ref);
      if (normalized.matches("^e\\d+$")) {
        return page.locator("[data-agentbot-role-ref='" + normalized + "']");
      }
      if (normalized.matches("^a\\d+$")) {
        return page.locator("[aria-ref='" + normalized + "']");
      }
      if (normalized.matches("^\\d+$")) {
        return page.locator("[data-agentbot-ref='" + normalized + "']");
      }
      return page.locator("[data-agentbot-ref='" + normalized + "'],[aria-ref='" + normalized + "'],[data-agentbot-role-ref='" + normalized + "']");
    }


    private void installResourceBlocking(Page page) {
      Set<String> blockTypes = new HashSet<>();
      List<String> configured = antiBot.getBlockResourceTypes();
      if (configured == null || configured.isEmpty()) {
        blockTypes.addAll(DEFAULT_BLOCK_RESOURCE_TYPES);
      } else {
        configured.forEach(type -> blockTypes.add(type.toLowerCase()));
      }
      List<String> urlPatterns = antiBot.getBlockUrlPatterns();
      List<String> patterns = (urlPatterns == null || urlPatterns.isEmpty())
          ? DEFAULT_BLOCK_URL_PATTERNS
          : urlPatterns;

      page.route("**/*", route -> {
        String resourceType = route.request().resourceType();
        String url = route.request().url();
        boolean blockByType = resourceType != null && blockTypes.contains(resourceType.toLowerCase());
        boolean blockByPattern = matchesAnyPattern(url, patterns);
        if (blockByType || blockByPattern) {
          route.abort();
        } else {
          route.resume();
        }
      });
    }

    private boolean matchesAnyPattern(String url, List<String> patterns) {
      if (url == null || patterns == null || patterns.isEmpty()) return false;
      for (String pattern : patterns) {
        if (matchWildcard(url, pattern)) return true;
      }
      return false;
    }

    private boolean matchWildcard(String text, String pattern) {
      if (pattern == null || pattern.isBlank()) return false;
      StringBuilder regex = new StringBuilder();
      for (int i = 0; i < pattern.length(); i++) {
        char c = pattern.charAt(i);
        if (c == '*') {
          regex.append(".*");
        } else if (c == '?') {
          regex.append('.');
        } else if (".\\+*?[^]$(){}=!<>|:-".indexOf(c) >= 0) {
          regex.append('\\').append(c);
        } else {
          regex.append(c);
        }
      }
      return text.matches(regex.toString());
    }
  }

  public record BrowserStatus(boolean enabled, int port, String defaultProfile, List<BrowserProfileStatus> profiles) {}

  public record BrowserProfileStatus(String name, boolean running, int tabCount, boolean isDefault) {}

  public record BrowserTabInfo(String targetId, String title, String url) {}

  public record BrowserSnapshot(String profile, String targetId, String format, String snapshot, List<Map<String, Object>> items) {}


  public record BrowserActionResult(boolean ok, String message) {
    public static BrowserActionResult ok(String message) {
      return new BrowserActionResult(true, message);
    }

    public static BrowserActionResult error(String message) {
      return new BrowserActionResult(false, message);
    }
  }

  private static final String SNAPSHOT_AI_SCRIPT = """
      () => {
        const candidates = Array.from(document.querySelectorAll('a,button,input,textarea,select,[role="button"],[role="link"],[role="menuitem"],[onclick]'));
        let i = 1;
        const items = [];
        for (const el of candidates) {
          if (!el.isConnected) continue;
          const ref = String(i++);
          el.setAttribute('data-agentbot-ref', ref);
          const text = (el.innerText || el.getAttribute('aria-label') || el.getAttribute('name') || '').trim();
          items.push({
            ref,
            tag: el.tagName.toLowerCase(),
            text
          });
        }
        return items;
      }
      """;

  private static final String SNAPSHOT_ARIA_SCRIPT = """
      () => {
        const candidates = Array.from(document.querySelectorAll('[aria-label],[aria-labelledby],[role],a,button,input,textarea,select'));
        const items = [];
        let i = 1;
        const inferRole = (el) => {
          const tag = el.tagName.toLowerCase();
          if (tag === 'button') return 'button';
          if (tag === 'a') return 'link';
          if (tag === 'textarea') return 'textbox';
          if (tag === 'select') return 'combobox';
          if (tag === 'input') {
            const type = (el.getAttribute('type') || 'text').toLowerCase();
            if (['checkbox'].includes(type)) return 'checkbox';
            if (['radio'].includes(type)) return 'radio';
            if (['submit','button','reset'].includes(type)) return 'button';
            return 'textbox';
          }
          return 'generic';
        };
        for (const el of candidates) {
          if (!el.isConnected) continue;
          const ref = `a${i++}`;
          el.setAttribute('aria-ref', ref);
          const role = (el.getAttribute('role') || inferRole(el) || '').trim();
          const name = (el.getAttribute('aria-label') || el.getAttribute('name') || el.innerText || '').trim();
          items.push({
            ref,
            tag: el.tagName.toLowerCase(),
            role,
            name,
            text: name
          });
        }
        return items;
      }
      """;

  private static final String SNAPSHOT_ROLE_SCRIPT = """
      () => {
        const candidates = Array.from(document.querySelectorAll('a,button,input,textarea,select,[role]'));
        const items = [];
        let i = 1;
        const inferRole = (el) => {
          const tag = el.tagName.toLowerCase();
          if (tag === 'button') return 'button';
          if (tag === 'a') return 'link';
          if (tag === 'textarea') return 'textbox';
          if (tag === 'select') return 'combobox';
          if (tag === 'input') {
            const type = (el.getAttribute('type') || 'text').toLowerCase();
            if (['checkbox'].includes(type)) return 'checkbox';
            if (['radio'].includes(type)) return 'radio';
            if (['submit','button','reset'].includes(type)) return 'button';
            return 'textbox';
          }
          return 'generic';
        };
        for (const el of candidates) {
          if (!el.isConnected) continue;
          const ref = `e${i++}`;
          el.setAttribute('data-agentbot-role-ref', ref);
          const role = (el.getAttribute('role') || inferRole(el) || '').trim();
          const name = (el.getAttribute('aria-label') || el.getAttribute('name') || el.innerText || '').trim();
          items.push({
            ref,
            tag: el.tagName.toLowerCase(),
            role,
            name,
            text: name
          });
        }
        return items;
      }
      """;


  private static final String STEALTH_SCRIPT = """
        Object.defineProperty(navigator, 'webdriver', { get: () => undefined });
        window.chrome = window.chrome || { runtime: {} };
        Object.defineProperty(navigator, 'languages', { get: () => ['zh-CN', 'zh', 'en-US', 'en'] });
        Object.defineProperty(navigator, 'plugins', {
          get: () => [1, 2, 3, 4, 5]
        });
        const originalQuery = window.navigator.permissions.query;
        window.navigator.permissions.query = (parameters) => (
          parameters.name === 'notifications'
            ? Promise.resolve({ state: Notification.permission })
            : originalQuery(parameters)
        );
        const handler = {
          get: function(target, property) {
            if (property === 'length') return 1;
            return target[property];
          }
        };
        try {
          const plugins = navigator.plugins;
          navigator.plugins = new Proxy(plugins, handler);
        } catch (e) {}
    """;
}
