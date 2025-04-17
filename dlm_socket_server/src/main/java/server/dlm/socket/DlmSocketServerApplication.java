package server.dlm.socket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DlmSocketServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DlmSocketServerApplication.class, args);
    }

}
