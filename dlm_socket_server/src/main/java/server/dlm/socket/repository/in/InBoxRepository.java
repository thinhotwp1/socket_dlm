package server.dlm.socket.repository.in;

import org.springframework.data.jpa.repository.JpaRepository;
import server.dlm.socket.entity.in.InBox;

public interface InBoxRepository extends JpaRepository<InBox, Long> {
}