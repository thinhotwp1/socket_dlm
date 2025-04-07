package server.dlm.socket.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.dlm.socket.entity.InData;

public interface InDataRepository extends JpaRepository<InData, Long> {
}