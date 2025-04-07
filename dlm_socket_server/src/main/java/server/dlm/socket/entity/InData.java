package server.dlm.socket.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "in_data")
@Data
public class InData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String imei;
    @Column(name = "raw_data", columnDefinition = "TEXT")
    private String rawData;
    private LocalDateTime receivedAt;
    private Boolean isValid = true;

    @PrePersist
    public void prePersist() {
        if (receivedAt == null) {
            receivedAt = LocalDateTime.now();
        }
    }
}