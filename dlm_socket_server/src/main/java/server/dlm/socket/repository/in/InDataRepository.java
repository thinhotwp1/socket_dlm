package server.dlm.socket.repository.in;

import org.springframework.data.jpa.repository.JpaRepository;
import server.dlm.socket.entity.in.InData;

public interface InDataRepository extends JpaRepository<InData, Long> {
}