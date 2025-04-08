import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class SocketClient {
    public static void main(String[] args) {
        String serverHost = "localhost";
        int serverPort = 9001;

        try (Socket socket = new Socket(serverHost, serverPort);
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)) {

            String message = "352840051234567|220.5|5.3|0.95|ok";
            writer.println(message);

            System.out.println("Message sent to server: " + message);

        } catch (IOException e) {
            System.err.println("Failed to connect or send message: " + e.getMessage());
        }
    }
}
