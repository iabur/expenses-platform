package com.expenses.svcexpense.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "expense_attachments")
public class ExpenseAttachment {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "expense_id", nullable = false)
  private Expense expense;

  @Column(name = "file_name", nullable = false)
  @NotBlank(message = "File name is required")
  private String fileName;

  @Column(name = "file_url", nullable = false, columnDefinition = "TEXT")
  @NotBlank(message = "File URL is required")
  private String fileUrl;

  @Column(name = "file_size_bytes")
  private Long fileSizeBytes;

  @Column(name = "mime_type", length = 100)
  private String mimeType;

  @Column(name = "uploaded_by", nullable = false)
  @NotNull(message = "Uploaded by user ID is required")
  private UUID uploadedBy;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private ZonedDateTime createdAt;

  // Constructors
  public ExpenseAttachment() {
  }

  public ExpenseAttachment(Expense expense, String fileName, String fileUrl, UUID uploadedBy) {
    this.expense = expense;
    this.fileName = fileName;
    this.fileUrl = fileUrl;
    this.uploadedBy = uploadedBy;
  }

  public ExpenseAttachment(Expense expense, String fileName, String fileUrl,
      Long fileSizeBytes, String mimeType, UUID uploadedBy) {
    this.expense = expense;
    this.fileName = fileName;
    this.fileUrl = fileUrl;
    this.fileSizeBytes = fileSizeBytes;
    this.mimeType = mimeType;
    this.uploadedBy = uploadedBy;
  }

  // Getters and Setters
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public Expense getExpense() {
    return expense;
  }

  public void setExpense(Expense expense) {
    this.expense = expense;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public String getFileUrl() {
    return fileUrl;
  }

  public void setFileUrl(String fileUrl) {
    this.fileUrl = fileUrl;
  }

  public Long getFileSizeBytes() {
    return fileSizeBytes;
  }

  public void setFileSizeBytes(Long fileSizeBytes) {
    this.fileSizeBytes = fileSizeBytes;
  }

  public String getMimeType() {
    return mimeType;
  }

  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }

  public UUID getUploadedBy() {
    return uploadedBy;
  }

  public void setUploadedBy(UUID uploadedBy) {
    this.uploadedBy = uploadedBy;
  }

  public ZonedDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(ZonedDateTime createdAt) {
    this.createdAt = createdAt;
  }

  // Helper methods
  public boolean isImage() {
    return mimeType != null && mimeType.startsWith("image/");
  }

  public boolean isPdf() {
    return "application/pdf".equals(mimeType);
  }

  public String getFileSizeFormatted() {
    if (fileSizeBytes == null)
      return "Unknown";

    if (fileSizeBytes < 1024) {
      return fileSizeBytes + " B";
    } else if (fileSizeBytes < 1024 * 1024) {
      return String.format("%.1f KB", fileSizeBytes / 1024.0);
    } else {
      return String.format("%.1f MB", fileSizeBytes / (1024.0 * 1024.0));
    }
  }

  public String getFileExtension() {
    if (fileName == null)
      return "";

    int lastDotIndex = fileName.lastIndexOf('.');
    if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
      return fileName.substring(lastDotIndex + 1).toLowerCase();
    }
    return "";
  }
}
