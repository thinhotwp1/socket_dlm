package server.dlm.socket.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import server.dlm.socket.dto.ResponseData;
import server.dlm.socket.socket.TcpSocketServer;

@RestController("/dashboard")
public class DashboardController {

    @Autowired
    TcpSocketServer tcpSocketServer;

    @GetMapping("/get-socket-connection")
    public ResponseData<?> getSocketConnectionCount() {
        return new ResponseData<>().success(tcpSocketServer.getActiveConnectionCount());
    }
}
