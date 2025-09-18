package com.expenses.svcgroup.entity;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "groups")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = { "members", "settings", "invitations" })
public class Group {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @EqualsAndHashCode.Include
  private UUID id;

  @NotBlank(message = "Group name is required")
  @Size(max = 200, message = "Group name must be less than 200 characters")
  @Column(nullable = false)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(length = 20, nullable = false)
  @Builder.Default
  private GroupType type = GroupType.GENERAL;

  @Size(min = 3, max = 3, message = "Currency code must be 3 characters")
  @Column(name = "default_currency", length = 3, nullable = false)
  @Builder.Default
  private String defaultCurrency = "USD";

  @Column(name = "avatar_url")
  private String avatarUrl;

  @Column(name = "created_by", nullable = false)
  private UUID createdBy;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private ZonedDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private ZonedDateTime updatedAt;

  @Column(name = "is_active", nullable = false)
  @Builder.Default
  private Boolean isActive = true;

  @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @Builder.Default
  private Set<GroupMember> members = new HashSet<>();

  @OneToOne(mappedBy = "group", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private GroupSettings settings;

  @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @Builder.Default
  private Set<GroupInvitation> invitations = new HashSet<>();

  // Helper methods
  public void addMember(GroupMember member) {
    members.add(member);
    member.setGroup(this);
  }

  public void removeMember(GroupMember member) {
    members.remove(member);
    member.setGroup(null);
  }

  public long getActiveMemberCount() {
    return members.stream()
        .filter(member -> member.getStatus() == GroupMember.MemberStatus.ACTIVE)
        .count();
  }

  public boolean hasOwner(UUID userId) {
    return members.stream()
        .anyMatch(member -> member.getUserId().equals(userId) && member.isOwner());
  }

  public boolean hasAdmin(UUID userId) {
    return members.stream()
        .anyMatch(member -> member.getUserId().equals(userId) && member.isAdmin());
  }

  public boolean hasMember(UUID userId) {
    return members.stream()
        .anyMatch(member -> member.getUserId().equals(userId) && member.isActive());
  }

  // Group type enum
  public enum GroupType {
    GENERAL, HOUSEHOLD, TRIP, PROJECT
  }
}