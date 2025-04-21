package server.dlm.socket.repository.main;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.dlm.socket.entity.main.MasterTcpSocket;

@Repository
public interface
MasterTcpSocketRepository extends JpaRepository<MasterTcpSocket, Long> {
    boolean existsByDeviceId(String imei);
    MasterTcpSocket findByDeviceId(String imei);
}