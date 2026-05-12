package ru.kredwi.githubapi.placeholdersapi;

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
import lombok.var;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.kredwi.githubapi.MessageSource;
import ru.kredwi.githubapi.api.Profile;
import ru.kredwi.githubapi.api.github.AsyncGitHubProfileManager;
import ru.kredwi.githubapi.db.impl.CommonAsyncDatabase;

import java.util.*;
import java.util.function.Function;

/**
 * PlaceholdersAPI expansion for show information from api
 *
 * @author Kredwi
 * @since 1.0
 *
 */
@Log
public class PluginExpansion extends PlaceholderExpansion {

    public static final String EMPTY_PLACEHOLDER = "";
    /**
     * Support placeholders with plugin
     *
     * @since 1.0
     *
     */
    private final Map<String, Function<Profile, String>> placeholders = new HashMap<>();
    @Setter
    private CommonAsyncDatabase dbBridge;
    @Setter
    private AsyncGitHubProfileManager gitHubAPI;
    @Setter
    private MessageSource messageSource;

    public PluginExpansion(CommonAsyncDatabase bridge, AsyncGitHubProfileManager api, MessageSource messageSource) {
        this.dbBridge = bridge;
        this.gitHubAPI = api;
        this.messageSource = messageSource;

        // supported placeholders
        placeholders.put("login", Profile::getLogin);
        placeholders.put("id", p -> String.valueOf(p.getId()));
        placeholders.put("type", Profile::getType);
        placeholders.put("name", Profile::getName);
        placeholders.put("company", Profile::getCompany);
        placeholders.put("blog", Profile::getBlog);
        placeholders.put("location", Profile::getLocation);
        placeholders.put("email", Profile::getEmail);
        placeholders.put("hireable", Profile::getHireable);
        placeholders.put("bio", Profile::getBio);
        placeholders.put("followers", p -> String.valueOf(p.getFollowers()));
        placeholders.put("following", p -> String.valueOf(p.getFollowing()));
        placeholders.put("node_id", Profile::getNodeId);
        placeholders.put("twitter_username", Profile::getTwitterUsername);
        placeholders.put("created_at", Profile::getCreatedAt);
        placeholders.put("updated_at", Profile::getUpdatedAt);
        placeholders.put("public_repos", p -> String.valueOf(p.getPublicRepos()));
        placeholders.put("public_gists", p -> String.valueOf(p.getPublicGists()));
        placeholders.put("site_admin", p -> String.valueOf(p.isSiteAdmin()));
        placeholders.put("user_view_type", Profile::getUserViewType);
    }

    @Override
    public @NotNull String getIdentifier() {
        return "githubapi";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Kredwi";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean canRegister() {
        return super.canRegister();
    }

    @Override
    public @NotNull List<String> getPlaceholders() {
        return new ArrayList<>(placeholders.keySet());
    }

    private void sendMessage(OfflinePlayer player, String message) {
        if (player instanceof Player && player.isOnline())
            ((Player) player).sendMessage(message);
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null)
            return null;
        var loadingMessage = messageSource.get("papi.message.loading");

        // if database is not loading
        if (!dbBridge.isLoaded(player.getName()))
            return loadingMessage; // show loading message

        // if player dont linked github account
        var databaseUsername = dbBridge.getSession(player.getName());
        if (!databaseUsername.isPresent()) {
            return EMPTY_PLACEHOLDER; // empty placeholders
        }

        // If profile from github api is not loading
        if (!gitHubAPI.isLoaded(databaseUsername.get()))
            return loadingMessage; // show loading message

        var profile = gitHubAPI.getSession(databaseUsername.get());
        return profile
                .map(value -> Optional.ofNullable(placeholders.get(params.trim().toLowerCase()))
                        .map(placeholder -> placeholder.apply(value))
                        .orElse(EMPTY_PLACEHOLDER))
                .orElse(EMPTY_PLACEHOLDER);
    }
}
