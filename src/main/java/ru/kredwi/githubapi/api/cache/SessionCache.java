package ru.kredwi.githubapi.api.cache;

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


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.kredwi.githubapi.api.CachedSession;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Basic interface for session cache
 *
 * @param <C> Type of session
 * @author Kredwi
 * @since 1.2
 *
 */
public interface SessionCache<C> {
    /**
     * Session getter
     *
     * @param sessionName name of session
     * @return Optional with session type
     * @throws NullPointerException if sessionName is null
     * @see CachedSession#getObject()
     * @since 1.2
     *
     */
    Optional<C> getSession(@NotNull String sessionName);

    /**
     * Method for create session. Session rewrite if already created.
     *
     * @param sessionName     name of session
     * @param loadingCallback callback for loading data (return loaded data)
     * @param callback        callback running after save data but callback not running if returned data is null
     * @throws NullPointerException if sessionName or loadingCallback is null
     * @since 1.2
     *
     */
    void createSession(@NotNull String sessionName, @NotNull Supplier<@Nullable C> loadingCallback, @Nullable Runnable callback);

    /**
     * Method for remove sessions
     *
     * @param sessionName name of session
     * @throws NullPointerException if sessionName is null
     * @since 1.2
     *
     */
    void removeSession(@NotNull String sessionName);

    /**
     * Stop session instance
     * <ol>
     *     <li>Shutdown run tasks</li>
     *     <li>Clear all pools (pendings if async and sessions)</li>
     * </ol>
     *
     * @since 1.2
     *
     */
    void stopSession();

    /**
     * Return timestamp of created CacheValue
     *
     * @param sessionName name of session
     * @return value of timestamp or -1 if session not found
     * @throws NullPointerException if CachedSession is null
     * @see CachedSession#getTimestamp()
     * @since 1.2
     *
     */
    long getSessionTimestamp(@NotNull String sessionName);

    /**
     * Checking status of loading session
     *
     * @param sessionName name of session
     * @return Returns true if the session data is already loaded and is not currently being loaded.
     * @throws NullPointerException if sessionName is null
     * @since 1.2
     *
     */
    boolean isLoaded(@NotNull String sessionName);
}
