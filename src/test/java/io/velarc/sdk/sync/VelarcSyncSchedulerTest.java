package io.velarc.sdk.sync;

import io.velarc.sdk.exception.VelarcConfigException;
import io.velarc.sdk.exception.VelarcServiceException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VelarcSyncSchedulerTest {

    private final VelarcSyncService syncService = mock(VelarcSyncService.class);

    @Test
    void performStartupSyncReturnsTrueOnSuccess() throws Exception {
        when(syncService.isReady()).thenReturn(true);

        VelarcSyncScheduler scheduler = new VelarcSyncScheduler(syncService, Duration.ofSeconds(60));

        assertThat(scheduler.performStartupSync()).isTrue();
        verify(syncService).sync();
    }

    @Test
    void performStartupSyncFallsToDiskCacheWhenSyncFails() throws Exception {
        doThrow(new VelarcServiceException("timeout", new Exception()))
                .when(syncService).sync();
        when(syncService.isReady()).thenReturn(true);

        VelarcSyncScheduler scheduler = new VelarcSyncScheduler(syncService, Duration.ofSeconds(60));

        assertThat(scheduler.performStartupSync()).isTrue();
        verify(syncService).loadFromDisk();
    }

    @Test
    void performStartupSyncReturnsFalseWhenBothFail() throws Exception {
        doThrow(new VelarcServiceException("timeout", new Exception()))
                .when(syncService).sync();
        when(syncService.loadFromDisk()).thenReturn(false);
        when(syncService.isReady()).thenReturn(false);

        VelarcSyncScheduler scheduler = new VelarcSyncScheduler(syncService, Duration.ofSeconds(60));

        assertThat(scheduler.performStartupSync()).isFalse();
    }

    @Test
    void performStartupSyncFallsToDiskCacheWhenConfigException() throws Exception {
        doThrow(new VelarcConfigException("Invalid or expired API key (HTTP 401)"))
                .when(syncService).sync();
        when(syncService.isReady()).thenReturn(true);

        VelarcSyncScheduler scheduler = new VelarcSyncScheduler(syncService, Duration.ofSeconds(60));

        assertThat(scheduler.performStartupSync()).isTrue();
        verify(syncService).loadFromDisk();
    }

    @Test
    void periodicRefreshSurvivesConfigException() throws Exception {
        doThrow(new VelarcConfigException("Invalid or expired API key (HTTP 401)"))
                .when(syncService).sync();

        VelarcSyncScheduler scheduler = new VelarcSyncScheduler(syncService, Duration.ofMillis(50));
        try {
            scheduler.start();
            Thread.sleep(200);
            verify(syncService, atLeast(2)).sync();
        } finally {
            scheduler.stop();
        }
    }

    @Test
    void startTriggersPeriodicSync() throws Exception {
        VelarcSyncScheduler scheduler = new VelarcSyncScheduler(syncService, Duration.ofMillis(50));
        try {
            scheduler.start();
            Thread.sleep(200);
            verify(syncService, atLeastOnce()).sync();
        } finally {
            scheduler.stop();
        }
    }

    @Test
    void stopShutsDownCleanly() {
        VelarcSyncScheduler scheduler = new VelarcSyncScheduler(syncService, Duration.ofSeconds(60));
        scheduler.start();
        scheduler.stop();
        // No exception means clean shutdown — verify the scheduler doesn't throw on double-stop
        scheduler.stop();
    }
}
