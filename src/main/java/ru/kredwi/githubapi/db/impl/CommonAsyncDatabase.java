package ru.kredwi.githubapi.db.impl;

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

import lombok.Setter;
import lombok.extern.java.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.kredwi.githubapi.api.cache.SessionCache;
import ru.kredwi.githubapi.api.exception.db.DatabaseValueNotFoundException;
import ru.kredwi.githubapi.async.AbstractAsyncCache;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;

/**
 * Common class for implements async database
 *
 * @author Kredwi
 * @since 1.3
 *
 */
@Log
public abstract class CommonAsyncDatabase extends AbstractAsyncCache<String> {

    /**
     * URL for connection
     *
     */
    @Setter
    protected String url;
    /**
     * Single connection instance
     *
     */
    @Nullable
    protected Connection connection;
    /**
     * debug mode
     *
     */
    @Setter
    private boolean debug;

    /**
     * Method for creating session with before runnable</br>
     * new async task is not created if old task is not complete
     *
     * @param sessionName name of session
     * @param beforeLoad  running before execution create session
     * @param afterLoad   running after execution create session see {@link ru.kredwi.githubapi.api.cache.SessionCache#createSession(String, Supplier, Runnable)}
     * @since 1.3
     *
     */
    public abstract void createSession(@NotNull String sessionName,
                                       @Nullable Runnable beforeLoad,
                                       @Nullable Runnable afterLoad) throws DatabaseValueNotFoundException;

    /**
     * intern method for save profile
     *
     * @param username       player name
     * @param linkedUsername value of linked username
     * @since 1.3
     *
     */
    protected abstract void saveProfileRequest(String username, String linkedUsername);


    /**
     * Wrapper of {@link AbstractAsyncCache#stopSession()}.</br>
     * This wrapper closed connection to database
     *
     * @throws RuntimeException if a database access error occurs
     * @see AbstractAsyncCache#stopSession()
     * @since 1.3
     *
     */
    @Override
    public void stopSession() {
        try {
            if (connection != null && !connection.isClosed()) {
                debug("Close connection with database");
                connection.close();
            }
            super.stopSession();
        } catch (SQLException e) {
            throw new RuntimeException("Error of stop session", e);
        }
    }

    /**
     * Method for update session data and recreate session
     *
     * @param sessionName      name of session
     * @param linkeageUsername new linked username
     * @param afterLoad        callback runnable running after loading see {@link SessionCache#createSession}
     *
     */
    public void updateSession(@NotNull String sessionName, String linkeageUsername, Runnable afterLoad) {
        createSession(sessionName, () -> {
            saveProfileRequest(sessionName, linkeageUsername);
            return linkeageUsername;
        }, afterLoad);
    }

    /**
     * Method for get a connection
     *
     * @throws IllegalStateException if connection is not created
     *
     */
    protected Connection getConnection() {
        if (connection == null)
            throw new IllegalStateException("Connection is not a ready");
        return this.connection;
    }

    /**
     * Util class for logging debug messages</br>
     * If debug mode is enabled this logging working
     *
     * @param msg logging message
     *
     */
    public void debug(String msg) {
        if (debug)
            log.info("[DEBUG] [DATABASE] ".concat(msg));
    }
}
