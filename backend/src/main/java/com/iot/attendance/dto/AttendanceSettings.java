package com.iot.attendance.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class AttendanceSettings {
    @JsonFormat(pattern = "HH:mm")
    private LocalTime classStartTime;

    private int lateGraceMinutes;

    private List<LocalDate> dayOffs = List.of();

    private List<Integer> weeklyDayOffs = List.of(0, 6);

    public LocalTime getClassStartTime() { return classStartTime; }
    public void setClassStartTime(LocalTime classStartTime) { this.classStartTime = classStartTime; }
    public int getLateGraceMinutes() { return lateGraceMinutes; }
    public void setLateGraceMinutes(int lateGraceMinutes) { this.lateGraceMinutes = lateGraceMinutes; }
    public List<LocalDate> getDayOffs() { return dayOffs; }
    public void setDayOffs(List<LocalDate> dayOffs) { this.dayOffs = dayOffs == null ? List.of() : dayOffs; }
    public List<Integer> getWeeklyDayOffs() { return weeklyDayOffs; }
    public void setWeeklyDayOffs(List<Integer> weeklyDayOffs) { this.weeklyDayOffs = weeklyDayOffs == null ? List.of() : weeklyDayOffs; }
}