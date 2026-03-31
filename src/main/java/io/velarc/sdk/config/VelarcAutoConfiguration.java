package io.velarc.sdk.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.velarc.sdk.client.VelarcClient;
import io.velarc.sdk.exception.VelarcConfigException;
import io.velarc.sdk.health.VelarcHealthIndicator;
import io.velarc.sdk.sync.VelarcSyncScheduler;
import io.velarc.sdk.sync.VelarcSyncService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot auto-configuration for the Velarc SDK.
 *
 * <p>Creates and wires all SDK beans: {@link VelarcSyncService},
 * {@link VelarcSyncScheduler}, {@link VelarcClient}, and optionally
 * {@link VelarcHealthIndicator} when Spring Boot Actuator is on the classpath.
 *
 * <p>Required properties: {@code velarc.endpoint} and {@code velarc.api-key}.
 */
@AutoConfiguration
@EnableConfigurationProperties(VelarcProperties.class)
public class VelarcAutoConfiguration {

    private final VelarcProperties properties;
    private VelarcSyncScheduler syncScheduler;

    /**
     * Creates the auto-configuration with the bound properties.
     *
     * @param properties Velarc configuration properties
     */
    public VelarcAutoConfiguration(VelarcProperties properties) {
        this.properties = properties;
    }

    /**
     * Validates that required properties are configured.
     *
     * @throws VelarcConfigException if endpoint or apiKey is missing
     */
    @PostConstruct
    public void validate() {
        if (properties.getEndpoint() == null || properties.getEndpoint().isBlank()) {
            throw new VelarcConfigException("velarc.endpoint must be configured");
        }
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new VelarcConfigException("velarc.api-key must be configured");
        }
    }

    /**
     * Creates the sync service for fetching and caching SDK configuration.
     *
     * @return the sync service
     */
    @Bean
    public VelarcSyncService velarcSyncService() {
        return new VelarcSyncService(
                properties.getEndpoint(),
                properties.getApiKey(),
                properties.getCache().getPath(),
                velarcObjectMapper());
    }

    /**
     * Creates the sync scheduler, performs startup sync, and begins periodic refresh.
     *
     * @param syncService the sync service
     * @return the sync scheduler
     */
    @Bean
    public VelarcSyncScheduler velarcSyncScheduler(VelarcSyncService syncService) {
        VelarcSyncScheduler scheduler = new VelarcSyncScheduler(
                syncService, properties.getSync().getInterval());
        scheduler.performStartupSync();
        scheduler.start();
        this.syncScheduler = scheduler;
        return scheduler;
    }

    /**
     * Stops the sync scheduler on application shutdown.
     */
    @PreDestroy
    public void shutdown() {
        if (syncScheduler != null) {
            syncScheduler.stop();
        }
    }

    /**
     * Creates the main Velarc client for interacting with the proxy.
     *
     * @param syncService the sync service
     * @return the Velarc client
     */
    @Bean
    public VelarcClient velarcClient(VelarcSyncService syncService) {
        return new VelarcClient(syncService, properties, velarcObjectMapper());
    }

    private ObjectMapper velarcObjectMapper() {
        return new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Health indicator configuration, activated only when Spring Boot Actuator
     * is on the classpath.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(org.springframework.boot.actuate.health.HealthIndicator.class)
    static class HealthIndicatorConfiguration {

        /**
         * Creates the Velarc health indicator.
         *
         * @param syncService the sync service
         * @param properties  Velarc configuration properties
         * @return the health indicator
         */
        @Bean
        public VelarcHealthIndicator velarcHealthIndicator(VelarcSyncService syncService,
                                                           VelarcProperties properties) {
            return new VelarcHealthIndicator(syncService, properties);
        }
    }
}
