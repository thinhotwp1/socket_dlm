package server.dlm.socket.socket;

import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import server.dlm.socket.entity.InData;
import server.dlm.socket.entity.MainData;
import server.dlm.socket.repository.InDataRepository;
import server.dlm.socket.repository.MainDataRepository;
import server.dlm.socket.repository.WhitelistRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Log4j2
public class TcpSocketServer {

    private final Set<String> activeConnections = ConcurrentHashMap.newKeySet();

    @Autowired
    private InDataRepository inDataRepository;
    @Autowired
    private MainDataRepository mainDataRepository;
    @Autowired
    private WhitelistRepository whitelistRepository;

    @PostConstruct
    public void startServer() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(9001)) {
                log.info("TCP Server started on port 9001");
                while (true) {
                    Socket socket = serverSocket.accept();
                    String clientIp = socket.getInetAddress().getHostAddress();
                    activeConnections.add(clientIp);

                    // save to database: connectedAt
                    handleConnection(socket, clientIp);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void handleConnection(Socket socket, String clientIp) {
        new Thread(() -> {
            try (socket;
                 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                log.info("Client {} connected", clientIp);

                String rawData;
                while ((rawData = reader.readLine()) != null) {
                    log.info("Received from {}: {}", clientIp, rawData);

                    try {
                        String[] parts = rawData.split("\\|");
                        if (parts.length != 5) {
                            log.error("Invalid message format: {}", rawData);
                            continue;
                        }

                        String imei = parts[0];
                        if (!whitelistRepository.existsByImei(imei)) {
                            log.error("IMEI {} not in whitelist", imei);
                            continue;
                        }

                        // parse message and save to database
                        messageProcess(imei, rawData, parts);

                    } catch (Exception ex) {
                        log.error("Failed to parse or save data from {}: {}", clientIp, rawData, ex);
                    }
                }

            } catch (Exception e) {
                log.error("Connection error with client {}", clientIp, e);
            } finally {
                activeConnections.remove(clientIp);
                log.info("Client {} disconnected", clientIp);
            }
        }).start();
    }

    private void messageProcess(String imei, String rawData, String[] parts) {
        InData inData = new InData();
        inData.setImei(imei);
        inData.setRawData(rawData);
        inDataRepository.save(inData);

        MainData data = new MainData();
        data.setImei(imei);
        data.setDeviceTimestamp(LocalDateTime.now(ZoneOffset.UTC)); // or extract from device if available
        data.setVoltage(Double.parseDouble(parts[1]));
        data.setCurrent(Double.parseDouble(parts[2]));
        data.setPowerFactor(Double.parseDouble(parts[3]));
        data.setStatus(parts[4]);

        mainDataRepository.save(data);

        log.info("Saved data for IMEI {}: {}", imei, data);
    }

    public int getActiveConnectionCount() {
        return activeConnections.size();
    }
}
