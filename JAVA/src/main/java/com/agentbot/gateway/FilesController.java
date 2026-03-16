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
import org.springframework.web.bind.annotation.PathVariable;
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
  public List<Map<String, Object>> listFiles() {
    Path tmpDir = resolveTmpDir();
    if (!Files.exists(tmpDir)) {
      return List.of();
    }

    List<Map<String, Object>> items = new ArrayList<>();
    try (var stream = Files.list(tmpDir)) {
      stream.filter(Files::isRegularFile)
          .sorted(Comparator.comparingLong(this::lastModified).reversed())
          .forEach(path -> items.add(describeFile(path)));
    } catch (Exception e) {
      log.warn("Failed to list tmp files", e);
    }
    return items;
  }

  @GetMapping("/{name}")
  public ResponseEntity<Resource> download(@PathVariable("name") String name) {
    Path target = resolveTmpFile(name);
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

  @DeleteMapping("/{name}")
  public Map<String, Object> delete(@PathVariable("name") String name) {
    Path target = resolveTmpFile(name);
    if (target == null || !Files.exists(target) || !Files.isRegularFile(target)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "file not found");
    }

    try {
      Files.delete(target);
      return Map.of("ok", true, "name", target.getFileName().toString());
    } catch (Exception e) {
      log.error("Failed to delete file", e);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "delete failed");
    }
  }

  private long lastModified(Path path) {
    try {
      return Files.getLastModifiedTime(path).toMillis();
    } catch (Exception ignored) {
      return 0L;
    }
  }

  private Map<String, Object> describeFile(Path path) {
    Map<String, Object> item = new LinkedHashMap<>();
    String name = path.getFileName().toString();
    item.put("name", name);
    try {
      item.put("size", Files.size(path));
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

  private Path resolveTmpDir() {
    return ConfigPathResolver.resolveUserDataDir().resolve("workspace").resolve("tmp").toAbsolutePath().normalize();
  }

  private Path resolveTmpFile(String name) {
    if (name == null || name.isBlank()) return null;
    if (name.contains("/") || name.contains("\\") || name.contains("..")) return null;
    Path tmpDir = resolveTmpDir();
    Path target = tmpDir.resolve(name).normalize();
    if (!target.startsWith(tmpDir)) return null;
    return target;
  }
}
