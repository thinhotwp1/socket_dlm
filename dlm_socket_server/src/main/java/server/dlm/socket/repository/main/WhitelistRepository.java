package server.dlm.socket.repository.main;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.dlm.socket.entity.main.Whitelist;

@Repository
public interface
WhitelistRepository extends JpaRepository<Whitelist, Long> {
    boolean existsByImei(String imei);
    Whitelist findByImei(String imei);
}