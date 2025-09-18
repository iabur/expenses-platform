package com.expenses.common.event;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventName")
@JsonSubTypes({
    @JsonSubTypes.Type(value = UserEvent.UserCreated.class, name = "USER_CREATED"),
    @JsonSubTypes.Type(value = UserEvent.UserUpdated.class, name = "USER_UPDATED"),
    @JsonSubTypes.Type(value = UserEvent.UserDeactivated.class, name = "USER_DEACTIVATED"),
    @JsonSubTypes.Type(value = UserEvent.UserPreferencesUpdated.class, name = "USER_PREFERENCES_UPDATED")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
public abstract class UserEvent extends DomainEvent {

  public static final String TOPIC = "user-events";

  protected UserEvent(String eventName, UUID userId) {
    super("USER", eventName, userId, "User");
  }

  @Override
  public String getTopicName() {
    return TOPIC;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @SuperBuilder
  @NoArgsConstructor
  public static class UserCreated extends UserEvent {
    private String email;
    private String firstName;
    private String lastName;
    private String displayName;
    private String defaultCurrency;
    private String locale;
    private String timezone;

    public UserCreated(UUID userId, String email, String firstName, String lastName,
        String displayName, String defaultCurrency, String locale, String timezone) {
      super("USER_CREATED", userId);
      this.email = email;
      this.firstName = firstName;
      this.lastName = lastName;
      this.displayName = displayName;
      this.defaultCurrency = defaultCurrency;
      this.locale = locale;
      this.timezone = timezone;
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @SuperBuilder
  @NoArgsConstructor
  public static class UserUpdated extends UserEvent {
    private String email;
    private String firstName;
    private String lastName;
    private String displayName;
    private String defaultCurrency;
    private String locale;
    private String timezone;

    public UserUpdated(UUID userId, String email, String firstName, String lastName,
        String displayName, String defaultCurrency, String locale, String timezone) {
      super("USER_UPDATED", userId);
      this.email = email;
      this.firstName = firstName;
      this.lastName = lastName;
      this.displayName = displayName;
      this.defaultCurrency = defaultCurrency;
      this.locale = locale;
      this.timezone = timezone;
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @SuperBuilder
  @NoArgsConstructor
  public static class UserDeactivated extends UserEvent {
    public UserDeactivated(UUID userId) {
      super("USER_DEACTIVATED", userId);
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @SuperBuilder
  @NoArgsConstructor
  public static class UserPreferencesUpdated extends UserEvent {
    private Boolean notificationEmail;
    private Boolean notificationPush;
    private Boolean notificationSms;
    private String digestFrequency;

    public UserPreferencesUpdated(UUID userId, Boolean notificationEmail, Boolean notificationPush,
        Boolean notificationSms, String digestFrequency) {
      super("USER_PREFERENCES_UPDATED", userId);
      this.notificationEmail = notificationEmail;
      this.notificationPush = notificationPush;
      this.notificationSms = notificationSms;
      this.digestFrequency = digestFrequency;
    }
  }
}
