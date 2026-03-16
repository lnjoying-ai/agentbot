package com.agentbot.gateway;

import com.agentbot.core.util.ConfigPathResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;


@RestController
@RequestMapping("/api/chat")
public class ChatUploadController {
  private static final Logger log = LoggerFactory.getLogger(ChatUploadController.class);

  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
    if (file == null || file.isEmpty()) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of("ok", false, "error", "file is required"));
    }

    try {
      Path workspaceDir = resolveWorkspaceDir();
      Path tmpDir = workspaceDir.resolve("tmp");
      Files.createDirectories(tmpDir);

      String original = file.getOriginalFilename();
      String safeName = sanitizeFileName(original == null || original.isBlank() ? "upload" : original);
      String storedName = safeName;
      Path target = tmpDir.resolve(storedName).normalize();

      Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);


      Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("ok", true);
      payload.put("originalName", original);
      payload.put("storedName", storedName);
      payload.put("size", file.getSize());
      payload.put("path", "workspace/tmp/" + storedName);
      payload.put("timestamp", OffsetDateTime.now().toString());

      return ResponseEntity.ok(payload);
    } catch (Exception e) {
      log.error("Failed to upload file", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("ok", false, "error", "upload failed"));
    }
  }

  private Path resolveWorkspaceDir() {
    return ConfigPathResolver.resolveUserDataDir().resolve("workspace").toAbsolutePath().normalize();
  }

  private String sanitizeFileName(String name) {
    String cleaned = name.replace("\\", "/");
    int idx = cleaned.lastIndexOf('/');
    if (idx >= 0) {
      cleaned = cleaned.substring(idx + 1);
    }
    cleaned = cleaned.replaceAll("[\\r\\n]", "_");
    cleaned = cleaned.replaceAll("[^a-zA-Z0-9._\\-\\u4e00-\\u9fff]", "_");

    if (cleaned.isBlank()) {
      return "upload";
    }
    return cleaned;
  }

}

