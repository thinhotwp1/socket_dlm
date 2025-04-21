package server.dlm.socket.entity.in;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "inBox")
@Data
public class InBox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String deviceId; // IMEI
    private String clientIp; // client ip
    private String socketSessionId;
    @Column(name = "data", columnDefinition = "TEXT")
    private String data;
    @Column(name = "insert_dts")
    private LocalDateTime insertDts;
    @Column(name = "processStatus")
    private String processStatus = "N";

    @PrePersist
    public void prePersist() {
        if (insertDts == null) {
            insertDts = LocalDateTime.now();
        }
    }
}
