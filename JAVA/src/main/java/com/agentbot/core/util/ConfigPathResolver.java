package com.agentbot.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;



public final class ConfigPathResolver {
  private static final Logger log = LoggerFactory.getLogger(ConfigPathResolver.class);

  private ConfigPathResolver() {}


  public static Path resolveUserDataDir() {
    String root = System.getenv("AGENTBOT_USERDATA");
    if (root != null && !root.isBlank()) {
      Path resolved = Path.of(expandEnvVars(root));
      log.info("User data dir resolved from AGENTBOT_USERDATA: {}", resolved);
      return resolved;
    }
    String userHome = System.getProperty("user.home");
    if (userHome != null && !userHome.isBlank()) {
      Path resolved = Path.of(userHome, ".agentbot");
      log.info("User data dir resolved from user.home: {}", resolved);
      return resolved;
    }
    throw new IllegalStateException("Unable to determine user data directory");
  }


  public static Path resolveConfigPath() {
    Path userBase = resolveUserDataDir();

    Path userConfig = userBase.resolve("config").resolve("agentbot.yml");
    if (Files.exists(userConfig)) {
      log.info("Config path resolved: {}", userConfig.toAbsolutePath().normalize());
      return userConfig;
    }

    log.warn("Config path missing: {}", userConfig.toAbsolutePath().normalize());
    return null;
  }


  public static Path resolveConfigDir() {
    Path userBase = resolveUserDataDir();
    Path userConfig = userBase.resolve("config");
    log.info("Config dir resolved: {}", userConfig.toAbsolutePath().normalize());
    return userConfig;
  }


  public static Path resolveAppDir() {
    try {
      String os = System.getProperty("os.name", "").toLowerCase();
      Path baseDir = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
      Path appDir = baseDir;
      if (os.contains("win")) {
        Path candidate = baseDir.resolve("app");     //msi安装后下面有app目录
        if (Files.exists(candidate)) {
          appDir = candidate;
        }
      }
      log.info("App dir resolved from user.dir: baseDir={}, appDir={}, os={}", baseDir, appDir, os);
      return appDir;
    } catch (Exception e) {
      log.warn("Failed to resolve app dir", e);
      return null;
    }
  }





  private static void ensureUserDataDirs(Path userBase) {
    if (userBase == null) {
      return;
    }
    try {
      Files.createDirectories(userBase.resolve("config"));
      Files.createDirectories(userBase.resolve("workspace"));
      Files.createDirectories(userBase.resolve("log"));
    } catch (Exception ignored) {
    }
  }

  private static String expandEnvVars(String input) {

    if (input == null || input.isBlank()) {
      return input;
    }
    StringBuilder out = new StringBuilder();
    int idx = 0;
    while (idx < input.length()) {
      int start = input.indexOf('%', idx);
      if (start < 0 || start == input.length() - 1) {
        out.append(input.substring(idx));
        break;
      }
      int end = input.indexOf('%', start + 1);
      if (end < 0) {
        out.append(input.substring(idx));
        break;
      }
      out.append(input, idx, start);
      String key = input.substring(start + 1, end);
      String value = System.getenv(key);
      if (value != null) {
        out.append(value);
      } else {
        out.append('%').append(key).append('%');
      }
      idx = end + 1;
    }
    return out.toString();
  }
}

