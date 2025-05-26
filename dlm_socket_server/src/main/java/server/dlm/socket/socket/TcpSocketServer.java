package server.dlm.socket.socket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import server.dlm.socket.entity.in.InBox;
import server.dlm.socket.entity.main.MainBox;
import server.dlm.socket.entity.main.MasterTcpSocket;
import server.dlm.socket.repository.in.InBoxRepository;
import server.dlm.socket.repository.main.MainBoxRepository;
import server.dlm.socket.repository.main.MasterTcpSocketRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Log4j2
public class TcpSocketServer {

    @Value("${socket.server.send-delay-ms}")
    private long sendDelayMs;

    @Value("${socket.server.port}")
    private int portSocketServer;

    @Autowired
    private InBoxRepository inBoxRepository;

    @Autowired
    private MainBoxRepository mainBoxRepository;

    @Autowired
    private MasterTcpSocketRepository masterTcpSocketRepository;

    private final Map<String, Socket> imeiToSocketMap = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private ServerSocket serverSocket;
    private final Map<String, Long> imeiToLastReceivedTime = new ConcurrentHashMap<>();

    @PostConstruct
    public void startServer() {
        new Thread(() -> {
            try {
                // Listen on all interfaces (IPv4 & IPv6 compatible)
                InetAddress bindAddr = InetAddress.getByName("::");
                serverSocket = new ServerSocket(portSocketServer, 50, bindAddr);

                log.info("✅ TCP Server (IPv4 & IPv6) started on [{}]:{}", bindAddr.getHostAddress(), portSocketServer);

                while (true) {
                    Socket socket = serverSocket.accept();
                    String clientIp = socket.getInetAddress().getHostAddress();
                    handleConnection(socket, clientIp);
                }

            } catch (IOException e) {
                log.error("❌ Error in TCP Server: {}", e.getMessage(), e);
            }
        }).start();
    }

    /**
     * <p>MESSAGE TEXT SAMPLE: <352840051234567|220.5|5.3|0.95|ok></p>
     * <p>MESSAGE JSON SAMPLE:
     * {
     * "imei": "352840051234567",
     * "voltage": 220.5,
     * "current": 5.3,
     * "powerFactor": 0.95,
     * "status": "ok"
     * }</p>
     */
    private void handleConnection(Socket socket, String clientIp) {
        new Thread(() -> startConnectionThread(socket, clientIp)).start();
    }

    private void startConnectionThread(Socket socket, String clientIp) {
        String socketNo = UUID.randomUUID().toString();
        AtomicBoolean authenticated = new AtomicBoolean(false);
        AtomicBoolean hasReceivedLogin = new AtomicBoolean(false);
        String[] imeiHolder = new String[1];  // Trick to mutate from inner scope

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        ScheduledFuture<?> timeoutTask = scheduler.schedule(() -> {
            if (!hasReceivedLogin.get()) {
                log.warn("⏱️ No login received within 30s from client {} → closing connection", clientIp);
                tryClose(socket);
            }
        }, 30, TimeUnit.SECONDS);

        try (socket;
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            log.info("🔌 Client {} connected", clientIp);

            // Wait for login before processing further
            if (!waitForLogin(reader, socket, clientIp, authenticated, hasReceivedLogin, imeiHolder, socketNo)) {
                return;
            }

            String imei = imeiHolder[0];
            String rawData;

            while ((rawData = reader.readLine()) != null) {
                processIncomingData(rawData.trim(), imei, clientIp, socketNo);
            }

        } catch (IOException e) {
            log.error("💥 Connection error from {}: {}", clientIp, e.getMessage());
        } finally {
            timeoutTask.cancel(true);
            scheduler.shutdown();
            handleSocketCleanup(imeiHolder[0], socketNo);
        }
    }

    private boolean waitForLogin(BufferedReader reader, Socket socket, String clientIp,
                                 AtomicBoolean authenticated, AtomicBoolean hasReceivedLogin,
                                 String[] imeiHolder, String socketNo) throws IOException {
        String rawData;
        while ((rawData = reader.readLine()) != null) {
            rawData = rawData.trim();
            log.info("📥 Received from {}: {}", clientIp, rawData);

            if (isLoginPacket(rawData)) {
                String[] parts = rawData.substring(1, rawData.length() - 1).split("\\|");
                String imei = parts[0];
                hasReceivedLogin.set(true);

                if (!authenticateDevice(imei)) {
                    log.warn("❌ IMEI {} not in whitelist → closing connection", imei);
                    tryClose(socket);
                    return false;
                }

                authenticated.set(true);
                imeiHolder[0] = imei;
                updateConnectionState(imei, socketNo, true);
                imeiToSocketMap.put(imei, socket);
                log.info("✅ Device {} authenticated successfully", imei);
                return true;
            } else {
                log.warn("⚠️ Ignored non-login packet before authentication: {}", rawData);
            }
        }
        return false;
    }

    private boolean isLoginPacket(String rawData) {
        return rawData.startsWith("<") && rawData.endsWith(">") && rawData.contains("|LOGIN>");
    }

    private boolean authenticateDevice(String imei) {
        return masterTcpSocketRepository.existsByDeviceId(imei);
    }

    private void processIncomingData(String rawData, String imei, String clientIp, String socketNo) {
        long now = System.currentTimeMillis();
        long lastTime = imeiToLastReceivedTime.getOrDefault(imei, 0L);
        long elapsed = now - lastTime;

        if (elapsed < sendDelayMs) {
            log.warn("⏱️ IMEI {} sending too fast ({}ms < {}ms), message ignored", imei, elapsed, sendDelayMs);
            return;
        }

        imeiToLastReceivedTime.put(imei, now);
        saveRawOnly(rawData, clientIp, imei, socketNo);

        try {
            if (rawData.startsWith("{") && rawData.endsWith("}")) {
                JsonNode json = objectMapper.readTree(rawData);
                if (json.has("imei")) {
                    processJsonMessage(json, imei, socketNo);
                } else {
                    log.warn("⚠️ JSON missing IMEI: {}", rawData);
                }
            } else if (rawData.startsWith("<") && rawData.endsWith(">")) {
                String[] parts = rawData.substring(1, rawData.length() - 1).split("\\|");
                if (parts.length == 5) {
                    processTextMessage(imei, parts, socketNo);
                } else {
                    log.warn("❗ Invalid message format after login: {}", rawData);
                }
            } else {
                log.warn("❗ Unknown message format after login: {}", rawData);
            }
        } catch (Exception e) {
            log.warn("⚠️ Error processing message: {}", e.getMessage());
        }
    }

    private void handleSocketCleanup(String imei, String socketNo) {
        if (imei != null) {
            updateConnectionState(imei, socketNo, false);
            log.info("❎ IMEI {} disconnected (socket {})", imei, socketNo);
            imeiToSocketMap.remove(imei);
        }
    }

    private void tryClose(Socket socket) {
        try {
            socket.close();
        } catch (IOException ignored) {}
    }


    private void updateConnectionState(String imei, String socketNo, boolean connected) {
        MasterTcpSocket tcpSocket = masterTcpSocketRepository.findByDeviceId(imei);
        tcpSocket.setSocketNo(socketNo);
        tcpSocket.setSocketStatus(connected ? "1" : "0");
        tcpSocket.setSysDts(LocalDateTime.now());
        masterTcpSocketRepository.save(tcpSocket);
    }

    private void processTextMessage(String imei, String[] parts, String socketNo) {
        try {
            MainBox mainBox = new MainBox();
            mainBox.setImei(imei);
            mainBox.setSocketSessionId(socketNo);
            mainBox.setVoltage(Double.parseDouble(parts[1]));
            mainBox.setCurrent(Double.parseDouble(parts[2]));
            mainBox.setPowerFactor(Double.parseDouble(parts[3]));
            mainBox.setStatus(parts[4]);
            mainBox.setDeviceTimestamp(LocalDateTime.now(ZoneOffset.UTC));
            mainBoxRepository.save(mainBox);

            log.info("✅ Parsed data saved for IMEI {}", imei);
        } catch (Exception e) {
            log.warn("⚠️ Failed to parse data for IMEI {}: {}", imei, e.getMessage());
        }
    }

    private void processJsonMessage(JsonNode json, String imei, String socketNo) {
        try {
            MainBox mainBox = new MainBox();
            mainBox.setImei(imei);
            mainBox.setSocketSessionId(socketNo);
            mainBox.setVoltage(json.get("voltage").asDouble());
            mainBox.setCurrent(json.get("current").asDouble());
            mainBox.setPowerFactor(json.get("powerFactor").asDouble());
            mainBox.setStatus(json.get("status").asText());
            mainBox.setDeviceTimestamp(LocalDateTime.now(ZoneOffset.UTC));
            mainBoxRepository.save(mainBox);

            log.info("✅ JSON parsed & saved for IMEI {}", imei);
        } catch (Exception e) {
            log.warn("⚠️ Failed to parse JSON for IMEI {}: {}", imei, e.getMessage());
        }
    }

    private void saveRawOnly(String rawData, String clientIp, String imei, String socketNo) {
        InBox inBox = new InBox();
        inBox.setDeviceId(imei);
        inBox.setSocketSessionId(socketNo);
        inBox.setClientIp(clientIp);
        inBox.setData(rawData);
        inBox.setInsertDts(LocalDateTime.now());
        inBox.setProcessStatus("N");
        inBoxRepository.save(inBox);

        log.info("✅ Raw message saved for IMEI {}", imei);
    }

    public int getActiveConnectionCount() {
        return imeiToSocketMap.size();
    }

    public void sendMessageToClient(String imei, String message) {
        Socket socket = imeiToSocketMap.get(imei);
        if (socket == null || socket.isClosed()) {
            log.warn("❌ Cannot send message, socket for IMEI {} is not active", imei);
            return;
        }

        try {
            socket.getOutputStream().write((message + "\n").getBytes());
            socket.getOutputStream().flush();
            log.info("📤 Sent message to IMEI {}: {}", imei, message);
        } catch (IOException e) {
            log.error("❌ Failed to send message to IMEI {}: {}", imei, e.getMessage());
        }
    }

    @PreDestroy
    public void shutdownServer() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                log.info("🛑 TCP Server stopped and port {} released", portSocketServer);
            }
        } catch (IOException e) {
            log.error("Error closing ServerSocket", e);
        }
    }
}
