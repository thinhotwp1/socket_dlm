package server.dlm.socket.repository.main;

import org.springframework.data.jpa.repository.JpaRepository;
import server.dlm.socket.entity.main.MainData;

public interface MainDataRepository extends JpaRepository<MainData, Long> {
}