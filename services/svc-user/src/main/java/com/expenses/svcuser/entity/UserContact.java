package com.expenses.svcuser.entity;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

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

@Entity
@Table(name = "user_contacts", uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "contact_user_id" }))
public class UserContact {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "contact_user_id", nullable = false)
  private User contactUser;

  @Enumerated(EnumType.STRING)
  @Column(name = "relationship_type", length = 20)
  private RelationshipType relationshipType = RelationshipType.FRIEND;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private ZonedDateTime createdAt;

  // Constructors
  public UserContact() {
  }

  public UserContact(User user, User contactUser) {
    this.user = user;
    this.contactUser = contactUser;
  }

  public UserContact(User user, User contactUser, RelationshipType relationshipType) {
    this.user = user;
    this.contactUser = contactUser;
    this.relationshipType = relationshipType;
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

  public User getContactUser() {
    return contactUser;
  }

  public void setContactUser(User contactUser) {
    this.contactUser = contactUser;
  }

  public RelationshipType getRelationshipType() {
    return relationshipType;
  }

  public void setRelationshipType(RelationshipType relationshipType) {
    this.relationshipType = relationshipType;
  }

  public ZonedDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(ZonedDateTime createdAt) {
    this.createdAt = createdAt;
  }

  // Enum for relationship types
  public enum RelationshipType {
    FRIEND, FAMILY, COLLEAGUE
  }
}
