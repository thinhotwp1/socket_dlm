package server.dlm.socket.entity.main;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "whitelist")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Whitelist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String imei;

    private boolean socketConnected = false;

    public Whitelist(String imei) {
        this.imei = imei;
    }

    public Whitelist(String imei, boolean socketConnected) {
        this.imei = imei;
        this.socketConnected = socketConnected;
    }
}