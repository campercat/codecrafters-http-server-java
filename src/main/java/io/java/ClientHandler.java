package io.java;

import static java.util.Objects.requireNonNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

public class ClientHandler implements Runnable {

  String[] args;
  Socket clientSocket;
  RequestModel requestModel;

  public ClientHandler(String[] args, Socket clientSocket) {
    this.args = args;
    this.clientSocket = clientSocket;
  }

  @Override
  public void run() {
    try {
      InputStream inputStream = clientSocket.getInputStream();
      this.requestModel = parseRequest(inputStream);
      if (requestModel.getHttpMethod().equals("GET")) {
        handleGet();
      } else if (requestModel.getHttpMethod().equals("POST")) {
        handlePost(inputStream);
      } else {
        writeUnsuccessfulOutput();
      }
    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    }
  }

  private RequestModel parseRequest(InputStream inputStream) throws IOException {
    String[] tokens = readLine(inputStream).split(" ");

    String httpMethod = requireNonNull(tokens[0]);
    String requestTarget = requireNonNull(tokens[1]).toLowerCase();
    String version = requireNonNull(tokens[2]);

    Map<String, String> headers = parseHeaders(inputStream);

    return new RequestModel(httpMethod, requestTarget, version, headers);
  }

  private Map<String, String> parseHeaders(InputStream inputStream) throws IOException {
    Map<String, String> headers = new HashMap<>();
    String header;
    while (!(header = readLine(inputStream)).equals("\r\n")) {
      if (header.isEmpty()) {
        break;
      }
      String[] tokens = header.split(": ");
      headers.put(tokens[0].toLowerCase(), tokens[1]);
    }
    return headers;
  }

  private static String readLine(InputStream inputStream) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    int prev = -1, curr;
    while ((curr = inputStream.read()) != -1) {
      buffer.write(curr);
      if (prev == '\r' && curr == '\n') {
        break;
      }
      prev = curr;
    }
    return buffer.toString(StandardCharsets.UTF_8).trim();
  }

  private void parseRequestBody(InputStream inputStream, String filename) throws IOException {
    String directoryPath = requireNonNull(extractArg(args, "--directory"));

    File file = new File(directoryPath, filename);
    try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
      int len = Integer.parseInt(requestModel.getHeaders().get("content-length"));
      fileOutputStream.write(inputStream.readNBytes(len));
    }
  }

  private void handleGet()
      throws IOException {
    String requestTarget = requestModel.getRequestTarget();
    Map<String, String> headers = requestModel.getHeaders();

    if (requestTarget.matches("/")) {
      writeSuccessOutput();
    } else if (requestTarget.matches("/echo/.*")) {
      String echoPhrase = requestTarget.substring("/echo/".length());
      processRequestWithEncoding(echoPhrase);
      writeSuccessOutput(
          echoPhrase.length(),
          ContentType.TEXT_PLAIN,
          echoPhrase);
    } else if (requestTarget.matches("/user-agent")) {
      final String USER_AGENT = "user-agent";
      writeSuccessOutput(
          headers.get(USER_AGENT).length(),
          ContentType.TEXT_PLAIN,
          headers.get(USER_AGENT)
      );
    } else if (requestTarget.matches("/files/.*")) {
      String filename = requestTarget.substring("/files/".length());
      String directory = extractArg(args, "--directory");
      File file = new File(requireNonNull(directory), filename);

      returnFileIfFound(file);
    } else {
      writeUnsuccessfulOutput();
    }
  }

  private void handlePost(InputStream inputStream)
      throws IOException {
    String requestTarget = requestModel.getRequestTarget();

    if (requestTarget.matches("/files/.*")) {
      String filename = requestTarget.substring("/files/".length());
      parseRequestBody(inputStream, filename);
      clientSocket.getOutputStream()
          .write(String.format("%s 201 Created\r\n\r\n", requestModel.getHttpVersion()).getBytes());
    } else {
      writeUnsuccessfulOutput();
    }
  }

  private void processRequestWithEncoding(String echoPhrase) throws IOException {
    Map<String, String> headers = requestModel.getHeaders();
    if (headers.containsKey("accept-encoding")) {
      String acceptedEncodingSchemes = headers.get("accept-encoding");
      if (acceptedEncodingSchemes.contains("gzip")) {
        byte[] compressedData = gzipCompress(echoPhrase);
        writeSuccessOutput(ContentType.TEXT_PLAIN, "gzip", compressedData);
      }
    }
  }

  private void writeSuccessOutput()
      throws IOException {
    clientSocket.getOutputStream().write(String.format(
            "%s 200 OK\r\n\r\n",
            requestModel.getHttpVersion()
        )
        .getBytes());
  }

  private void writeSuccessOutput(String contentType, String contentEncoding, byte[] body)
      throws IOException {
    clientSocket.getOutputStream().write(String.format(
            "%s 200 OK\r\nContent-Type: %s\r\nContent-Encoding: %s\r\nContent-Length: %d\r\n\r\n",
            requestModel.getHttpVersion(),
            contentType,
            contentEncoding,
            body.length)
        .getBytes());
    clientSocket.getOutputStream().write(body);
  }

  private void writeSuccessOutput(Integer contentLength, String contentType,
      String body)
      throws IOException {
    clientSocket.getOutputStream().write(String.format(
            "%s 200 OK\r\nContent-Type: %s\r\nContent-Length: %d\r\n\r\n%s",
            requestModel.getHttpVersion(),
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

  private void returnFileIfFound(File file) throws IOException {
    if (file.exists()) {
      byte[] fileContent = Files.readAllBytes(file.toPath());
      writeSuccessOutput(fileContent.length, ContentType.APPLICATION_OCTET_STREAM,
          new String(fileContent));
    } else {
      writeUnsuccessfulOutput();
    }
  }

  private byte[] gzipCompress(String data) throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)) {
      gzipOutputStream.write(data.getBytes(StandardCharsets.UTF_8));
    }
    return byteArrayOutputStream.toByteArray();
  }

  private static class ContentType {

    static final String TEXT_PLAIN = "text/plain";
    static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
  }
}
