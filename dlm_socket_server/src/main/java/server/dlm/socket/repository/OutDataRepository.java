package server.dlm.socket.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.dlm.socket.entity.OutData;

public interface OutDataRepository extends JpaRepository<OutData, Long> {
}