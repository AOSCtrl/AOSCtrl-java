# Java SDK

Requires Java 17. Auth is the orchestrator API token.

```xml
<dependency>
  <groupId>com.aosctrl</groupId>
  <artifactId>orch-sdk</artifactId>
  <version>1.0.0</version>
</dependency>
```

Install from this repo with `mvn -f SDK/java/pom.xml install`.

```java
import com.aosctrl.orch.OrchClient;
import java.util.Map;

OrchClient client = new OrchClient(System.getenv("API_TOKEN"), "http://localhost:3001");

client.sendSms(
    "sms-demo-001",
    "+919999999999",
    "Your OTP is 123456",
    Map.of("country", "IN", "requestType", "OTP", "capabilities", java.util.List.of("SMS")),
    null,
    "corr-demo-001");

client.sendEmail(
    "email-demo-001",
    "SEND",
    Map.of("recipient", "user@example.com", "subject", "Test message"),
    Map.of("tenantId", "tenant-123"),
    null);

client.sendOtp("otp-demo-001", null, Map.of("phoneNumber", "+919999999999"), null, null);
client.capture("capture-demo-001", null, Map.of("source", "web"), null, null);
client.healthLive();
client.healthReady();
```

Every call sends `Authorization: Bearer <apiToken>` so APISIX can validate it. A non-success response throws `OrchApiException` with `statusCode` and `code`. A blank token throws `IllegalArgumentException` before any HTTP call.

Pass `null` for optional `operation`, `payload`, `routingContext`, `unicode`, or `correlationId`.
