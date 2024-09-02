package io.java;

import java.util.Map;

public class RequestModel {

  // Request Line
  String httpMethod;
  String requestTarget;
  String httpVersion;

  Map<String, String> headers;

  public RequestModel(String httpMethod, String requestTarget, String httpVersion,
      Map<String, String> headers) {
    this.httpMethod = httpMethod;
    this.requestTarget = requestTarget;
    this.httpVersion = httpVersion;
    this.headers = headers;
  }


  public String getHttpMethod() {
    return httpMethod;
  }

  public void setHttpMethod(String httpMethod) {
    this.httpMethod = httpMethod;
  }

  public String getRequestTarget() {
    return requestTarget;
  }

  public void setRequestTarget(String requestTarget) {
    this.requestTarget = requestTarget;
  }

  public String getHttpVersion() {
    return httpVersion;
  }

  public void setHttpVersion(String httpVersion) {
    this.httpVersion = httpVersion;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public void setHeaders(Map<String, String> headers) {
    this.headers = headers;
  }
}
