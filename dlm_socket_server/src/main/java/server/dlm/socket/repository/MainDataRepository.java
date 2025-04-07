package server.dlm.socket.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.dlm.socket.entity.MainData;

public interface MainDataRepository extends JpaRepository<MainData, Long> {
}