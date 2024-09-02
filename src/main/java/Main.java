import io.java.ClientHandler;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
  public static void main(String[] args) {

   ServerSocket serverSocket;
   Socket clientSocket;

     try {
       serverSocket = new ServerSocket(4221);
       // to avoid 'Address already in use' errors
       serverSocket.setReuseAddress(true);

       ExecutorService executor = Executors.newFixedThreadPool(10);

       while (true) {
         clientSocket = serverSocket.accept(); // Wait for connection from client.
         executor.submit(new ClientHandler(args, clientSocket));
       }
     } catch (IOException e) {
       System.out.println("IOException: " + e.getMessage());
     }
  }
}
