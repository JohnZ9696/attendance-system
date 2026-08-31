package com.iot.attendance.service;

import com.iot.attendance.entity.AttendanceLog;
import com.iot.attendance.entity.Student;
import com.iot.attendance.entity.VerificationLog;
import com.iot.attendance.repository.VerificationLogRepository;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class ParentNotificationService {

    private static final Logger log = LoggerFactory.getLogger(ParentNotificationService.class);
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final VerificationLogRepository verificationLogRepository;
    public ParentNotificationService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            VerificationLogRepository verificationLogRepository
    ) {
        this.mailSenderProvider = mailSenderProvider;
        this.verificationLogRepository = verificationLogRepository;
    }

    public void notifyCheckIn(Student student, AttendanceLog attendanceLog, VerificationLog verificationLog) {
        if (Boolean.TRUE.equals(verificationLog.getNotificationSent())) {
            return;
        }

        String status = "ON_TIME".equals(attendanceLog.getStatus()) ? "đúng giờ" : "đi muộn";
        String time = TIME_FORMAT.format(attendanceLog.getCheckTime());
        String subject = "Thông báo điểm danh - " + student.getFullName();
        String body = String.format(
                "Kính gửi phụ huynh của %s (MSSV %s),\n\n"
                        + "Con của quý phụ huynh đã điểm danh %s lúc %s.\n"
                        + "Trạng thái: %s\n\n"
                        + "Trân trọng,\nHệ thống điểm danh",
                student.getFullName(), student.getMssv(), status, time, status);

        boolean delivered = false;
        String error = null;

        if (hasText(student.getParentEmail())) {
            try {
                sendEmail(student.getParentEmail(), subject, body);
                delivered = true;
            } catch (RuntimeException exception) {
                error = "EMAIL_FAILED: " + exception.getMessage();
                log.warn("Parent email notification failed for student {}", student.getId(), exception);
            }
        }



        if (!delivered && !hasText(student.getParentEmail())) {
            error = "NO_PARENT_CONTACT";
            log.info("No parent email for student {}; skipping notification", student.getId());
        }

        verificationLog.setNotificationSent(delivered);
        verificationLog.setNotificationError(error);
        verificationLogRepository.save(verificationLog);
    }

    private void sendEmail(String to, String subject, String body) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.info("SMTP not configured; email would be sent to {} ({}).", to, subject);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}