package ru.kredwi.githubapi.db.impl.mysql;

import lombok.Builder;
import lombok.extern.java.Log;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.kredwi.githubapi.db.DatabaseValueNotFoundException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Log
public class SessionMySQLDatabase extends CommonMySQLDatabase{

    private final Map<String, String> sessions = new ConcurrentHashMap<>();

    @Builder
    public SessionMySQLDatabase(
            @NotNull String url,
            @NotNull String username,
            String password,
            boolean debug) {
        super(Objects.requireNonNull(url), Objects.requireNonNull(username), password);
        setDebug(debug);
    }

    public void saveProfile(String username, String linkedUsername,
                               @Nullable Consumer<Boolean> callback) {
        getDbExecutorService().submit(() -> {
            boolean saveResult = saveProfileRequest(username, linkedUsername);
            if (saveResult)
                sessions.put(username, linkedUsername);

            if (callback != null)
                callback.accept(saveResult);
        });
    }

    /**
     * Sync
     *
     */
    @Nullable
    public String getProfile(String username) throws DatabaseValueNotFoundException {
        return Optional.ofNullable(sessions.get(username))
                .orElseThrow(() -> new DatabaseValueNotFoundException("Values for " + username + " is not found"));
    }

    public boolean isLoaded(String playerName) {
        return !getPendings().containsKey(playerName) && sessions.get(playerName) != null;
    }

    public void createSession(String dataKey, Runnable afterLoad) {
        getPendings().computeIfAbsent(dataKey, value -> {
            val sql = "SELECT linkedUsername FROM `githubapi` WHERE `username` = ?";

            if (isDebug())
                log.info(String.format("[DEBUG] [Database Session] Create session with id %s", dataKey));

            return getDbExecutorService().submit(() -> {
                try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
                    statement.setString(1, value);
                    ResultSet result = statement.executeQuery();
                    if (result.next()) {
                        val linkedUsername = result.getString("linkedUsername");
                        if (linkedUsername == null || linkedUsername.isEmpty())
                            throw new DatabaseValueNotFoundException("linkedUsername is empty");

                        sessions.put(value, linkedUsername);

                        if (afterLoad != null)
                            afterLoad.run();
                    }
                } catch (SQLException e) {
                    log.severe("Error of get profile from db " + e.getMessage());
                } finally {
                    getPendings().remove(value);
                }
            });
        });
    }

    public void removeSession(String dataKey) {
        if (isDebug())
            log.info(String.format("[DEBUG] [Database Session] Remove session with id %s", dataKey));

        sessions.remove(dataKey);
    }
}
