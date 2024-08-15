import static java.util.Objects.requireNonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ClientHandler implements Runnable {
  private final String USER_AGENT = "User-Agent";
  Socket clientSocket;

  public ClientHandler(Socket clientSocket) {
    this.clientSocket = clientSocket;
  }

  @Override
  public void run() {
    // Map<String, String> headers = new HashMap<>();

    // 1. handle /files endpoint, it should extract the file path after /files/
    // 2. implement opening and reading file at path
    // 3. if file exists, return 200 with the content of file in response body
    // 4. if files does not exist, return 404

    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      parseRequestLine(reader);
    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    }
  }

  private void parseRequestLine(BufferedReader reader) throws IOException {

    String[] tokens = reader.readLine().split(" ");
    String httpMethod = requireNonNull(tokens[0]);
    String requestTarget = requireNonNull(tokens[1]);
    String version = requireNonNull(tokens[2]);

    Map<String, String> headers = parseHeaders(reader);

    switch(httpMethod) {
      case "GET":
        handleGet(requestTarget, version, headers);
        return;
      default:
        clientSocket.getOutputStream().write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
    }
  }

  private Map<String, String> parseHeaders(BufferedReader reader) throws IOException {
    Map<String, String> headers = new HashMap<>();
    String header;
    // read headers into a map
    while(!(header = reader.readLine()).equals("\r\n")) {
      if(header.isEmpty()) break;
      String[] tokens = header.split(": ");
      headers.put(tokens[0].toLowerCase(), tokens[1]);
    }
    clientSocket.getOutputStream().write(String.format(
            "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: %d\r\n\r\n%s",
            headers.get(USER_AGENT.toLowerCase()).length(),
            headers.get(USER_AGENT.toLowerCase()))
        .getBytes());
    return headers;
  }

  private void handleGet(String requestTarget, String version, Map<String, String> headers) throws IOException {
    switch(requestTarget) {
      case "/":
        clientSocket.getOutputStream().write(String.format("%s 200 OK\r\n\r\n", version).getBytes());
        return;
      case "/echo/*":
        String echoPhrase = requestTarget.substring("/echo/".length());
        writeOutput(version, echoPhrase.length(), echoPhrase);
      case "user-agent":
        writeOutput(version,
            headers.get(USER_AGENT.toLowerCase()).length(),
            headers.get(USER_AGENT.toLowerCase())
        );
    }
  }

  private void writeOutput(String version, int contentLength, String body) throws IOException {
    clientSocket.getOutputStream().write(String.format(
            "%s 200 OK\r\nContent-Type: text/plain\r\nContent-Length: %d\r\n\r\n%s",
            version,
            contentLength,
            body)
        .getBytes());
  }
}
