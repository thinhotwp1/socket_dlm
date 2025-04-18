package server.dlm.socket.repository.out;

import org.springframework.data.jpa.repository.JpaRepository;
import server.dlm.socket.entity.out.OutData;

import java.util.List;

public interface OutDataRepository extends JpaRepository<OutData, Long> {
    List<OutData> findByExecutionStatusAndImeiIn(String executionStatus, List<String> imeiWhiteList);
}