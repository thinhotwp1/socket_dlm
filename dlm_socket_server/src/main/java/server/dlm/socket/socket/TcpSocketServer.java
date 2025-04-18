
package server.dlm.socket.socket;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import server.dlm.socket.entity.in.InData;
import server.dlm.socket.entity.main.MainData;
import server.dlm.socket.entity.main.Whitelist;
import server.dlm.socket.repository.in.InDataRepository;
import server.dlm.socket.repository.main.MainDataRepository;
import server.dlm.socket.repository.main.WhitelistRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Log4j2
public class TcpSocketServer {
    @Value("${socket.server.port}")
    private int portSocketServer;

    private final HashMap<String, String> activeConnections = new HashMap<>();

    @Autowired
    private InDataRepository inDataRepository;
    @Autowired
    private MainDataRepository mainDataRepository;
    @Autowired
    private WhitelistRepository whitelistRepository;

    private ServerSocket serverSocket;


    @PostConstruct
    public void startServer() {
        new Thread(() -> {
            while (true) {
                try {
                    serverSocket = new ServerSocket(portSocketServer);
                    log.info("✅ TCP Server started on port {}", portSocketServer);

                    while (true) {
                        Socket socket = serverSocket.accept();
                        String clientIp = socket.getInetAddress().getHostAddress();

                        // handle socket connection
                        handleConnection(socket, clientIp);
                    }

                } catch (IOException e) {
                    log.error("❌ Error in TCP Server: {}", e.getMessage(), e);
                }
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
            try (socket;
                 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                // Message sample: 352840051234567|220.5|5.3|0.95|ok
                // Path          : imei|voltage|current|powerFactor|status
                log.info("Client {} connected", clientIp);

                String rawData;
                while ((rawData = reader.readLine()) != null) {
                    log.info("Received from {}: {}", clientIp, rawData);

                    try {
                        String[] parts = rawData.split("\\|");
                        if (parts.length != 5) {
                            log.error("Invalid message format: {}, sample message 'imei|voltage|current|powerFactor|status': 352840051234567|220.5|5.3|0.95|ok", rawData);
                            continue;
                        }

                        String imei = parts[0];
                        if (!whitelistRepository.existsByImei(imei)) {
                            log.error("IMEI {} not in whitelist", imei);
                            continue;
                        }

                        if (connectionValidate(socket, clientIp, imei)) continue;

                        // parse message and save to database
                        messageProcess(imei, rawData, parts, clientIp);

                    } catch (Exception ex) {
                        log.error("Failed to parse or save data from {}: {}", clientIp, rawData, ex);
                    }
                }

            } catch (Exception e) {
                log.error("Connection error with client {}", clientIp, e);
            } finally {
                Whitelist whitelist = whitelistRepository.findByImei(activeConnections.get(clientIp));
                whitelist.setSocketConnected(false);
                whitelistRepository.save(whitelist);
                activeConnections.remove(clientIp);
                log.info("Client {} - {} disconnected", clientIp, activeConnections.get(clientIp));
            }
        }).start();
    }

    private boolean connectionValidate(Socket socket, String clientIp, String imei) {
        activeConnections.put(socket.getInetAddress().getHostAddress(), imei);
        if (activeConnections.containsKey(clientIp) && !activeConnections.get(clientIp).equals(imei)) {
            log.error("Only one imei for one socket connection !");
            return true;
        }

        Whitelist whitelist = whitelistRepository.findByImei(imei);
        whitelist.setSocketConnected(true);
        whitelistRepository.save(whitelist);
        return false;
    }

    private void messageProcess(String imei, String rawData, String[] parts, String clientIp) {
        String socketSessionId = UUID.randomUUID().toString();
        InData inData = new InData();
        inData.setImei(imei);
        inData.setRawData(rawData);
        inData.setIpClient(clientIp);
        inData.setSocketSessionId(socketSessionId);
        inDataRepository.save(inData);

        MainData outData = new MainData();
        outData.setImei(imei);
        outData.setDeviceTimestamp(LocalDateTime.now(ZoneOffset.UTC)); // or extract from device if available
        outData.setVoltage(Double.parseDouble(parts[1]));
        outData.setCurrent(Double.parseDouble(parts[2]));
        outData.setPowerFactor(Double.parseDouble(parts[3]));
        outData.setStatus(parts[4]);
        outData.setSocketSessionId(socketSessionId);
        mainDataRepository.save(outData);

        log.info("Saved data for IMEI {}: {}", imei, outData);
    }

    public int getActiveConnectionCount() {
        return activeConnections.size();
    }
}
