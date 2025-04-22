package server.dlm.socket.scheduler;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import server.dlm.socket.enums.ConnectStatusSocket;
import server.dlm.socket.enums.ProcessStatus;
import server.dlm.socket.entity.main.MasterTcpSocket;
import server.dlm.socket.entity.out.OutBox;
import server.dlm.socket.repository.main.MasterTcpSocketRepository;
import server.dlm.socket.repository.out.OutBoxRepository;
import server.dlm.socket.socket.TcpSocketServer;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Log4j2
@RequiredArgsConstructor
public class OutCommandSyncScheduler {

    @Autowired
    private TcpSocketServer tcpSocketServer;
    @Autowired
    private OutBoxRepository outBoxRepository;
    @Autowired
    private MasterTcpSocketRepository masterSocketRepository;
    @Autowired
    private MasterTcpSocketRepository masterTcpSocketRepository;

    @Scheduled(fixedRate = 60000) // Every 60 seconds
    @Transactional
    public void syncOutToMain() {
//        initTestOutDatabase();
        log.info("🔁 Checking OUT commands for execution...");

        List<String> imeiValidList = masterSocketRepository.findAll().stream().map(MasterTcpSocket::getDeviceId).toList();
        List<OutBox> pendingCommands = outBoxRepository.findByProcessStatusAndDeviceIdIn(ProcessStatus.NONE.getValue(), imeiValidList);

        for (OutBox cmd : pendingCommands) {
            String imei = cmd.getDeviceId();
            MasterTcpSocket socketEntry = masterTcpSocketRepository.findByDeviceId(imei);

            boolean isConnected = ConnectStatusSocket.CONNECTED.getValue().equals(socketEntry.getSocketStatus());
            log.info("→ Command for IMEI {} | connected: {}", imei, isConnected);

            if (isConnected) {
                // TODO: Send `cmd.getData()` to the live socket
                tcpSocketServer.sendMessageToClient(imei, cmd.getData());

                cmd.setProcessStatus(ProcessStatus.SUCCESS.getValue());
                cmd.setSentDts(LocalDateTime.now());
                log.info("✅ Executed command for IMEI {}: {}", imei, cmd.getData());
            } else {
                log.warn("⚠️ IMEI {} not connected. Will retry later...", imei);
            }

            outBoxRepository.save(cmd);
        }

        log.info("✅ Finished checking OUT commands");
    }

    // Optional: test data init
    private void initTestOutDatabase() {
        outBoxRepository.save(OutBox.builder()
                .deviceId("352840051234567")
                .data("TURN_OFF")
                .insertDts(LocalDateTime.now())
                .processStatus("N")
                .build());
    }
}
