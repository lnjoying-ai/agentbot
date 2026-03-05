package com.agentbot.core.security;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent 访问控制管理器.
 * 
 * 功能:
 * - 用户权限管理
 * - Agent 访问控制列表 (ACL)
 * - 操作权限验证
 * - 角色基础访问控制 (RBAC)
 */
public class AgentAccessControl {
  
  /**
   * 权限类型.
   */
  public enum Permission {
    READ,      // 读取 Agent 信息
    WRITE,     // 修改 Agent 配置
    EXECUTE,   // 执行 Agent 操作 (发送消息)
    DELETE,    // 删除 Agent
    ADMIN      // 管理员权限 (所有操作)
  }
  
  /**
   * 用户角色.
   */
  public enum Role {
    ADMIN,     // 管理员 - 所有权限
    USER,      // 普通用户 - 基本使用权限
    VIEWER,    // 查看者 - 只读权限
    DEVELOPER, // 开发者 - 配置权限
    GUEST      // 访客 - 受限权限
  }
  
  // Agent ACL: agentId -> userId -> permissions
  private final Map<String, Map<String, Set<Permission>>> agentAcls = new ConcurrentHashMap<>();
  
  // User roles: userId -> role
  private final Map<String, Role> userRoles = new ConcurrentHashMap<>();
  
  // Role permissions mapping
  private static final Map<Role, Set<Permission>> ROLE_PERMISSIONS = Map.of(
      Role.ADMIN, Set.of(Permission.READ, Permission.WRITE, Permission.EXECUTE, Permission.DELETE, Permission.ADMIN),
      Role.DEVELOPER, Set.of(Permission.READ, Permission.WRITE, Permission.EXECUTE),
      Role.USER, Set.of(Permission.READ, Permission.EXECUTE),
      Role.VIEWER, Set.of(Permission.READ),
      Role.GUEST, Set.of()
  );
  
  /**
   * 检查用户是否有权限访问 Agent.
   */
  public boolean hasPermission(String userId, String agentId, Permission permission) {
    // Admin role has all permissions
    Role userRole = userRoles.getOrDefault(userId, Role.GUEST);
    if (userRole == Role.ADMIN) {
      return true;
    }
    
    // Check role-based permissions
    Set<Permission> rolePermissions = ROLE_PERMISSIONS.get(userRole);
    if (rolePermissions != null && rolePermissions.contains(permission)) {
      return true;
    }
    
    // Check agent-specific ACL
    Map<String, Set<Permission>> agentAcl = agentAcls.get(agentId);
    if (agentAcl != null) {
      Set<Permission> userPermissions = agentAcl.get(userId);
      if (userPermissions != null && userPermissions.contains(permission)) {
        return true;
      }
    }
    
    return false;
  }
  
  /**
   * 授予用户权限.
   */
  public void grantPermission(String userId, String agentId, Permission permission) {
    agentAcls
        .computeIfAbsent(agentId, k -> new ConcurrentHashMap<>())
        .computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
        .add(permission);
  }
  
  /**
   * 撤销用户权限.
   */
  public void revokePermission(String userId, String agentId, Permission permission) {
    Map<String, Set<Permission>> agentAcl = agentAcls.get(agentId);
    if (agentAcl != null) {
      Set<Permission> userPermissions = agentAcl.get(userId);
      if (userPermissions != null) {
        userPermissions.remove(permission);
      }
    }
  }
  
  /**
   * 设置用户角色.
   */
  public void setUserRole(String userId, Role role) {
    userRoles.put(userId, role);
  }
  
  /**
   * 获取用户角色.
   */
  public Role getUserRole(String userId) {
    return userRoles.getOrDefault(userId, Role.GUEST);
  }
  
  /**
   * 检查用户是否可以访问 Agent.
   */
  public boolean canAccessAgent(String userId, String agentId) {
    return hasPermission(userId, agentId, Permission.READ);
  }
  
  /**
   * 检查用户是否可以执行 Agent 操作.
   */
  public boolean canExecuteAgent(String userId, String agentId) {
    return hasPermission(userId, agentId, Permission.EXECUTE);
  }
  
  /**
   * 检查用户是否可以修改 Agent.
   */
  public boolean canModifyAgent(String userId, String agentId) {
    return hasPermission(userId, agentId, Permission.WRITE);
  }
  
  /**
   * 检查用户是否可以删除 Agent.
   */
  public boolean canDeleteAgent(String userId, String agentId) {
    return hasPermission(userId, agentId, Permission.DELETE);
  }
  
  /**
   * 获取用户可访问的 Agent 列表.
   */
  public List<String> getAccessibleAgents(String userId, List<String> allAgentIds) {
    return allAgentIds.stream()
        .filter(agentId -> canAccessAgent(userId, agentId))
        .toList();
  }
  
  /**
   * 清除 Agent 的所有 ACL.
   */
  public void clearAgentAcl(String agentId) {
    agentAcls.remove(agentId);
  }
  
  /**
   * 获取 Agent 的用户权限.
   */
  public Map<String, Set<Permission>> getAgentAcl(String agentId) {
    Map<String, Set<Permission>> acl = agentAcls.get(agentId);
    return acl != null ? Map.copyOf(acl) : Map.of();
  }
}
