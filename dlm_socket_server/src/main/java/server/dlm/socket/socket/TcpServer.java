package server.dlm.socket.socket;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TcpServer {

    private final Set<String> activeConnections = ConcurrentHashMap.newKeySet();

    @PostConstruct
    public void startServer() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(9001)) {
                System.out.println("TCP Server started on port 9001");
                while (true) {
                    Socket socket = serverSocket.accept();
                    String clientIp = socket.getInetAddress().getHostAddress();
                    activeConnections.add(clientIp);

                    // save to database: connectedAt
                    handleConnection(socket, clientIp);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void handleConnection(Socket socket, String clientIp) {
        new Thread(() -> {
            try (socket) {
                // Input/Output stream handling here
                Thread.sleep(10000); // simulate
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                activeConnections.remove(clientIp);
                // save to database: disconnectedAt
            }
        }).start();
    }

    public int getActiveConnectionCount() {
        return activeConnections.size();
    }
}
