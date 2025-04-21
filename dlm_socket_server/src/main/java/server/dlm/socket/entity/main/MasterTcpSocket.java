package server.dlm.socket.entity.main;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "master_tcp_socket")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class MasterTcpSocket {

    @Id
    @Column(name = "deviceID", length = 20)
    private String deviceId; // IMEI

    @Column(name = "socketNo")
    private String socketNo; // UUID Socket

    @Column(name = "data", length = 4096)
    private String data; // optional (nếu bạn lưu gì đó)

    @Column(name = "socketStatus", length = 1, nullable = false)
    private String socketStatus = "0"; // "1" = connected, "0" = disconnected

    @Column(name = "sys_dts", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime sysDts;

    public MasterTcpSocket(String deviceId) {
        this.deviceId = deviceId;
    }
}