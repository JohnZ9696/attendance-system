package com.iot.attendance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "attendance_logs")
public class AttendanceRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private User user;

    @Column(name = "check_time", nullable = false)
    private OffsetDateTime checkInTime;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String method;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public OffsetDateTime getCheckInTime() { return checkInTime; }
    public void setCheckInTime(OffsetDateTime checkInTime) { this.checkInTime = checkInTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
}
