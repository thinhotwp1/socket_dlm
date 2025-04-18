package server.dlm.socket.entity.out;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "out_data")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OutData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String imei;

    // e.g., ON, OFF
    private String instructionType;

    @Column(name = "instruction_content", columnDefinition = "TEXT")
    private String instructionContent;

    // e.g., WAITING, EXECUTED, FAILED
    private String executionStatus;

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
