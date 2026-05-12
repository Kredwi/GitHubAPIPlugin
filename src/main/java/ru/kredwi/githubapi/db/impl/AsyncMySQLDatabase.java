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


import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Setter;
import lombok.extern.java.Log;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.kredwi.githubapi.api.exception.db.DatabaseInitializeException;
import ru.kredwi.githubapi.api.exception.db.DatabaseValueNotFoundException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Log
public class AsyncMySQLDatabase extends CommonAsyncDatabase {
    @Setter
    private String username;
    @Setter
    private String password;

    @Override
    public void createSession(@NotNull String sessionName,
                              @Nullable Runnable beforeLoad,
                              @Nullable Runnable afterLoad) throws DatabaseValueNotFoundException {
        super.createSession(sessionName, () -> {

            if (beforeLoad != null)
                beforeLoad.run();

            val sql = "SELECT linkedUsername FROM `githubapi` WHERE `username` = ?";

            try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
                statement.setString(1, sessionName);
                ResultSet result = statement.executeQuery();
                if (result.next()) {
                    val linkedUsername = result.getString("linkedUsername");
                    if (linkedUsername == null || linkedUsername.isEmpty())
                        throw new DatabaseValueNotFoundException("linkedUsername is empty");

                    return linkedUsername;
                }
            } catch (SQLException e) {
                log.severe("Error of get profile from db " + e.getMessage());
            }
            return null;
        }, afterLoad);
    }

    @Override
    protected void saveProfileRequest(String username, String linkedUsername) {
        val sql = "INSERT INTO `githubapi` (username, linkedUsername)"
                + " VALUES (?, ?) ON DUPLICATE KEY UPDATE linkedUsername = ?";
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, linkedUsername);
            statement.setString(3, linkedUsername);
            if (statement.executeUpdate() == 0)
                debug("Save profile returned 0 changes rows");
        } catch (SQLException e) {
            log.severe("Error of saving profile to db " + e.getMessage());
        }
    }

    public void init() {
        debug("Init database");
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        dataSource = new HikariDataSource(config);

        createTablesIfExists();
        debug("MySQL database is ready");
    }


    private void createTablesIfExists() {
        try (Statement statement = getConnection().createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS `githubapi` (\n" +
                    "    id INT PRIMARY KEY AUTO_INCREMENT,\n" +
                    "    username VARCHAR(255) UNIQUE NOT NULL,\n" +
                    "    linkedUsername VARCHAR(255) NOT NULL\n" +
                    ");");
            debug("Tables with name `githubapi` is successfully created");
        } catch (SQLException e) {
            throw new DatabaseInitializeException("Error of creating db table " + e.getMessage());
        }
    }
}
