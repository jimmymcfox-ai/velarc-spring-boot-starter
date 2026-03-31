package io.velarc.sdk.sync;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record SdkSyncResponse(
        int version,
        String serverTimestamp,
        List<SdkUseCaseDefinition> useCases,
        List<SdkUserDefinition> users,
        List<SdkBusinessObjectTypeDefinition> businessObjectTypes
) {
}
