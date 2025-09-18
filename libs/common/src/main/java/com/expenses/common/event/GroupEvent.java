package com.expenses.common.event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventName")
@JsonSubTypes({
    @JsonSubTypes.Type(value = GroupEvent.GroupCreated.class, name = "GROUP_CREATED"),
    @JsonSubTypes.Type(value = GroupEvent.GroupUpdated.class, name = "GROUP_UPDATED"),
    @JsonSubTypes.Type(value = GroupEvent.GroupDeleted.class, name = "GROUP_DELETED"),
    @JsonSubTypes.Type(value = GroupEvent.MemberAdded.class, name = "MEMBER_ADDED"),
    @JsonSubTypes.Type(value = GroupEvent.MemberRemoved.class, name = "MEMBER_REMOVED"),
    @JsonSubTypes.Type(value = GroupEvent.MemberRoleUpdated.class, name = "MEMBER_ROLE_UPDATED"),
    @JsonSubTypes.Type(value = GroupEvent.OwnershipTransferred.class, name = "OWNERSHIP_TRANSFERRED"),
    @JsonSubTypes.Type(value = GroupEvent.GroupSettingsUpdated.class, name = "GROUP_SETTINGS_UPDATED")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
public abstract class GroupEvent extends DomainEvent {

  public static final String TOPIC = "group-events";

  protected GroupEvent(String eventName, UUID groupId) {
    super("GROUP", eventName, groupId, "Group");
  }

  @Override
  public String getTopicName() {
    return TOPIC;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @SuperBuilder
  @NoArgsConstructor
  public static class GroupCreated extends GroupEvent {
    private String name;
    private String description;
    private String type;
    private String defaultCurrency;
    private UUID createdBy;

    public GroupCreated(UUID groupId, String name, String description, String type,
        String defaultCurrency, UUID createdBy) {
      super("GROUP_CREATED", groupId);
      this.name = name;
      this.description = description;
      this.type = type;
      this.defaultCurrency = defaultCurrency;
      this.createdBy = createdBy;
      setCausedBy(createdBy);
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @SuperBuilder
  @NoArgsConstructor
  public static class GroupUpdated extends GroupEvent {
    private String name;
    private String description;
    private String defaultCurrency;
    private String avatarUrl;
    private UUID updatedBy;

    public GroupUpdated(UUID groupId, String name, String description,
        String defaultCurrency, String avatarUrl, UUID updatedBy) {
      super("GROUP_UPDATED", groupId);
      this.name = name;
      this.description = description;
      this.defaultCurrency = defaultCurrency;
      this.avatarUrl = avatarUrl;
      this.updatedBy = updatedBy;
      setCausedBy(updatedBy);
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @SuperBuilder
  @NoArgsConstructor
  public static class GroupDeleted extends GroupEvent {
    private UUID deletedBy;

    public GroupDeleted(UUID groupId, UUID deletedBy) {
      super("GROUP_DELETED", groupId);
      this.deletedBy = deletedBy;
      setCausedBy(deletedBy);
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @SuperBuilder
  @NoArgsConstructor
  public static class MemberAdded extends GroupEvent {
    private UUID userId;
    private String role;
    private UUID addedBy;

    public MemberAdded(UUID groupId, UUID userId, String role, UUID addedBy) {
      super("MEMBER_ADDED", groupId);
      this.userId = userId;
      this.role = role;
      this.addedBy = addedBy;
      setCausedBy(addedBy);
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @SuperBuilder
  @NoArgsConstructor
  public static class MemberRemoved extends GroupEvent {
    private UUID userId;
    private String reason; // LEFT, REMOVED
    private UUID removedBy;

    public MemberRemoved(UUID groupId, UUID userId, String reason, UUID removedBy) {
      super("MEMBER_REMOVED", groupId);
      this.userId = userId;
      this.reason = reason;
      this.removedBy = removedBy;
      setCausedBy(removedBy);
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @SuperBuilder
  @NoArgsConstructor
  public static class MemberRoleUpdated extends GroupEvent {
    private UUID userId;
    private String oldRole;
    private String newRole;
    private UUID updatedBy;

    public MemberRoleUpdated(UUID groupId, UUID userId, String oldRole, String newRole, UUID updatedBy) {
      super("MEMBER_ROLE_UPDATED", groupId);
      this.userId = userId;
      this.oldRole = oldRole;
      this.newRole = newRole;
      this.updatedBy = updatedBy;
      setCausedBy(updatedBy);
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @SuperBuilder
  @NoArgsConstructor
  public static class OwnershipTransferred extends GroupEvent {
    private UUID fromUserId;
    private UUID toUserId;

    public OwnershipTransferred(UUID groupId, UUID fromUserId, UUID toUserId) {
      super("OWNERSHIP_TRANSFERRED", groupId);
      this.fromUserId = fromUserId;
      this.toUserId = toUserId;
      setCausedBy(fromUserId);
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @SuperBuilder
  @NoArgsConstructor
  public static class GroupSettingsUpdated extends GroupEvent {
    private Boolean simplifyDebts;
    private Boolean allowNonMembersToView;
    private Boolean requireApprovalForExpenses;
    private UUID updatedBy;

    public GroupSettingsUpdated(UUID groupId, Boolean simplifyDebts, Boolean allowNonMembersToView,
        Boolean requireApprovalForExpenses, UUID updatedBy) {
      super("GROUP_SETTINGS_UPDATED", groupId);
      this.simplifyDebts = simplifyDebts;
      this.allowNonMembersToView = allowNonMembersToView;
      this.requireApprovalForExpenses = requireApprovalForExpenses;
      this.updatedBy = updatedBy;
      setCausedBy(updatedBy);
    }
  }
}
