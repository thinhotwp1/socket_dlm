package server.dlm.socket.scheduler;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import server.dlm.socket.entity.main.MasterTcpSocket;
import server.dlm.socket.entity.out.OutBox;
import server.dlm.socket.repository.main.MasterTcpSocketRepository;
import server.dlm.socket.repository.out.OutDataRepository;

import java.util.List;

@Component
@Log4j2
@RequiredArgsConstructor
public class OutCommandSyncScheduler {

    private final OutDataRepository outDataRepository;
    private final MasterTcpSocketRepository masterTcpSocketRepository;

    // every 10 seconds
    @Scheduled(fixedRate = 10000)
    @Transactional
    public void syncOutToMain() {
        log.info("🔁 Checking OUT commands for execution...");
        List<String> imeiWhiteList = masterTcpSocketRepository.findAll().stream().map(MasterTcpSocket::getDeviceId).toList();

        initTestOutDatabase();

        List<OutBox> pendingCommands = outDataRepository.findByExecutionStatusAndImeiIn("WAITING", imeiWhiteList);

        for (OutBox cmd : pendingCommands) {
            String imei = cmd.getDeviceId();

            boolean isConnected = masterTcpSocketRepository.findByDeviceId(imei).getSocketStatus().equals("1");
            log.info("→ Command for IMEI {} | connected: {}", imei, isConnected);

            if (isConnected) {
                // TODO: Add logic send command to device, can use socket to send to device

//                cmd.setExecutionStatus("EXECUTED");
//                cmd.setSocketLive(true);
//                cmd.setExecutedAt(LocalDateTime.now());

//                log.info("✅ Executed command: {} for IMEI {}", cmd.getInstructionType(), imei);
            } else {
//                cmd.setExecutionStatus("FAILED");
//                cmd.setSocketLive(false);
//                log.warn("⚠️ Device {} not connected, failed to execute {}", imei, cmd.getInstructionType());
            }

            outDataRepository.save(cmd);
        }

        log.info("✅ Checking OUT database finished");
    }

    private void initTestOutDatabase() {
        outDataRepository.save(OutBox.builder()
                .deviceId("352840051234567")
                .processStatus("N")
                .build());
    }
}
