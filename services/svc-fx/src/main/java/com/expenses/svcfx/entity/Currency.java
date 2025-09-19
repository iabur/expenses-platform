package com.expenses.svcfx.entity;

import java.time.ZonedDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "currencies")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Currency {

  @Id
  @Size(min = 3, max = 3, message = "Currency code must be exactly 3 characters")
  @NotBlank(message = "Currency code is required")
  @Column(name = "code", length = 3)
  private String code;

  @NotBlank(message = "Currency name is required")
  @Size(max = 100, message = "Currency name cannot exceed 100 characters")
  @Column(name = "name", length = 100, nullable = false)
  private String name;

  @Size(max = 10, message = "Currency symbol cannot exceed 10 characters")
  @Column(name = "symbol", length = 10)
  private String symbol;

  @Builder.Default
  @Column(name = "decimal_places")
  private Integer decimalPlaces = 2;

  @Builder.Default
  @NotNull
  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private ZonedDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private ZonedDateTime updatedAt;

  // Helper methods
  public boolean isActive() {
    return Boolean.TRUE.equals(isActive);
  }

  public void activate() {
    this.isActive = true;
  }

  public void deactivate() {
    this.isActive = false;
  }

  // Format amount according to currency decimal places
  public String formatAmount(long amountCents) {
    if (decimalPlaces == null || decimalPlaces == 0) {
      return String.valueOf(amountCents / 100);
    }

    double amount = amountCents / 100.0;
    return String.format("%." + decimalPlaces + "f", amount);
  }

  @Override
  public String toString() {
    return String.format("Currency{code='%s', name='%s', symbol='%s'}", code, name, symbol);
  }
}
