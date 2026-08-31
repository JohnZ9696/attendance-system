package com.iot.attendance.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UdpDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(UdpDiscoveryService.class);
    private static final String REQUEST = "ATTENDANCE_DISCOVER_V1";

    private final int discoveryPort;
    private final int serverPort;
    private DatagramSocket socket;
    private Thread worker;

    public UdpDiscoveryService(
            @Value("${discovery.port:4210}") int discoveryPort,
            @Value("${server.port:8080}") int serverPort) {
        this.discoveryPort = discoveryPort;
        this.serverPort = serverPort;
    }

    @PostConstruct
    public void start() {
        worker = new Thread(this::listen, "udp-discovery");
        worker.setDaemon(true);
        worker.start();
    }

    private void listen() {
        try (DatagramSocket serverSocket = new DatagramSocket(discoveryPort)) {
            socket = serverSocket;
            serverSocket.setBroadcast(true);
            log.info("UDP discovery listening on port {}", discoveryPort);

            byte[] buffer = new byte[128];
            while (!serverSocket.isClosed()) {
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                serverSocket.receive(request);
                String message = new String(
                        request.getData(), request.getOffset(), request.getLength(), StandardCharsets.US_ASCII);
                if (!REQUEST.equals(message)) {
                    continue;
                }

                byte[] response = ("ATTENDANCE_SERVER_V1:" + serverPort)
                        .getBytes(StandardCharsets.US_ASCII);
                serverSocket.send(new DatagramPacket(
                        response, response.length, request.getAddress(), request.getPort()));
            }
        } catch (SocketException e) {
            if (socket == null || !socket.isClosed()) {
                log.warn("UDP discovery stopped: {}", e.getMessage());
            }
        } catch (IOException e) {
            log.warn("UDP discovery failed: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void stop() {
        if (socket != null) {
            socket.close();
        }
    }
}
