package com.iot.attendance.entity;

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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "verification_logs")
public class VerificationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @Column(name = "scanned_uid", nullable = false)
    private String scannedUid;

    @Column(name = "similarity_percent", precision = 5, scale = 2)
    private BigDecimal similarityPercent;

    @Column(name = "liveness_passed")
    private Boolean livenessPassed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationResult result = VerificationResult.PENDING;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "model_name")
    private String modelName;

    @Column(name = "model_version")
    private String modelVersion;

    @Column(name = "notification_sent", nullable = false)
    private Boolean notificationSent = false;

    @Column(name = "notification_error")
    private String notificationError;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public String getScannedUid() { return scannedUid; }
    public void setScannedUid(String scannedUid) { this.scannedUid = scannedUid; }
    public BigDecimal getSimilarityPercent() { return similarityPercent; }
    public void setSimilarityPercent(BigDecimal similarityPercent) { this.similarityPercent = similarityPercent; }
    public Boolean getLivenessPassed() { return livenessPassed; }
    public void setLivenessPassed(Boolean livenessPassed) { this.livenessPassed = livenessPassed; }
    public VerificationResult getResult() { return result; }
    public void setResult(VerificationResult result) { this.result = result; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
    public Boolean getNotificationSent() { return notificationSent; }
    public void setNotificationSent(Boolean notificationSent) { this.notificationSent = notificationSent; }
    public String getNotificationError() { return notificationError; }
    public void setNotificationError(String notificationError) { this.notificationError = notificationError; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(OffsetDateTime startedAt) { this.startedAt = startedAt; }
    public OffsetDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; }
}