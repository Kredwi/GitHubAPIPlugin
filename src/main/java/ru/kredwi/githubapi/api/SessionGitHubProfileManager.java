package ru.kredwi.githubapi.api;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import lombok.Setter;
import lombok.extern.java.Log;
import lombok.var;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.kredwi.githubapi.api.exception.ProfileNotFoundException;
import ru.kredwi.githubapi.async.AsyncUtils;
import ru.kredwi.githubapi.db.DatabaseValueNotFoundException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Log
public class SessionGitHubProfileManager {

    public static final Gson gson = new Gson();

    public static final String BASE_API = "https://api.github.com";

    private final Map<String, Profile> sessions = new ConcurrentHashMap<>();

    private final Map<String, Future<?>> pendings = new ConcurrentHashMap<>();

    private final ExecutorService gitExecutorService = Executors.newFixedThreadPool(5);

    @Setter
    private boolean debug = false;
    @Setter
    private int timeout = 1000;

    @Nullable
    @Setter
    private String token;

    public Profile getProfile(@NotNull String username) throws ProfileNotFoundException {
        Profile profile = Optional.ofNullable(sessions.get(username))
                .orElse(Profile.EMPTY_PROFILE);

        if (profile == Profile.EMPTY_PROFILE)
            throw new ProfileNotFoundException("Profile with username " + username + " not found");

        return profile;
    }

    public void close() {
        pendings.values().forEach(e -> e.cancel(true));
        sessions.clear();

        AsyncUtils.shutdownTaskExecutor(gitExecutorService);

    }

    @Nullable
    private Profile getProfileFromAPI(String username) throws ProfileNotFoundException {
        URL url = generateURL(username);
        if (url == null)
            return null;
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();

            conn.setConnectTimeout(timeout);
            conn.setReadTimeout(timeout);
            conn.setRequestProperty("User-Agent", "GitHubAPIPlugin/1.0");
            if (token != null && !token.isEmpty())
                conn.setRequestProperty("Authorization", "Bearer " + token);

            String serverResponse;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    (conn.getResponseCode() == HttpURLConnection.HTTP_OK)
                    ? conn.getInputStream()
                    : conn.getErrorStream(), StandardCharsets.UTF_8))) {
                serverResponse = br.lines().collect(Collectors.joining());
            }

            if (conn.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND)
                throw new ProfileNotFoundException("Profile with name " + username + " is not found");

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                if (serverResponse.isEmpty()) {
                    log.severe(
                            String.format("Server return bad response status %s message %s",
                                    conn.getResponseCode(), conn.getResponseMessage())
                    );
                    return null;
                }
                log.severe("Server return bad response. Answer ".concat(serverResponse));
                return null;
            }

            try {
                return gson.fromJson(serverResponse, Profile.class);

            } catch (JsonSyntaxException e) {
                log.severe("Error of json syntax provided json ".concat(serverResponse));
                return null;
            }


        } catch (IOException e) {
            log.severe("Error of connection: ".concat(e.getMessage()));
            return null;
        } finally {
            if (conn != null)
                conn.disconnect();
        }
    }

    public boolean isLoaded(String databaseName) {
        return !pendings.containsKey(databaseName) && sessions.get(databaseName) != null;
    }

    public void createSession(String dataKey, Runnable afterLoad) {
        pendings.computeIfAbsent(dataKey, k -> {
            if (debug)
                log.info(String.format("[DEBUG] [Git Session] Create session with id %s", k));

            return gitExecutorService.submit(() -> {
                try {
                    var profile = getProfileFromAPI(k);
                    if (profile == null)
                        profile = Profile.EMPTY_PROFILE;
                    sessions.put(k, profile);
                } catch (ProfileNotFoundException e) {
                    sessions.putIfAbsent(k, Profile.EMPTY_PROFILE);
                } finally {
                    sessions.putIfAbsent(k, Profile.EMPTY_PROFILE);
                    pendings.remove(k);
                }
                if (afterLoad != null)
                    afterLoad.run();
            });
        });
    }

    public void removeSession(String gitUsername) {
        if (debug)
            log.info(String.format("[DEBUG] [Git Session] Remove session with id %s", gitUsername));

        var task = pendings.remove(gitUsername);
        if (task != null)
            task.cancel(true);
        sessions.remove(gitUsername);
    }

    @Nullable
    private URL generateURL(String username) {
        try {
            return new URL(BASE_API.concat("/users/")
                    .concat(username));
        } catch (MalformedURLException e) {
            log.severe("Error of generate url: ".concat(e.getMessage()));
            return null;
        }
    }
}
