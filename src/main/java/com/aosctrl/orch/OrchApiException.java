package com.aosctrl.orch;

public class OrchApiException extends RuntimeException {
  private final int statusCode;
  private final String code;
  private final Object details;
  private final String requestId;
  private final String correlationId;

  public OrchApiException(
      int statusCode,
      String code,
      String message,
      Object details,
      String requestId,
      String correlationId) {
    super(message);
    this.statusCode = statusCode;
    this.code = code;
    this.details = details;
    this.requestId = requestId;
    this.correlationId = correlationId;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public String getCode() {
    return code;
  }

  public Object getDetails() {
    return details;
  }

  public String getRequestId() {
    return requestId;
  }

  public String getCorrelationId() {
    return correlationId;
  }
}
