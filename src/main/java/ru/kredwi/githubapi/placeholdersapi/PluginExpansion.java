package ru.kredwi.githubapi.placeholdersapi;

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
import ru.kredwi.githubapi.api.SessionGitHubProfileManager;
import ru.kredwi.githubapi.api.exception.ProfileNotFoundException;
import ru.kredwi.githubapi.db.DatabaseValueNotFoundException;
import ru.kredwi.githubapi.db.impl.mysql.SessionMySQLDatabase;

import java.util.*;
import java.util.function.Function;

@Log
public class PluginExpansion extends PlaceholderExpansion {

    private final Map<String, Function<Profile, String>> placeholders = new HashMap<>();

    @Setter
    private SessionMySQLDatabase dbBridge;
    @Setter
    private SessionGitHubProfileManager gitHubAPI;
    @Setter
    private MessageSource messageSource;

    public PluginExpansion(SessionMySQLDatabase bridge, SessionGitHubProfileManager api, MessageSource messageSource) {
        this.dbBridge = bridge;
        this.gitHubAPI = api;
        this.messageSource = messageSource;

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

        if (!dbBridge.isLoaded(player.getName())) {
            return "";
        }

        try {
            var username = dbBridge.getProfile(player.getName());
            if (username == null) {
                return loadingMessage;
            }

            if (!gitHubAPI.isLoaded(username)) {
                return loadingMessage;
            }

            Profile profile = gitHubAPI.getProfile(username);
            if (profile == null) {
                return loadingMessage;
            }

            return Optional.ofNullable(placeholders.get(params.trim().toLowerCase()))
                    .map(placeholder -> placeholder.apply(profile))
                    .orElse("");
        } catch (ProfileNotFoundException | DatabaseValueNotFoundException e) {
            return "";
        }
    }
}
