package server.dlm.socket.scheduler;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import server.dlm.socket.entity.main.MainData;
import server.dlm.socket.entity.out.OutData;
import server.dlm.socket.repository.main.MainDataRepository;
import server.dlm.socket.repository.out.OutDataRepository;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Log4j2
@RequiredArgsConstructor
public class OutCommandSyncScheduler {

    private final OutDataRepository outDataRepository;
    private final MainDataRepository mainDataRepository;

    // every 30 seconds
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void syncOutToMain() {
        log.info("🔁 Checking OUT commands for execution...");

        List<OutData> pendingCommands = outDataRepository.findByExecutionStatus("PENDING");

        for (OutData cmd : pendingCommands) {
            String imei = cmd.getImei();

            boolean isConnected = mainDataRepository.existsByImeiAndSocketSessionIdIsNotNull(imei);
            log.info("→ Command for IMEI {} | connected: {}", imei, isConnected);

            if (isConnected) {
                // giả lập gửi lệnh ON/OFF tới thiết bị (sau này có thể mở rộng TCP response)
                cmd.setExecutionStatus("EXECUTED");
                cmd.setSocketLive(true);
                cmd.setExecutedAt(LocalDateTime.now());

                log.info("✅ Executed command: {} for IMEI {}", cmd.getInstructionType(), imei);
            } else {
                cmd.setExecutionStatus("FAILED");
                cmd.setSocketLive(false);
                log.warn("⚠️ Device {} not connected, failed to execute {}", imei, cmd.getInstructionType());
            }

            outDataRepository.save(cmd);
        }

        log.info("✅ Checking OUT database finished");
    }
}
