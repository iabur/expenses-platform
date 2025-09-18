package com.expenses.svcuser.entity;

import java.time.LocalTime;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_preferences")
public class UserPreferences {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "notification_email", nullable = false)
  private Boolean notificationEmail = true;

  @Column(name = "notification_push", nullable = false)
  private Boolean notificationPush = true;

  @Column(name = "notification_sms", nullable = false)
  private Boolean notificationSms = false;

  @Enumerated(EnumType.STRING)
  @Column(name = "digest_frequency", length = 20)
  private DigestFrequency digestFrequency = DigestFrequency.DAILY;

  @Column(name = "quiet_hours_start")
  private LocalTime quietHoursStart;

  @Column(name = "quiet_hours_end")
  private LocalTime quietHoursEnd;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private ZonedDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private ZonedDateTime updatedAt;

  // Constructors
  public UserPreferences() {
  }

  public UserPreferences(User user) {
    this.user = user;
  }

  // Getters and Setters
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public Boolean getNotificationEmail() {
    return notificationEmail;
  }

  public void setNotificationEmail(Boolean notificationEmail) {
    this.notificationEmail = notificationEmail;
  }

  public Boolean getNotificationPush() {
    return notificationPush;
  }

  public void setNotificationPush(Boolean notificationPush) {
    this.notificationPush = notificationPush;
  }

  public Boolean getNotificationSms() {
    return notificationSms;
  }

  public void setNotificationSms(Boolean notificationSms) {
    this.notificationSms = notificationSms;
  }

  public DigestFrequency getDigestFrequency() {
    return digestFrequency;
  }

  public void setDigestFrequency(DigestFrequency digestFrequency) {
    this.digestFrequency = digestFrequency;
  }

  public LocalTime getQuietHoursStart() {
    return quietHoursStart;
  }

  public void setQuietHoursStart(LocalTime quietHoursStart) {
    this.quietHoursStart = quietHoursStart;
  }

  public LocalTime getQuietHoursEnd() {
    return quietHoursEnd;
  }

  public void setQuietHoursEnd(LocalTime quietHoursEnd) {
    this.quietHoursEnd = quietHoursEnd;
  }

  public ZonedDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(ZonedDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public ZonedDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(ZonedDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  // Enum for digest frequency
  public enum DigestFrequency {
    DAILY, WEEKLY, MONTHLY, NEVER
  }
}
