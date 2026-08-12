package com.iot.attendance.student;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "students")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_code")
    private String studentCode;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "rfid_uid")
    private String rfidUid;

    private String email;

    @Column(name = "parent_phone")
    private String parentPhone;

    @Column(name = "face_image")
    private String faceImage;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public Long getId() {
        return id;
    }

    public String getStudentCode() {
        return studentCode;
    }

    public String getFullName() {
        return fullName;
    }

    public String getRfidUid() {
        return rfidUid;
    }

    public String getEmail() {
        return email;
    }

    public String getParentPhone() {
        return parentPhone;
    }

    public String getFaceImage() {
        return faceImage;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setStudentCode(String studentCode) {
    this.studentCode = studentCode;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setRfidUid(String rfidUid) {
        this.rfidUid = rfidUid;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setParentPhone(String parentPhone) {
        this.parentPhone = parentPhone;
    }

    public void setFaceImage(String faceImage) {
        this.faceImage = faceImage;
    }
}