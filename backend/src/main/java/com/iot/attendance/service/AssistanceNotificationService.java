package com.iot.attendance.service;

import com.iot.attendance.entity.AssistanceRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class AssistanceNotificationService {

    private static final Logger log = LoggerFactory.getLogger(AssistanceNotificationService.class);
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${attendance.admin-email:}")
    private String adminEmail;

    public AssistanceNotificationService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    public void notifyIncident(AssistanceRequest request) {
        String subject = "[SU CO] Yeu cau ho tro tu thiet bi";
        String body = String.format(
                "He thong diem danh phat hien su co:\n\n"
                        + "Thoi gian: %s\n"
                        + "Nguon: %s\n"
                        + "Loai: %s\n"
                        + "Trang thai: %s\n\n"
                        + "Vui long kiem tra tai trang Quan ly su co.",
                TIME_FORMAT.format(request.getCreatedAt()),
                request.getSource(),
                request.getMessage(),
                request.getStatus());

        if (adminEmail == null || adminEmail.isBlank()) {
            log.info("No admin email configured; skipping incident notification");
            request.setNotificationError("NO_ADMIN_EMAIL");
            return;
        }

        try {
            sendEmail(adminEmail, subject, body);
            request.setNotificationSent(true);
            log.info("Incident notification sent to {} for assistance {}", adminEmail, request.getId());
        } catch (RuntimeException exception) {
            request.setNotificationError("EMAIL_FAILED: " + exception.getMessage());
            log.warn("Incident notification failed for assistance {}", request.getId(), exception);
        }
    }

    private void sendEmail(String to, String subject, String body) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new IllegalStateException("SMTP_NOT_CONFIGURED");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(adminEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}
