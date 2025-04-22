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
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Log4j2
public class TcpSocketServer {

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

    @PostConstruct
    public void startServer() {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(portSocketServer);
                log.info("✅ TCP Server started on port {}", portSocketServer);

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

    /**
     TEXT DATA SAMPLE: <352840051234567|220.5|5.3|0.95|ok>
     JSON DATA SAMPLE:
     {
     "imei": "352840051234567",
     "voltage": 220.5,
     "current": 5.3,
     "powerFactor": 0.95,
     "status": "ok"
     }
     */
    private void handleConnection(Socket socket, String clientIp) {
        new Thread(() -> {
            String socketNo = UUID.randomUUID().toString();
            String imei = null;

            try (socket;
                 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                log.info("🔌 Client {} connected", clientIp);

                String rawData;

                while ((rawData = reader.readLine()) != null) {
                    rawData = rawData.trim();
                    log.info("📥 Received from {}: {}", clientIp, rawData);

                    if (rawData.startsWith("{") && rawData.endsWith("}")) {
                        try {
                            JsonNode json = objectMapper.readTree(rawData);
                            if (json.has("imei")) {
                                imei = json.get("imei").asText();
                                updateConnectionState(imei, socketNo, true);
                                imeiToSocketMap.put(imei, socket);
                                saveRawOnly(rawData, clientIp, imei, socketNo);

                                if (json.hasNonNull("voltage") && json.hasNonNull("current") &&
                                        json.hasNonNull("powerFactor") && json.hasNonNull("status")) {
                                    processJsonMessage(json, imei, socketNo);
                                } else {
                                    log.warn("⚠️ Missing data fields in JSON for IMEI {}", imei);
                                }
                            } else {
                                log.warn("⚠️ JSON missing IMEI: {}", rawData);
                            }
                        } catch (Exception e) {
                            log.warn("⚠️ Invalid JSON format: {}", e.getMessage());
                        }
                        continue;
                    } else if (rawData.startsWith("<") && rawData.endsWith(">")) {
                        rawData = rawData.substring(1, rawData.length() - 1);

                        String[] parts = rawData.split("\\|");
                        if (parts.length == 5) {
                            imei = parts[0];
                            if (!masterTcpSocketRepository.existsByDeviceId(imei)) {
                                log.warn("IMEI {} not registered", imei);
                                continue;
                            }

                            updateConnectionState(imei, socketNo, true);
                            imeiToSocketMap.put(imei, socket);
                            saveRawOnly(rawData, clientIp, imei, socketNo);
                            processIncomingMessage(imei, parts, socketNo);
                        } else {
                            log.warn("❗ Invalid message: {}", rawData);
                        }
                    } else {
                        log.warn("❗ Invalid message: {} ", rawData);
                    }
                }

            } catch (IOException e) {
                log.error("💥 Connection error from {}: {}", clientIp, e.getMessage());
            } finally {
                if (imei != null) {
                    updateConnectionState(imei, socketNo, false);
                    log.info("❎ IMEI {} disconnected (socket {})", imei, socketNo);
                }
                imeiToSocketMap.remove(imei);
            }
        }).start();
    }

    private void updateConnectionState(String imei, String socketNo, boolean connected) {
        MasterTcpSocket tcpSocket = masterTcpSocketRepository.findByDeviceId(imei);
        tcpSocket.setSocketNo(socketNo);
        tcpSocket.setSocketStatus(connected ? "1" : "0");
        tcpSocket.setSysDts(LocalDateTime.now());
        masterTcpSocketRepository.save(tcpSocket);
    }

    private void processIncomingMessage(String imei, String[] parts, String socketNo) {
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

        log.info("📥 Raw message saved for IMEI {}", imei);
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
}
