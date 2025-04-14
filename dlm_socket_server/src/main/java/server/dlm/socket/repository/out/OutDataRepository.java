package server.dlm.socket.repository.out;

import org.springframework.data.jpa.repository.JpaRepository;
import server.dlm.socket.entity.out.OutData;

public interface OutDataRepository extends JpaRepository<OutData, Long> {
}