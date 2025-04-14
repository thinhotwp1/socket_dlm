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

    public Whitelist(String imei) {
        this.imei = imei;
    }
}