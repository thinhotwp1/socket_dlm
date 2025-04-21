package server.dlm.socket.entity.out;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "outBox")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OutBox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recID")
    private Long recId;

    @Column(name = "deviceID", length = 20)
    private String deviceId; // IMEI

    @Column(name = "data", columnDefinition = "TEXT")
    private String data;

    @Column(name = "processStatus", length = 1)
    private String processStatus = "N"; // 'N' = pending, 'S' = sent

    @Column(name = "insert_dts")
    private LocalDateTime insertDts;

    @Column(name = "sent_dts")
    private LocalDateTime sentDts;

    @PrePersist
    public void prePersist() {
        if (insertDts == null) {
            insertDts = LocalDateTime.now();
        }
    }
}
