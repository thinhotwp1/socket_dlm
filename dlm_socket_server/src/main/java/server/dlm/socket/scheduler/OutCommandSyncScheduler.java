package server.dlm.socket.scheduler;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import server.dlm.socket.entity.main.MainData;
import server.dlm.socket.entity.main.Whitelist;
import server.dlm.socket.entity.out.OutData;
import server.dlm.socket.repository.main.MainDataRepository;
import server.dlm.socket.repository.main.WhitelistRepository;
import server.dlm.socket.repository.out.OutDataRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Log4j2
@RequiredArgsConstructor
public class OutCommandSyncScheduler {

    private final OutDataRepository outDataRepository;
    private final WhitelistRepository whitelistRepository;

    // every 10 seconds
    @Scheduled(fixedRate = 10000)
    @Transactional
    public void syncOutToMain() {
        log.info("🔁 Checking OUT commands for execution...");
        List<String> imeiWhiteList = whitelistRepository.findAll().stream().map(Whitelist::getImei).toList();

        initTestOutDatabase();

        List<OutData> pendingCommands = outDataRepository.findByExecutionStatusAndImeiIn("WAITING", imeiWhiteList);

        for (OutData cmd : pendingCommands) {
            String imei = cmd.getImei();

            boolean isConnected = whitelistRepository.findByImei(imei).isSocketConnected();
            log.info("→ Command for IMEI {} | connected: {}", imei, isConnected);

            if (isConnected) {
                // TODO: Add logic send command to device, can use socket to send to device

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

    private void initTestOutDatabase() {
        outDataRepository.save(OutData.builder()
                .imei("352840051234567")
                .instructionType("OFF")
                .instructionContent("Off device")
                .executionStatus("WAITING")
                .build());
    }
}
