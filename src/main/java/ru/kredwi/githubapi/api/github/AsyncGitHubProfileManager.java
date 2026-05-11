package ru.kredwi.githubapi.api.github;

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


import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import lombok.Setter;
import lombok.extern.java.Log;
import lombok.var;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.kredwi.githubapi.api.Profile;
import ru.kredwi.githubapi.api.exception.ProfileNotFoundException;
import ru.kredwi.githubapi.async.AbstractAsyncCache;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Log
public class AsyncGitHubProfileManager extends AbstractAsyncCache<Profile> {

    public static final Gson gson = new Gson();

    public static final String BASE_API = "https://api.github.com";

    @Setter
    private boolean debug = false;
    @Setter
    private int timeout = 1000;

    @Nullable
    @Setter
    private String token;

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

    public void createSession(String databaseName, Runnable afterLoad) {
        if (debug)
            log.info("[DEBUG] [GitManager] Create git session with id " + databaseName);

        super.createSession(databaseName,
                () -> {
                    try {
                        var profile = getProfileFromAPI(databaseName);
                        if (profile == null)
                            profile = Profile.EMPTY_PROFILE;
                        return profile;
                    } catch (ProfileNotFoundException e) {
                        return null;
                    }
                }, afterLoad);
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

    @Override
    public void updateSession(@NotNull String sessionName, Profile newSessionInstance, @Nullable Runnable afterLoad) {
        createSession(sessionName, afterLoad);
    }
}
