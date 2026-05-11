package ru.kredwi.githubapi.async;

/*-
 * #%L
 * GithubAPIPlugin
 * %%
 * Copyright (C) 2026 Kredwi
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import lombok.extern.java.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Class for async utils.
 *
 * @author Kredwi
 * @since 1.0
 *
 */
@Log
public class AsyncUtils {

    /**
     * Time after now check termination
     *
     */
    public static final int SHUTDOWN_TIMEOUT = 5000;

    /**
     * Method for shutdown executors tasks
     *
     * @param service instance of ExecutorService
     * @since 1.0
     *
     */
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
