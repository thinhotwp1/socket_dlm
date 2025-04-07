package server.dlm.socket.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "out_data")
@Data
public class OutData {
    /**
     * instruction_type: e.g "update-config", "reset", "sync".
     */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String imei;
    private String instructionType;
    @Column(name = "instruction_content", columnDefinition = "TEXT")
    private String instructionContent;
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}