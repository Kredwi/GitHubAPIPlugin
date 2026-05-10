package ru.kredwi.githubapi.db.impl.mysql;

import lombok.*;
import lombok.extern.java.Log;
import ru.kredwi.githubapi.async.AsyncUtils;
import ru.kredwi.githubapi.db.DatabaseInitializeException;

import java.sql.*;
import java.util.Map;
import java.util.concurrent.*;

@Log
@RequiredArgsConstructor
@Getter
public abstract class CommonMySQLDatabase {

    @Getter
    private final ExecutorService dbExecutorService = Executors
            .newFixedThreadPool(5);

    private final String url;
    private final String username;
    private final String password;
    @Setter
    private boolean debug;
    @Getter
    private final Map<String, Future<?>> pendings =
            new ConcurrentHashMap<>();
    private Connection connection;

    public void init() {
        debug("Init database");
        try {
            connection = DriverManager.getConnection(url, username, password);
            debug("Connection is created");
        } catch (SQLException e) {
            throw new DatabaseInitializeException(e);
        }

        createTablesIfExists();
        debug("Database is ready");
    }

    public void disconnect() {
        try {
            debug("Cancel async tasks");
            pendings.values().forEach(e -> e.cancel(true));
            pendings.clear();
            AsyncUtils.shutdownTaskExecutor(dbExecutorService);

            if (connection != null) {
                debug("Close connection");
                connection.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void createTablesIfExists() {
        try (Statement statement = connection.createStatement()) {
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

    public void debug(String msg) {
        if (debug)
            log.info("[DEBUG] [DATABASE] ".concat(msg));
    }

    protected Connection getConnection() {
        if (connection == null)
            throw new IllegalStateException("Connection is not a ready");
        return this.connection;
    }

    protected boolean saveProfileRequest(String username, String linkedUsername) {
        val sql = "INSERT INTO `githubapi` (username, linkedUsername)"
                + " VALUES (?, ?) ON DUPLICATE KEY UPDATE linkedUsername = ?";
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, linkedUsername);
            statement.setString(3, linkedUsername);
            if (statement.executeUpdate() == 0) {
                debug("Save profile returned 0 changes rows");
                return false;
            }
            return true;
        } catch (SQLException e) {
            log.severe("Error of saving profile to db " + e.getMessage());
            return false;
        }
    }

}
