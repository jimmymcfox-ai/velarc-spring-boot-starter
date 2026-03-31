package io.velarc.sdk.sync;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record SdkBusinessObjectTypeDefinition(
        String code,
        String name,
        String idPattern,
        Integer idMinLength,
        Integer idMaxLength,
        String idDescription
) {
}
