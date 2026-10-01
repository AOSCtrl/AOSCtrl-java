package com.aosctrl.orch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public final class OrchClient {
  public static final String DEFAULT_BASE_URL = "http://localhost:3001";
  private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final String apiToken;
  private final String baseUrl;
  private final Duration timeout;
  private final HttpClient http;

  public OrchClient(String apiToken) {
    this(apiToken, DEFAULT_BASE_URL, DEFAULT_TIMEOUT);
  }

  public OrchClient(String apiToken, String baseUrl) {
    this(apiToken, baseUrl, DEFAULT_TIMEOUT);
  }

  public OrchClient(String apiToken, String baseUrl, Duration timeout) {
    if (apiToken == null || apiToken.isBlank()) {
      throw new IllegalArgumentException("apiToken is required");
    }
    this.apiToken = apiToken.trim();
    this.baseUrl = baseUrl.replaceAll("/+$", "");
    this.timeout = timeout;
    this.http = HttpClient.newBuilder().connectTimeout(timeout).build();
  }

  public JsonNode sendSms(
      String requestId,
      String phoneNumber,
      String message,
      Map<String, Object> routingContext,
      Boolean unicode,
      String correlationId) {
    ObjectNode body = MAPPER.createObjectNode();
    body.put("requestId", requestId);
    body.put("phoneNumber", phoneNumber);
    body.put("message", message);
    if (routingContext != null) body.set("routingContext", MAPPER.valueToTree(routingContext));
    if (unicode != null) body.put("unicode", unicode);
    return request("POST", "/v1/sms/send", body, correlationId);
  }

  public JsonNode sendOtp(
      String requestId,
      String operation,
      Map<String, Object> payload,
      Map<String, Object> routingContext,
      String correlationId) {
    return service("/v1/otp/send", requestId, operation, payload, routingContext, correlationId);
  }

  public JsonNode sendEmail(
      String requestId,
      String operation,
      Map<String, Object> payload,
      Map<String, Object> routingContext,
      String correlationId) {
    return service("/v1/email/send", requestId, operation, payload, routingContext, correlationId);
  }

  public JsonNode capture(
      String requestId,
      String operation,
      Map<String, Object> payload,
      Map<String, Object> routingContext,
      String correlationId) {
    return service("/v1/capture", requestId, operation, payload, routingContext, correlationId);
  }

  public JsonNode healthLive() {
    return request("GET", "/health/live", null, null);
  }

  public JsonNode healthReady() {
    return request("GET", "/health/ready", null, null);
  }

  private JsonNode service(
      String path,
      String requestId,
      String operation,
      Map<String, Object> payload,
      Map<String, Object> routingContext,
      String correlationId) {
    ObjectNode body = MAPPER.createObjectNode();
    body.put("requestId", requestId);
    if (operation != null) body.put("operation", operation);
    if (payload != null) body.set("payload", MAPPER.valueToTree(payload));
    if (routingContext != null) body.set("routingContext", MAPPER.valueToTree(routingContext));
    return request("POST", path, body, correlationId);
  }

  private JsonNode request(String method, String path, JsonNode body, String correlationId) {
    try {
      HttpRequest.Builder builder = HttpRequest.newBuilder()
          .uri(URI.create(baseUrl + path))
          .timeout(timeout)
          .header("Authorization", "Bearer " + apiToken)
          .header("Accept", "application/json")
          .header("User-Agent", "aosctrl-orch-sdk-java/1.0.0");
      if (correlationId != null && !correlationId.isBlank()) {
        builder.header("X-Correlation-Id", correlationId);
      }
      if (body == null) {
        builder.method(method, HttpRequest.BodyPublishers.noBody());
      } else {
        builder.header("Content-Type", "application/json");
        builder.method(method, HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)));
      }

      HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
      String raw = response.body() == null ? "" : response.body();
      JsonNode payload = raw.isBlank() ? MAPPER.createObjectNode() : MAPPER.readTree(raw);
      if (response.statusCode() >= 200 && response.statusCode() < 300) {
        return payload;
      }

      JsonNode error = payload.path("error");
      throw new OrchApiException(
          response.statusCode(),
          error.path("code").asText("HTTP_ERROR"),
          error.path("message").asText("Orchestrator request failed"),
          error.get("details"),
          textOrNull(payload, "requestId"),
          textOrNull(payload, "correlationId"));
    } catch (OrchApiException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new IllegalStateException("Orchestrator request failed", exception);
    }
  }

  private static String textOrNull(JsonNode payload, String field) {
    JsonNode value = payload.get(field);
    return value == null || value.isNull() ? null : value.asText();
  }
}
