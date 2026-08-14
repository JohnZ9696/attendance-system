package com.iot.attendance.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalTime;

public class AttendanceSettings {
    @JsonFormat(pattern = "HH:mm")
    private LocalTime classStartTime;

    private int lateGraceMinutes;

    public LocalTime getClassStartTime() { return classStartTime; }
    public void setClassStartTime(LocalTime classStartTime) { this.classStartTime = classStartTime; }
    public int getLateGraceMinutes() { return lateGraceMinutes; }
    public void setLateGraceMinutes(int lateGraceMinutes) { this.lateGraceMinutes = lateGraceMinutes; }
}