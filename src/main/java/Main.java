import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Main {
  public static void main(String[] args) {
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    System.out.println("Logs from your program will appear here!");

     ServerSocket serverSocket = null;
     Socket clientSocket = null;
     Map<String, String> headers = new HashMap<>();
     final String USER_AGENT = "User-Agent";

     try {
       serverSocket = new ServerSocket(4221);
       // Since the tester restarts your program quite often, setting SO_REUSEADDR
       // ensures that we don't run into 'Address already in use' errors
       serverSocket.setReuseAddress(true);
       clientSocket = serverSocket.accept(); // Wait for connection from client.
       BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
       String line;
       while((line = reader.readLine()) != null) {
         System.out.println(line);
         if (line.contains("GET") && line.contains(" / ")) {
           clientSocket.getOutputStream().write("HTTP/1.1 200 OK\r\n\r\n".getBytes());
         } else if (line.contains("GET") && line.contains("echo")) {
           int start = line.indexOf("/echo/") + "/echo/".length();
           int end = line.indexOf(" ", start);
           String body = line.substring(start, end);
           clientSocket.getOutputStream().write(String.format(
               "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: %d\r\n\r\n%s",
               body.length(),
               body)
               .getBytes());
         } else if (line.contains("GET") && line.toLowerCase().contains(USER_AGENT.toLowerCase())) {
           // read headers into a map
           while(!(line = reader.readLine()).equals("\r\n")) {
             if(line.isEmpty()) break;
             String[] tokens = line.split(": ");
             headers.put(tokens[0].toLowerCase(), tokens[1]);
           }
           clientSocket.getOutputStream().write(String.format(
                   "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: %d\r\n\r\n%s",
                   headers.get(USER_AGENT.toLowerCase()).length(),
                   headers.get(USER_AGENT.toLowerCase()))
               .getBytes());
         }
         else if (line.contains("GET")){
           clientSocket.getOutputStream().write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
         }
       }
     } catch (IOException e) {
       System.out.println("IOException: " + e.getMessage());
     }
  }
}
