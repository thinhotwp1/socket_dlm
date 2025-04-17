package server.dlm.socket.entity.out;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "out_data")
@Data
public class OutData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String imei;

    // e.g., ON, OFF
    private String instructionType;

    @Column(name = "instruction_content", columnDefinition = "TEXT")
    private String instructionContent;

    // e.g., PENDING, EXECUTED, FAILED
    private String executionStatus;

    // Track if socket is live or not at execution time
    private Boolean socketLive;

    private LocalDateTime createdAt;
    private LocalDateTime executedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
