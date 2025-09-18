package com.expenses.svcgroup.entity;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "group_members", uniqueConstraints = @UniqueConstraint(columnNames = { "group_id", "user_id" }))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = { "group" })
public class GroupMember {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @EqualsAndHashCode.Include
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "group_id", nullable = false)
  private Group group;

  @Column(name = "user_id", nullable = false)
  @EqualsAndHashCode.Include
  private UUID userId;

  @Enumerated(EnumType.STRING)
  @Column(length = 20, nullable = false)
  @Builder.Default
  private MemberRole role = MemberRole.MEMBER;

  @Column(name = "joined_at", nullable = false)
  @Builder.Default
  private ZonedDateTime joinedAt = ZonedDateTime.now();

  @Enumerated(EnumType.STRING)
  @Column(length = 20, nullable = false)
  @Builder.Default
  private MemberStatus status = MemberStatus.ACTIVE;

  @Column(name = "invited_by")
  private UUID invitedBy;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private ZonedDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private ZonedDateTime updatedAt;

  // Helper methods
  public boolean isOwner() {
    return role == MemberRole.OWNER;
  }

  public boolean isAdmin() {
    return role == MemberRole.ADMIN || role == MemberRole.OWNER;
  }

  public boolean isActive() {
    return status == MemberStatus.ACTIVE;
  }

  public void promote() {
    if (role == MemberRole.MEMBER) {
      role = MemberRole.ADMIN;
    }
  }

  public void demote() {
    if (role == MemberRole.ADMIN) {
      role = MemberRole.MEMBER;
    }
  }

  public void leave() {
    if (role != MemberRole.OWNER) {
      status = MemberStatus.LEFT;
    } else {
      throw new IllegalStateException("Owner cannot leave the group");
    }
  }

  public void remove() {
    if (role != MemberRole.OWNER) {
      status = MemberStatus.REMOVED;
    } else {
      throw new IllegalStateException("Owner cannot be removed from the group");
    }
  }

  // Enums
  public enum MemberRole {
    OWNER, ADMIN, MEMBER
  }

  public enum MemberStatus {
    ACTIVE, INVITED, LEFT, REMOVED
  }
}