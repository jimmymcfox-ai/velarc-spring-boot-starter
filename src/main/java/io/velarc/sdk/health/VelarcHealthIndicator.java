package io.velarc.sdk.health;

import io.velarc.sdk.config.VelarcProperties;
import io.velarc.sdk.sync.VelarcSyncService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

/**
 * Spring Boot health indicator that reports the readiness of the Velarc SDK.
 *
 * <p>When {@code velarc.startup.require-sync} is {@code true} (the default),
 * the indicator reports {@link org.springframework.boot.actuate.health.Status#DOWN DOWN}
 * until the sync service has successfully loaded configuration. When
 * {@code require-sync} is {@code false}, the indicator always reports
 * {@link org.springframework.boot.actuate.health.Status#UP UP} but includes
 * a {@code synced} detail showing the actual readiness state.
 *
 * <p>The current {@code config_version} is always included in the health details.
 */
public class VelarcHealthIndicator implements HealthIndicator {

    private final VelarcSyncService syncService;
    private final VelarcProperties properties;

    /**
     * Creates a new health indicator.
     *
     * @param syncService the sync service to check readiness against
     * @param properties  Velarc configuration properties
     */
    public VelarcHealthIndicator(VelarcSyncService syncService, VelarcProperties properties) {
        this.syncService = syncService;
        this.properties = properties;
    }

    /**
     * Returns the health status of the Velarc SDK.
     *
     * @return health status with {@code config_version} detail, and optionally a
     *         {@code synced} detail when {@code require-sync} is disabled
     */
    @Override
    public Health health() {
        boolean ready = syncService.isReady();
        int version = syncService.getCurrentVersion();

        if (properties.getStartup().isRequireSync()) {
            Health.Builder builder = ready ? Health.up() : Health.down();
            return builder.withDetail("config_version", version).build();
        }

        return Health.up()
                .withDetail("synced", ready)
                .withDetail("config_version", version)
                .build();
    }
}
