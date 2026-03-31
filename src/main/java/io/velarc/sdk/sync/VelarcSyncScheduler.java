package io.velarc.sdk.sync;

import io.velarc.sdk.exception.VelarcConfigException;
import io.velarc.sdk.exception.VelarcServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages startup synchronisation and periodic background refresh of the
 * Velarc SDK configuration.
 *
 * <p>This is a plain Java class with no Spring dependencies — it is wired
 * into the application lifecycle by the auto-configuration layer. Call
 * {@link #start()} to begin periodic refresh and {@link #stop()} to shut
 * down the background executor.
 */
public class VelarcSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(VelarcSyncScheduler.class);

    private final VelarcSyncService syncService;
    private final Duration syncInterval;
    private ScheduledExecutorService executor;

    /**
     * Creates a new sync scheduler.
     *
     * @param syncService  the sync service to poll
     * @param syncInterval interval between periodic sync attempts
     */
    public VelarcSyncScheduler(VelarcSyncService syncService, Duration syncInterval) {
        this.syncService = syncService;
        this.syncInterval = syncInterval;
    }

    /**
     * Attempts an initial sync, falling back to the disk cache if the server
     * is unreachable.
     *
     * <p>This method does not block or retry — the caller is responsible for
     * retry logic based on the {@code velarc.startup.require-sync} setting.
     *
     * @return {@code true} if the sync service is ready after this call
     */
    public boolean performStartupSync() {
        try {
            syncService.sync();
        } catch (VelarcConfigException e) {
            log.error("Startup sync failed due to configuration error, attempting disk cache: {}", e.getMessage());
            syncService.loadFromDisk();
        } catch (VelarcServiceException e) {
            log.warn("Startup sync failed, attempting disk cache: {}", e.getMessage());
            syncService.loadFromDisk();
        }
        return syncService.isReady();
    }

    /**
     * Starts periodic background sync at the configured interval.
     *
     * <p>Sync failures during refresh are logged at {@code WARN} but do not
     * affect readiness — the stale cache continues to serve lookups.
     */
    public void start() {
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "velarc-sync");
            t.setDaemon(true);
            return t;
        });
        executor.scheduleAtFixedRate(this::refreshSync,
                syncInterval.toMillis(), syncInterval.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Shuts down the background sync executor.
     */
    public void stop() {
        if (executor != null) {
            executor.shutdown();
        }
    }

    private void refreshSync() {
        try {
            syncService.sync();
        } catch (VelarcConfigException e) {
            log.error("Periodic sync failed due to configuration error: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("Periodic sync failed: {}", e.getMessage());
        }
    }
}
