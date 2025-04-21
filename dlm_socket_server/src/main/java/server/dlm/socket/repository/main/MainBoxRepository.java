package server.dlm.socket.repository.main;

import org.springframework.data.jpa.repository.JpaRepository;
import server.dlm.socket.entity.main.MainBox;

public interface MainBoxRepository extends JpaRepository<MainBox, Long> {
}