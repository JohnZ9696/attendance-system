package com.iot.attendance.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TwilioSmsService {
    private static final Logger log = LoggerFactory.getLogger(TwilioSmsService.class);

    @Value("${twilio.account-sid:}")
    private String accountSid;

    @Value("${twilio.auth-token:}")
    private String authToken;

    @Value("${twilio.phone-number:}")
    private String fromPhoneNumber;

    private boolean configured;

    @PostConstruct
    public void init() {
        if (accountSid != null && !accountSid.isBlank() && authToken != null && !authToken.isBlank()) {
            try {
                Twilio.init(accountSid, authToken);
                configured = true;
                log.info("Twilio SMS Service initialized.");
            } catch (Exception e) {
                log.error("Failed to initialize Twilio: {}", e.getMessage());
                configured = false;
            }
        } else {
            log.warn("Twilio credentials not fully provided. SMS will be simulated.");
            configured = false;
        }
    }

    public void sendSms(String to, String messageBody) {
        if (to == null || to.isBlank()) return;

        if (!configured) {
            log.info("[SIMULATED SMS to {}]: {}", to, messageBody);
            return;
        }

        try {
            Message message = Message.creator(
                    new PhoneNumber(to),
                    new PhoneNumber(fromPhoneNumber),
                    "sms_event_notifications"
            ).create();
            log.info("SMS sent to {}. SID: {}", to, message.getSid());
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", to, e.getMessage());
            throw new RuntimeException("SMS_FAILED", e);
        }
    }
}
