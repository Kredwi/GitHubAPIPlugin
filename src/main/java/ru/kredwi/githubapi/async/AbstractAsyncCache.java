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


import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.kredwi.githubapi.api.CachedSession;
import ru.kredwi.githubapi.api.cache.SessionCache;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

public abstract class AbstractAsyncCache<P> implements SessionCache<P> {

    private final ExecutorService executorService = Executors.newFixedThreadPool(5);
    private final Map<String, CachedSession<P>> sessions = new ConcurrentHashMap<>();
    private final Map<String, Future<?>> loadingPendings = new ConcurrentHashMap<>();

    @Override
    @NotNull
    public Optional<P> getSession(@NotNull String sessionName) {
        Objects.requireNonNull(sessionName, "sessionName cannot null");
        return Optional.ofNullable(sessions.get(sessionName))
                .map(CachedSession::getObject);
    }


    // for next updates
    @Override
    public long getSessionTimestamp(@NotNull String sessionName) {
        Objects.requireNonNull(sessionName, "sessionName' cannot'be null");
        return Optional.ofNullable(sessions.get(sessionName))
                .map(CachedSession::getTimestamp)
                .orElse(-1L);
    }

    @Override
    public void createSession(@NotNull String sessionName,
                              @NotNull Supplier<@Nullable P> loadingCallback,
                              @Nullable Runnable callback) {
        Objects.requireNonNull(sessionName, "sessionName cannot be null");
        Objects.requireNonNull(loadingCallback, "loadingCallback cannot be null");
        runAsyncTask(sessionName,
                () -> {
                    P p = loadingCallback.get();
                    if (p == null) {
                        this.sessions.putIfAbsent(sessionName, CachedSession.empty());
                        return;
                    }
                    sessions.put(sessionName, new CachedSession<>(System.currentTimeMillis(), p));

                    if (callback != null)
                        callback.run();
                });
    }

    @Override
    public void removeSession(@NotNull String sessionName) {
        Objects.requireNonNull(sessionName, "sessionName cannot be null");
        val future = loadingPendings.remove(sessionName);
        if (future != null) future.cancel(true);

        sessions.remove(sessionName);
    }

    @Override
    public void stopSession() {
        this.loadingPendings.values().forEach(future -> future.cancel(true));
        this.loadingPendings.clear();
        this.sessions.clear();

        AsyncUtils.shutdownTaskExecutor(executorService);
    }

    @Override
    public boolean isLoaded(@NotNull String sessionName) {
        Objects.requireNonNull(sessionName, "sessionName cannot be null");
        return !loadingPendings.containsKey(sessionName) && sessions.get(sessionName) != null;
    }

    /**
     * Protected method for create async tasks for a global executor service
     * </br>
     * If async task for a sessionName already created, <b>new task is not running</b> for a current name
     *
     * @param sessionName name of session
     * @param runnable    runnable to run in async
     * @throws NullPointerException if param sessionName or runnable is null
     * @throws RuntimeException     if runnable throwed
     * @since 1.2
     *
     */
    protected void runAsyncTask(@NotNull String sessionName, @NotNull Runnable runnable) {
        Objects.requireNonNull(sessionName, "sessionName cannot be null");
        Objects.requireNonNull(runnable, "runnable cannot be null");
        this.loadingPendings.computeIfAbsent(sessionName, __ ->
                executorService.submit(() -> {
                    try {
                        runnable.run();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        loadingPendings.remove(sessionName);
                    }
                })
        );

    }
}
