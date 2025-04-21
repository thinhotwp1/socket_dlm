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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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

    private final Map<String, String> activeConnections = new HashMap<>();
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

    private void handleConnection(Socket socket, String clientIp) {
        new Thread(() -> {
            String socketNo = UUID.randomUUID().toString(); // Create socketNo
            String imei = null;

            try (socket;
                 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                log.info("🔌 Client {} connected", clientIp);

                String rawData;
                while ((rawData = reader.readLine()) != null) {
                    log.info("📥 Received from {}: {}", clientIp, rawData);

                    if (rawData.startsWith("{") && rawData.endsWith("}")) {
                        // JSON message
                        try {
                            JsonNode json = objectMapper.readTree(rawData);
                            if (json.has("imei")) {
                                imei = json.get("imei").asText();
//                                if (!masterTcpSocketRepository.existsByDeviceId(imei)) {
//                                    log.warn("IMEI {} not registered in masterTcpSockets", imei);
//                                    continue;
//                                }

                                updateConnectionState(imei, socketNo, true);
                                activeConnections.put(clientIp, imei);
                                saveRawOnly(rawData, clientIp, imei, socketNo);

                                if (json.hasNonNull("voltage") && json.hasNonNull("current")
                                        && json.hasNonNull("powerFactor") && json.hasNonNull("status")) {
                                    processJsonMessage(json, imei, socketNo);
                                } else {
                                    log.warn("⚠️ Missing data fields in JSON for IMEI {}", imei);
                                }
                            } else {
                                log.warn("⚠️ JSON does not contain IMEI: {}", rawData);
                            }
                        } catch (Exception e) {
                            log.warn("⚠️ Invalid JSON format: {}", e.getMessage());
                        }
                        continue;
                    }

                    // Handle <...> custom packet as normal `imei|v|c|pf|status`
                    if (rawData.startsWith("<") && rawData.endsWith(">")) {
                        rawData = rawData.substring(1, rawData.length() - 1); // remove <>
                    }

                    // Pipe-separated format
                    String[] parts = rawData.split("\\|");
                    if (parts.length == 5) {
                        imei = parts[0];
                        if (!masterTcpSocketRepository.existsByDeviceId(imei)) {
                            log.warn("IMEI {} not registered in masterTcpSockets", imei);
                            continue;
                        }

                        updateConnectionState(imei, socketNo, true);
                        activeConnections.put(clientIp, imei);
                        saveRawOnly(rawData, clientIp, imei, socketNo);
                        processIncomingMessage(imei, parts, socketNo);
                    } else {
                        log.warn("❗ Unrecognized format and unable to parse: {}", rawData);
                    }
                }

            } catch (IOException e) {
                log.error("💥 Connection error from {}: {}", clientIp, e.getMessage());
            } finally {
                if (imei != null) {
                    updateConnectionState(imei, socketNo, false);
                    log.info("❎ IMEI {} disconnected (socket {})", imei, socketNo);
                }
                activeConnections.remove(clientIp);
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

            log.info("✅ Parsed data saved for IMEI {}: voltage={} current={} pf={} status={}",
                    imei, parts[1], parts[2], parts[3], parts[4]);
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

            log.info("✅ JSON parsed & saved for IMEI {}: voltage={} current={} pf={} status={}",
                    imei,
                    json.get("voltage").asDouble(),
                    json.get("current").asDouble(),
                    json.get("powerFactor").asDouble(),
                    json.get("status").asText());
        } catch (Exception e) {
            log.warn("⚠️ Failed to parse JSON values for IMEI {}: {}", imei, e.getMessage());
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

        log.info("📥 Raw message saved for IMEI {}: {}", imei, rawData);
    }

    public int getActiveConnectionCount() {
        return activeConnections.size();
    }
}
