package ru.kredwi.githubapi.async;

import lombok.extern.java.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Log
public class AsyncUtils {

    public static final int SHUTDOWN_TIMEOUT = 5000;

    public static void shutdownTaskExecutor(ExecutorService service) {
        service.shutdown();

        try {
            if (!service.awaitTermination(SHUTDOWN_TIMEOUT, TimeUnit.MILLISECONDS)) {
                service.shutdownNow();

                if (!service.awaitTermination(500, TimeUnit.MILLISECONDS))
                    log.warning("tasks not shutdown after shutdownNow()");
            }
        } catch (InterruptedException e) {
            service.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
