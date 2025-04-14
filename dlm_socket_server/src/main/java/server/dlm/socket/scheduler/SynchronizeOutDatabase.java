package server.dlm.socket.scheduler;

import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import server.dlm.socket.repository.out.OutDataRepository;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@Log4j2
public class SynchronizeOutDatabase {

    private int syncSeconds = 30;

    @Autowired
    private OutDataRepository outDataRepository;

    @PostConstruct
    public void synchronizeOutDatabase() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(() -> {
            try {
                // TODO: Implement logic sync data from OUT database

            } catch (Exception e) {
                log.error("Error when synchronize out da" +
                        "tabase: ", e);
            }
        }, 1, syncSeconds, TimeUnit.SECONDS);
    }
}
