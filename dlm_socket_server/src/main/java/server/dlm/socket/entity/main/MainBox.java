package server.dlm.socket.entity.main;

import jakarta.persistence.*;
        import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "main_box")
@Data
public class MainBox {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String imei;
    private String socketSessionId;
    private Double voltage;
    private Double current;
    private Double powerFactor;
    private String status;
    private LocalDateTime deviceTimestamp;
    private LocalDateTime receivedAt;

    @PrePersist
    public void prePersist() {
        if (receivedAt == null) {
            receivedAt = LocalDateTime.now();
        }
    }
}