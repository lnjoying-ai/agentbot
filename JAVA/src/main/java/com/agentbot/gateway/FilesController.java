package com.agentbot.gateway;

import com.agentbot.core.util.ConfigPathResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/workspace/files")
public class FilesController {
  private static final Logger log = LoggerFactory.getLogger(FilesController.class);


  @GetMapping
  public List<Map<String, Object>> listFiles(@org.springframework.web.bind.annotation.RequestParam(value = "path", required = false) String path) {
    Path rootDir = resolveWorkspaceDir();
    Path targetDir = resolveWorkspacePath(path);
    if (targetDir == null || !Files.exists(targetDir) || !Files.isDirectory(targetDir)) {
      return List.of();
    }

    List<Map<String, Object>> items = new ArrayList<>();
    try (var stream = Files.list(targetDir)) {
      stream.sorted(Comparator.comparing((Path p) -> !Files.isDirectory(p))
          .thenComparing(p -> p.getFileName().toString().toLowerCase()))
          .forEach(p -> items.add(describeEntry(rootDir, p)));
    } catch (Exception e) {
      log.warn("Failed to list workspace files", e);
    }
    return items;
  }

  @GetMapping("/download")
  public ResponseEntity<Resource> download(@org.springframework.web.bind.annotation.RequestParam("path") String path) {
    Path target = resolveWorkspacePath(path);
    if (target == null || !Files.exists(target) || !Files.isRegularFile(target)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "file not found");
    }

    try {
      Resource resource = new UrlResource(target.toUri());
      if (!resource.exists()) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "file not found");
      }
      String filename = target.getFileName().toString();
      HttpHeaders headers = new HttpHeaders();
      headers.setContentDisposition(ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build());

      headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
      return new ResponseEntity<>(resource, headers, HttpStatus.OK);
    } catch (Exception e) {
      log.error("Failed to download file", e);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "download failed");
    }
  }

  @DeleteMapping
  public Map<String, Object> delete(@org.springframework.web.bind.annotation.RequestParam("path") String path) {
    Path target = resolveWorkspacePath(path);
    if (target == null || !Files.exists(target)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "file not found");
    }
    if (resolveWorkspaceDir().equals(target)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cannot delete workspace root");
    }

    try {
      deleteRecursively(target);
      return Map.of("ok", true, "path", path);
    } catch (Exception e) {
      log.error("Failed to delete file", e);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "delete failed");
    }
  }

  private Map<String, Object> describeEntry(Path rootDir, Path path) {
    Map<String, Object> item = new LinkedHashMap<>();
    String name = path.getFileName().toString();
    String relative = rootDir.relativize(path).toString().replace("\\", "/");
    boolean isDir = Files.isDirectory(path);
    item.put("name", name);
    item.put("path", relative);
    item.put("type", isDir ? "dir" : "file");
    try {
      item.put("size", isDir ? 0L : Files.size(path));
    } catch (Exception ignored) {
      item.put("size", 0L);
    }
    try {
      item.put("modifiedAt", OffsetDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(), java.time.ZoneOffset.UTC).toString());
    } catch (Exception ignored) {
      item.put("modifiedAt", null);
    }
    return item;
  }

  private Path resolveWorkspaceDir() {
    return ConfigPathResolver.resolveUserDataDir().resolve("workspace").toAbsolutePath().normalize();
  }

  private Path resolveWorkspacePath(String path) {
    Path rootDir = resolveWorkspaceDir();
    if (path == null || path.isBlank() || "/".equals(path)) {
      return rootDir;
    }
    if (path.contains("\\") || path.contains("..")) return null;
    String normalized = path.startsWith("/") ? path.substring(1) : path;
    Path target = rootDir.resolve(normalized).normalize();
    if (!target.startsWith(rootDir)) return null;
    return target;
  }

  private void deleteRecursively(Path path) throws Exception {
    if (!Files.exists(path)) return;
    if (Files.isDirectory(path)) {
      try (var stream = Files.walk(path)) {
        stream.sorted(Comparator.reverseOrder()).forEach(p -> {
          try {
            Files.deleteIfExists(p);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
      }
    } else {
      Files.delete(path);
    }
  }
}

