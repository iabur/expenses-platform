package com.expenses.svcgroup.entity;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.apache.commons.lang3.RandomStringUtils;
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
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "group_invitations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = { "group" })
public class GroupInvitation {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @EqualsAndHashCode.Include
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "group_id", nullable = false)
  private Group group;

  @Column(name = "invited_by", nullable = false)
  @NotNull(message = "Invited by user ID is required")
  private UUID invitedBy;

  @Email(message = "Valid email is required")
  @NotBlank(message = "Invited email is required")
  @Column(name = "invited_email", nullable = false)
  @EqualsAndHashCode.Include
  private String invitedEmail;

  @Column(name = "invited_user_id")
  private UUID invitedUserId; // If the invited person already has an account

  @Enumerated(EnumType.STRING)
  @Column(length = 20, nullable = false)
  @Builder.Default
  private GroupMember.MemberRole role = GroupMember.MemberRole.MEMBER;

  @Column(name = "invitation_code", unique = true, nullable = false)
  @NotBlank(message = "Invitation code is required")
  @Builder.Default
  private String invitationCode = generateInvitationCode();

  @Column(name = "expires_at", nullable = false)
  @NotNull(message = "Expiration date is required")
  @Builder.Default
  private ZonedDateTime expiresAt = ZonedDateTime.now().plusDays(7); // Default 7 days expiry

  @Enumerated(EnumType.STRING)
  @Column(length = 20, nullable = false)
  @Builder.Default
  private InvitationStatus status = InvitationStatus.PENDING;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private ZonedDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private ZonedDateTime updatedAt;

  // Helper methods
  public boolean isExpired() {
    return ZonedDateTime.now().isAfter(expiresAt);
  }

  public boolean isPending() {
    return status == InvitationStatus.PENDING && !isExpired();
  }

  public boolean canBeAccepted() {
    return isPending();
  }

  public void accept() {
    if (!canBeAccepted()) {
      throw new IllegalStateException("Invitation cannot be accepted: " +
          (isExpired() ? "expired" : "status is " + status));
    }
    this.status = InvitationStatus.ACCEPTED;
  }

  public void decline() {
    if (!isPending()) {
      throw new IllegalStateException("Invitation cannot be declined: " +
          (isExpired() ? "expired" : "status is " + status));
    }
    this.status = InvitationStatus.DECLINED;
  }

  public void expire() {
    if (status == InvitationStatus.PENDING) {
      this.status = InvitationStatus.EXPIRED;
    }
  }

  public void resend() {
    if (status == InvitationStatus.PENDING && !isExpired()) {
      // Extend expiry by 7 more days
      this.expiresAt = ZonedDateTime.now().plusDays(7);
    } else {
      throw new IllegalStateException("Cannot resend invitation: " +
          (isExpired() ? "expired" : "status is " + status));
    }
  }

  public String getInvitationUrl(String baseUrl) {
    return baseUrl + "/invitations/" + invitationCode;
  }

  // Static helper method for generating invitation codes
  private static String generateInvitationCode() {
    return RandomStringUtils.randomAlphanumeric(32);
  }

  // Invitation status enum
  public enum InvitationStatus {
    PENDING, ACCEPTED, DECLINED, EXPIRED
  }
}