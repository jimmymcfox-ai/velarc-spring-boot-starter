package io.velarc.sdk.sync;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record SdkUserDefinition(
        String userKey,
        String displayName,
        String role
) {
}
