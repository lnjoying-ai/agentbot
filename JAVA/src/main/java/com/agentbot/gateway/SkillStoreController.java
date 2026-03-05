package com.agentbot.gateway;

import com.agentbot.core.skills.SkillStoreService;
import com.agentbot.core.skills.SkillStoreService.SkillDetail;
import com.agentbot.core.skills.SkillStoreService.SkillIndex;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/store/skills")
public class SkillStoreController {
  private final SkillStoreService storeService;

  public SkillStoreController(SkillStoreService storeService) {
    this.storeService = storeService;
  }

  @GetMapping
  public ResponseEntity<List<StoreSkillItem>> list() {
    storeService.refreshIndex();
    List<StoreSkillItem> result = storeService.listStoreSkills().stream()
        .map(this::toStoreItem)
        .toList();
    return ResponseEntity.ok(result);
  }

  @GetMapping("/{id}")
  public ResponseEntity<StoreSkillDetail> detail(@PathVariable("id") String id) {
    storeService.refreshIndex();
    SkillDetail detail = storeService.loadStoreSkillDetail(id);
    if (detail == null) return ResponseEntity.notFound().build();
    StoreSkillItem item = toStoreItem(detail.getSkill());
    return ResponseEntity.ok(new StoreSkillDetail(item, detail.getContent(), detail.getFiles()));
  }

  @PostMapping("/{id}/import")
  public ResponseEntity<Map<String, Object>> importToWorkspace(@PathVariable("id") String id, @RequestBody(required = false) Map<String, Object> body) {
    boolean ok = storeService.importToWorkspace(id);
    Map<String, Object> resp = new HashMap<>();
    resp.put("ok", ok);
    resp.put("id", id);
    return ok ? ResponseEntity.ok(resp) : ResponseEntity.badRequest().body(resp);
  }

  @PostMapping("/{id}/ignore")
  public ResponseEntity<Map<String, Object>> ignore(@PathVariable("id") String id) {
    boolean ok = storeService.ignoreSkill(id);
    Map<String, Object> resp = new HashMap<>();
    resp.put("ok", ok);
    resp.put("id", id);
    return ok ? ResponseEntity.ok(resp) : ResponseEntity.badRequest().body(resp);
  }

  private StoreSkillItem toStoreItem(SkillIndex item) {
    String status = computeStatus(item);
    return new StoreSkillItem(
        item.getId(),
        item.getName(),
        item.getDescription(),
        item.getVersion(),
        item.getHash(),
        item.getOrigin(),
        item.getScope(),
        item.getUpdatedAt(),
        item.getSize(),
        status,
        item.isChecksumOk()
    );
  }

  private String computeStatus(SkillIndex store) {
    if (store == null) return "invalid";
    if (!store.isChecksumOk()) return "invalid";
    SkillIndex local = storeService.findLocalSkillByName(store.getName()).orElse(null);
    if (local == null) return "available";
    if (store.getHash() != null && store.getHash().equals(local.getHash())) {
      return "installed";
    }
    if (store.getUpdatedAt() > local.getUpdatedAt()) {
      return "update_available";
    }
    return "conflict";
  }

  public record StoreSkillItem(
      String id,
      String name,
      String description,
      String version,
      String hash,
      String origin,
      String scope,
      long updatedAt,
      long size,
      String status,
      boolean checksumOk
  ) {}

  public record StoreSkillDetail(
      StoreSkillItem skill,
      String content,
      List<String> files
  ) {}
}
