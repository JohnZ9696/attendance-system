package com.iot.attendance.controller;

import com.iot.attendance.service.SseEventService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/monitor")
public class MonitorController {

    private final SseEventService service;

    public MonitorController(SseEventService service) {
        this.service = service;
    }

    @GetMapping(value = "/events", produces = "text/event-stream")
    public SseEmitter streamEvents() {
        return service.subscribe();
    }
}
