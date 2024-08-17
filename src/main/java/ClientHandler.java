import static java.util.Objects.requireNonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class ClientHandler implements Runnable {

  Socket clientSocket;
  String[] args;

  public ClientHandler(String[] args, Socket clientSocket) {
    this.args = args;
    this.clientSocket = clientSocket;
  }

  @Override
  public void run() {
    try {
      BufferedReader reader = new BufferedReader(
          new InputStreamReader(clientSocket.getInputStream()));
      parseRequestLine(reader);
    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    }
  }

  private void parseRequestLine(BufferedReader reader) throws IOException {

    String[] tokens = reader.readLine().split(" ");
    String httpMethod = requireNonNull(tokens[0]);
    String requestTarget = requireNonNull(tokens[1]).toLowerCase();
    String version = requireNonNull(tokens[2]);

    Map<String, String> headers = parseHeaders(reader);

    if (httpMethod.equals("GET")) {
      handleGet(requestTarget, version, headers);
    } else {
      writeUnsuccessfulOutput();
    }
  }

  private Map<String, String> parseHeaders(BufferedReader reader) throws IOException {
    Map<String, String> headers = new HashMap<>();
    String header;
    while (!(header = reader.readLine()).equals("\r\n")) {
      if (header.isEmpty()) {
        break;
      }
      String[] tokens = header.split(": ");
      headers.put(tokens[0].toLowerCase(), tokens[1]);
    }
    return headers;
  }

  private void handleGet(String requestTarget, String version, Map<String, String> headers)
      throws IOException {
    if (requestTarget.matches("/")) {
      writeSuccessOutput(version);
    } else if (requestTarget.matches("/echo/.*")) {
      String echoPhrase = requestTarget.substring("/echo/".length());
      writeSuccessOutput(version, echoPhrase.length(), ContentType.TEXT_PLAIN, echoPhrase);
    } else if (requestTarget.matches("/user-agent")) {
      final String USER_AGENT = "user-agent";
      writeSuccessOutput(version,
          headers.get(USER_AGENT).length(),
          ContentType.TEXT_PLAIN,
          headers.get(USER_AGENT)
      );
    } else if (requestTarget.matches("/files/.*")) {
      String fileName = requestTarget.substring("/files/".length());
      String directory = extractArg(args, "--directory");
      File file = new File(requireNonNull(directory), fileName);

      returnFileIfFound(file, version);
    } else {
      writeUnsuccessfulOutput();
    }
  }

  private void writeSuccessOutput(String version)
      throws IOException {
    clientSocket.getOutputStream().write(String.format("%s 200 OK\r\n\r\n", version).getBytes());
  }

  private void writeSuccessOutput(String version, Integer contentLength, String contentType, String body)
      throws IOException {
    clientSocket.getOutputStream().write(String.format(
            "%s 200 OK\r\nContent-Type: %s\r\nContent-Length: %d\r\n\r\n%s",
            version,
            contentType,
            contentLength,
            body)
        .getBytes());
  }

  private void writeUnsuccessfulOutput() throws IOException {
    clientSocket.getOutputStream().write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
  }

  private String extractArg(String[] args, String arg) {
    for (int i = 0; i < args.length; i++) {
      if (arg.equals(args[i]) && i + 1 < args.length) {
        return args[i + 1];
      }
    }
    return null;
  }

  private void returnFileIfFound(File file, String version) throws IOException {
    if (file.exists()) {
      byte[] fileContent = Files.readAllBytes(file.toPath());
      writeSuccessOutput(version, fileContent.length, ContentType.APPLICATION_OCTET_STREAM, new String(fileContent));
    } else {
      writeUnsuccessfulOutput();
    }
  }

  private static class ContentType {
    static final String TEXT_PLAIN = "text/plain";
    static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
  }
}
