import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ClientHandler implements Runnable {
  Socket clientSocket;

  public ClientHandler(Socket clientSocket) {
    this.clientSocket = clientSocket;
  }

  @Override
  public void run() {
    Map<String, String> headers = new HashMap<>();
    final String USER_AGENT = "User-Agent";

    try {
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
