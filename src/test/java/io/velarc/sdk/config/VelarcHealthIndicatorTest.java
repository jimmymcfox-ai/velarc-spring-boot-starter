package io.velarc.sdk.config;

import io.velarc.sdk.sync.VelarcSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VelarcHealthIndicatorTest {

    private final VelarcSyncService syncService = mock(VelarcSyncService.class);

    @Test
    void reportsDownWhenNotReadyAndRequireSyncTrue() {
        when(syncService.isReady()).thenReturn(false);
        when(syncService.getCurrentVersion()).thenReturn(-1);

        VelarcHealthIndicator indicator = indicator(true);
        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("config_version", -1);
    }

    @Test
    void reportsUpWhenReadyWithConfigVersion() {
        when(syncService.isReady()).thenReturn(true);
        when(syncService.getCurrentVersion()).thenReturn(7);

        VelarcHealthIndicator indicator = indicator(true);
        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("config_version", 7);
    }

    @Test
    void reportsUpWithSyncedFalseWhenNotReadyAndRequireSyncFalse() {
        when(syncService.isReady()).thenReturn(false);
        when(syncService.getCurrentVersion()).thenReturn(-1);

        VelarcHealthIndicator indicator = indicator(false);
        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("synced", false);
        assertThat(health.getDetails()).containsEntry("config_version", -1);
    }

    private VelarcHealthIndicator indicator(boolean requireSync) {
        VelarcProperties props = new VelarcProperties();
        props.getStartup().setRequireSync(requireSync);
        return new VelarcHealthIndicator(syncService, props);
    }
}
