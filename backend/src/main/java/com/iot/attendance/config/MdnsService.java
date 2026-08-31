package com.iot.attendance.config;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.InetAddress;
import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MdnsService {

    private static final Logger log = LoggerFactory.getLogger(MdnsService.class);

    private JmDNS jmDNS;
    private ServiceInfo serviceInfo;

    public MdnsService(
            @Value("${mdns.host:attendance}") String host,
            @Value("${mdns.port:8080}") int port) {
        try {
            jmDNS = JmDNS.create(InetAddress.getLocalHost());
            serviceInfo = ServiceInfo.create(
                    "_http._tcp.local.",
                    host + "._http._tcp.local.",
                    port,
                    "path=/"
            );
            jmDNS.registerService(serviceInfo);
            log.info("mDNS registered: {} resolves to {}:{}", host + ".local",
                    jmDNS.getInetAddress().getHostAddress(), port);
        } catch (IOException e) {
            log.warn("mDNS registration failed: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void close() {
        if (jmDNS != null) {
            jmDNS.unregisterAllServices();
            try {
                jmDNS.close();
            } catch (IOException ignored) {
            }
        }
    }
}
