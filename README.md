# velarc-spring-boot-starter

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Spring Boot starter for the [Velarc](https://velarc.io) AI governance proxy. Captures every AI interaction with structured business context for EU AI Act compliance.

## Quick Start

Add the dependency:

```xml
<dependency>
    <groupId>io.velarc</groupId>
    <artifactId>velarc-spring-boot-starter</artifactId>
    <version>0.1.0</version>
</dependency>
```

Configure in `application.yml`:

```yaml
velarc:
  endpoint: https://api.velarc.io
  api-key: ${VELARC_API_KEY}
  provider-api-keys:
    openai: ${OPENAI_API_KEY}
    anthropic: ${ANTHROPIC_API_KEY}
```

Inject and use:

```java
@Service
public class MyService {

    private final VelarcClient velarc;

    public MyService(VelarcClient velarc) {
        this.velarc = velarc;
    }

    public String summarise(String text) throws VelarcServiceException, VelarcProviderException {
        VelarcResponse response = velarc.chat("summarise", "dr-smith", text);
        return response.responseText();
    }
}
```

That's it. Provider, model, and system prompt are resolved from your use case configuration on the Velarc server.

## Features

- Automatic provider and model resolution from server-side use case definitions
- Structured business context (use case, user, business object) on every AI call
- Local configuration cache with periodic sync
- Spring Boot health indicator (with Actuator)
- Fail-closed startup by default — the app will not pass readiness checks until configuration is synced
- Multi-turn conversations via message arrays
- Provider-specific parameter passthrough
- Call-site overrides for provider, model, and API key when needed

## Advanced Usage

With business object context:

```java
VelarcResponse response = velarc.chat(
    "summarise", "dr-smith",
    BusinessObject.of("referral", "REF-2024-001"),
    "Summarise this referral");
```

Multi-turn conversation:

```java
VelarcResponse response = velarc.chat("clinical-qa", "dr-smith", List.of(
    Message.user("What are the symptoms?"),
    Message.assistant("The patient presents with..."),
    Message.user("What tests would you recommend?")));
```

Full builder for edge cases:

```java
VelarcResponse response = velarc.chat(VelarcRequest.builder()
    .useCase("summarise")
    .user("dr-smith")
    .businessObject("referral", "REF-2024-001")
    .provider("anthropic")
    .model("claude-sonnet-4-20250514")
    .providerParams(Map.of("temperature", 0.3))
    .messages(List.of(Message.user("Summarise this referral")))
    .build());
```

## Configuration Reference

| Property | Default | Description |
|---|---|---|
| `velarc.endpoint` | *(required)* | Velarc server URL |
| `velarc.api-key` | *(required)* | Tenant API key |
| `velarc.provider-api-keys.{name}` | | Provider API keys by provider name |
| `velarc.startup.require-sync` | `true` | Block readiness until config synced |
| `velarc.startup.sync-timeout` | `30s` | Timeout for startup sync |
| `velarc.cache.path` | `./velarc-cache.json` | Local cache file path |
| `velarc.sync.interval` | `60s` | Background sync interval |

## Error Handling

The SDK uses a four-class exception hierarchy:

- **VelarcConfigException** (unchecked) — missing configuration, invalid API key. Fix your config.
- **VelarcGovernanceException** (unchecked) — unknown use case, user, or business object. Fix your code.
- **VelarcServiceException** (checked) — Velarc unreachable. Retry or degrade gracefully.
- **VelarcProviderException** (checked) — AI provider error. Retry or return a fallback response.

```java
try {
    VelarcResponse response = velarc.chat("summarise", "dr-smith", prompt);
} catch (VelarcProviderException e) {
    log.warn("Provider {} failed: {}", e.getProviderName(), e.getProviderMessage());
    return fallbackResponse();
} catch (VelarcServiceException e) {
    log.error("Velarc unreachable", e);
    return fallbackResponse();
}
```

## Requirements

- Java 17+
- Spring Boot 3.x

## License

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for details.
