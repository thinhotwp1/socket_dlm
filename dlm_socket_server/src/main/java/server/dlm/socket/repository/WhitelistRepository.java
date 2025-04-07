package server.dlm.socket.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.dlm.socket.entity.Whitelist;

@Repository
public interface WhitelistRepository extends JpaRepository<Whitelist, Long> {
    boolean existsByImei(String imei);
}