package server.dlm.socket.repository.out;

import org.springframework.data.jpa.repository.JpaRepository;
import server.dlm.socket.entity.out.OutBox;

import java.util.Collection;
import java.util.List;

public interface OutBoxRepository extends JpaRepository<OutBox, Long> {
    List<OutBox> findByProcessStatusAndDeviceIdIn(String processStatus, Collection<String> deviceIds);
}