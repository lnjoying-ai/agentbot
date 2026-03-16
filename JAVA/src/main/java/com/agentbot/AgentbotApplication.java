package com.agentbot;

import com.agentbot.config.AgentbotProperties;
import com.agentbot.core.util.ConfigPathResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;

import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;


import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;



@SpringBootApplication
@EnableConfigurationProperties(AgentbotProperties.class)
public class AgentbotApplication {
  public static final long START_TIME = System.currentTimeMillis();
  private static final Logger log = LoggerFactory.getLogger(AgentbotApplication.class);


  public static void main(String[] args) {

    ensureUserDataPaths();
    seedUserDataIfMissing();

    configureConsoleEncoding();
    configureLoggingFile();

    log.info("Agentbot starting: userDataDir={}, appDir={}, configPath={}",
        ConfigPathResolver.resolveUserDataDir(),
        ConfigPathResolver.resolveAppDir(),
        System.getProperty("AGENTBOT_CONFIG"));

    ensureWindowsTrayEnabled();
    ConfigurableApplicationContext context = SpringApplication.run(AgentbotApplication.class, args);
    logServerAddress(context);
    initializeWindowsTray(context);

  }


  private static void configureConsoleEncoding() {
    try {
      System.setProperty("file.encoding", "UTF-8");
      System.setProperty("sun.stdout.encoding", "UTF-8");
      System.setProperty("sun.stderr.encoding", "UTF-8");
      System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
      System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));

    } catch (Exception e) {
      log.warn("Failed to configure console encoding", e);
    }
  }

  private static void configureLoggingFile() {
    if (System.getProperty("logging.file.name") != null || System.getProperty("logging.file.path") != null) {
      log.info("Logging file already configured: name={}, path={}",
          System.getProperty("logging.file.name"),
          System.getProperty("logging.file.path"));
      return;
    }
    try {
      Path userDataDir = ConfigPathResolver.resolveUserDataDir();
      Path logDir = resolveWritableDir(userDataDir.resolve("log"));
      Files.createDirectories(logDir);
      Path logFile = logDir.resolve("agentbot.log");

      System.setProperty("logging.file.name", logFile.toAbsolutePath().normalize().toString());
      log.info("Logging file initialized: {}", logFile.toAbsolutePath().normalize());
    } catch (Exception e) {
      log.warn("Failed to initialize log directory", e);
    }
  }



  private static void ensureUserDataPaths() {
      Path userBase = ConfigPathResolver.resolveUserDataDir();
      Path configPath = userBase.resolve("config").resolve("agentbot.yml");
      System.setProperty("AGENTBOT_CONFIG", configPath.toAbsolutePath().normalize().toString());
      log.info("User data paths resolved: userBase={}, configPath={}", userBase, configPath.toAbsolutePath().normalize());
  }

  private static void seedUserDataIfMissing() {
    try {
      Path appDir = ConfigPathResolver.resolveAppDir();
      if (appDir == null) {
        log.warn("App directory not found; skip seeding user data");
        return;
      }

      Path userBase = ConfigPathResolver.resolveUserDataDir();
      Path configDir = userBase.resolve("config");
      if (!Files.exists(configDir)) {
        Path sourceConfigDir = appDir.resolve("config");
        if (Files.exists(sourceConfigDir)) {
          copyDirectory(sourceConfigDir, configDir);
          log.info("Config directory copied: {} -> {}", sourceConfigDir, configDir);
        } else {
          Files.createDirectories(configDir);
          log.info("Config directory created: {}", configDir);
        }
      }

      Path workspaceDir = userBase.resolve("workspace");
      if (!Files.exists(workspaceDir)) {
        Path sourceWorkspace = appDir.resolve("workspace");
        if (Files.exists(sourceWorkspace)) {
          copyDirectory(sourceWorkspace, workspaceDir);
          log.info("Workspace directory copied: {} -> {}", sourceWorkspace, workspaceDir);
        } else {
          Files.createDirectories(workspaceDir);
          log.info("Workspace directory created: {}", workspaceDir);
        }
      }

      Path logDir = userBase.resolve("log");
      boolean existed = Files.exists(logDir);
      Files.createDirectories(logDir);
      if (!existed) {
        log.info("Log directory created: {}", logDir);
      }
    } catch (Exception e) {
      log.warn("Failed to initialize user data", e);
    }
  }


  private static Path resolveWritableDir(Path preferred) {
    if (isWritable(preferred)) {
      return preferred;
    }
    return ConfigPathResolver.resolveUserDataDir().resolve(preferred.getFileName());
  }


  private static boolean isWritable(Path dir) {
    try {
      Files.createDirectories(dir);
      Path probe = dir.resolve(".writable");
      Files.writeString(probe, "1");
      Files.deleteIfExists(probe);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private static void copyDirectory(Path source, Path target) throws Exception {
    Files.walk(source).forEach(path -> {
      try {
        Path relative = source.relativize(path);
        Path dest = target.resolve(relative);
        if (Files.isDirectory(path)) {
          Files.createDirectories(dest);
        } else if (!Files.exists(dest)) {
          Files.createDirectories(dest.getParent());
          Files.copy(path, dest);
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  private static void ensureWindowsTrayEnabled() {

    String os = System.getProperty("os.name", "").toLowerCase();
    if (!os.contains("win")) {
      log.info("Tray init skipped: non-windows os={}", os);
      return;
    }
    if (System.getProperty("java.awt.headless") == null) {
      System.setProperty("java.awt.headless", "false");
      log.info("Tray init: force java.awt.headless=false");
    }
  }


  private static void logServerAddress(ConfigurableApplicationContext context) {
    if (context == null) return;
    try {
      String address = context.getEnvironment().getProperty("server.address");
      if (address == null || address.isBlank()) {
        address = context.getEnvironment().getProperty("server.ip", "127.0.0.1");
      }
      String port = context.getEnvironment().getProperty("server.port", "8080");
      log.info("Agentbot listening on {}:{}", address, port);
    } catch (Exception e) {
      log.warn("Failed to resolve server address", e);
    }
  }

  private static void initializeWindowsTray(ConfigurableApplicationContext context) {

    try {
      String os = System.getProperty("os.name", "").toLowerCase();
      if (!os.contains("win")) {
        return;
      }
      if (GraphicsEnvironment.isHeadless()) {
        log.warn("Tray disabled: headless environment");
        return;
      }
      if (!SystemTray.isSupported()) {
        log.warn("Tray not supported by system");
        return;
      }

      EventQueue.invokeLater(() -> {
        try {
          SystemTray tray = SystemTray.getSystemTray();
          int trayW = tray.getTrayIconSize().width;
          int trayH = tray.getTrayIconSize().height;
          Image image = loadTrayImage(trayW * 2, trayH * 2);
          TrayIcon trayIcon = new TrayIcon(image, "agentbot");

          trayIcon.setImageAutoSize(true);


          PopupMenu menu = new PopupMenu();
          MenuItem aboutItem = new MenuItem("About");
          aboutItem.addActionListener(e -> {
            String message = "秒如科技 · agentbot - \"My name... is Neo.\"";
            try {
              JOptionPane.showMessageDialog(null, message, "About", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
              trayIcon.displayMessage("About", message, TrayIcon.MessageType.INFO);
            }
          });
          MenuItem exitItem = new MenuItem("Exit");
          exitItem.addActionListener(e -> {
            try {
              tray.remove(trayIcon);
            } catch (Exception ignored) {
            }
            SpringApplication.exit(context, () -> 0);
            System.exit(0);
          });

          menu.add(aboutItem);
          menu.addSeparator();
          menu.add(exitItem);
          trayIcon.setPopupMenu(menu);
          tray.add(trayIcon);
        } catch (Exception e) {
          log.warn("Failed to initialize tray", e);
        }

      });
    } catch (Exception e) {
      log.warn("Failed to initialize tray", e);
    }
  }


  private static Image loadTrayImage(int width, int height) {
    int targetW = Math.max(width, 64);
    int targetH = Math.max(height, 64);
    Image fallback = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
    try {
      Path appDir = ConfigPathResolver.resolveAppDir();
      if (appDir != null) {
        Path icoPath = appDir.resolve("dragon-logo.ico");
        if (Files.exists(icoPath)) {
          Image icoImage = new ImageIcon(icoPath.toString()).getImage();
          if (icoImage != null) {
            return icoImage.getScaledInstance(targetW, targetH, Image.SCALE_SMOOTH);
          }
        }
        Path pngPath = appDir.resolve("dragon-logo.png");
        if (Files.exists(pngPath)) {
          BufferedImage source = ImageIO.read(pngPath.toFile());
          if (source != null) {
            BufferedImage scaled = scaleImage(source, targetW, targetH);
            return roundCorner(scaled);
          }
        }
      }
    } catch (Exception e) {
      log.warn("Failed to load tray icon", e);
    }

    return fallback;
  }


  private static BufferedImage scaleImage(BufferedImage source, int width, int height) {
    int targetW = Math.max(width, 64);
    int targetH = Math.max(height, 64);
    Image scaled = source.getScaledInstance(targetW, targetH, Image.SCALE_SMOOTH);
    BufferedImage out = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = out.createGraphics();
    g.drawImage(scaled, 0, 0, null);
    g.dispose();
    return out;
  }

  private static BufferedImage roundCorner(BufferedImage source) {
    int w = source.getWidth();
    int h = source.getHeight();
    int radius = Math.max(2, Math.min(w, h) / 4);
    BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = out.createGraphics();
    g.setClip(new java.awt.geom.RoundRectangle2D.Float(0, 0, w, h, radius, radius));
    g.drawImage(source, 0, 0, null);
    g.dispose();
    return out;
  }
}
