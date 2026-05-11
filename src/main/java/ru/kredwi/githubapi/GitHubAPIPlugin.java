package ru.kredwi.githubapi;

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


import lombok.Getter;
import lombok.val;
import lombok.var;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bstats.bukkit.Metrics;
import org.bukkit.configuration.Configuration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.kredwi.githubapi.api.exception.db.DatabaseInitializeException;
import ru.kredwi.githubapi.api.github.AsyncGitHubProfileManager;
import ru.kredwi.githubapi.commands.PluginCommand;
import ru.kredwi.githubapi.db.impl.AsyncMySQLDatabase;
import ru.kredwi.githubapi.events.PlayerListener;
import ru.kredwi.githubapi.placeholdersapi.PluginExpansion;

import java.util.logging.Logger;

public class GitHubAPIPlugin extends JavaPlugin {

    public static final int METRICS_ID = 31202;

    public static Logger LOGGER;

    private Metrics metrics;

    @Getter
    private AsyncMySQLDatabase mySQLDatabase;
    private Configuration config;
    private AsyncGitHubProfileManager gitHubProfileManager;

    @Override
    public void onLoad() {
        LOGGER = getLogger();
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.config = getConfig();

        if (config.getInt("version") != 2) {
            getLogger().warning("Invalid config version.");
            getServer().getPluginManager()
                    .disablePlugin(this);
            return;
        }

        var debug = getConfig().getBoolean("debug");
        if (debug)
            getLogger().info("[DEBUG] Debug logging is enabled");

        MessageSource messageSource = new MessageSource(this, getConfig());
        this.gitHubProfileManager = new AsyncGitHubProfileManager();
        gitHubProfileManager.setDebug(debug);
        gitHubProfileManager.setDebug(debug);
        gitHubProfileManager.setTimeout(config.getInt("github.http.timeout", 1000));
        val enableToken = config.getBoolean("github.token.enable", false);
        if (enableToken) {
            val token = config.getString("github.token.value");
            if (token != null && !token.isEmpty()) {
                if (debug)
                    getLogger().info("[DEBUG] Token is enabled");
                gitHubProfileManager.setToken(token);
            }
        }

        var name = config.getString("database.name");
        var host = config.getString("database.host");
        var port = config.getString("database.port");
        var password = config.getString("database.password");
        var username = config.getString("database.username");
        var url = String.format("jdbc:mysql://%s:%s/%s?useSSL=false&allowPublicKeyRetrieval=true",
                host, port, name);

        AsyncMySQLDatabase mySQLDatabase = new AsyncMySQLDatabase();
        mySQLDatabase.setUrl(url);
        mySQLDatabase.setUsername(username);
        mySQLDatabase.setPassword(password);
        mySQLDatabase.setDebug(debug);
        try {
            mySQLDatabase.init();
        } catch (DatabaseInitializeException e) {
            getLogger().severe("Error of database initiliaze: " + e.getMessage());
            getServer().getPluginManager()
                    .disablePlugin(this);
            return;
        }
        this.mySQLDatabase = mySQLDatabase;

        getServer().getPluginManager()
                .registerEvents(new PlayerListener(mySQLDatabase, gitHubProfileManager), this);
        if (config.getBoolean("debug"))
            LOGGER.info("Register PlaceholdersAPI Expansion");
        PlaceholderExpansion expansion = new PluginExpansion(mySQLDatabase, gitHubProfileManager, messageSource);
        expansion.register();

        val pluginCommand = getServer().getPluginCommand("githubapi");
        if (pluginCommand != null) {
            val command = new PluginCommand(messageSource, mySQLDatabase,
                    gitHubProfileManager);
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }

        if (debug)
            getLogger().info("[DEBUG] Loading metrics");
        this.metrics = new Metrics(this, METRICS_ID);
    }

    @Override
    public void onDisable() {
        if (this.gitHubProfileManager != null) {
            this.gitHubProfileManager.stopSession();
            this.gitHubProfileManager = null;
        }
        if (this.mySQLDatabase != null) {
            this.mySQLDatabase.stopSession();
            this.mySQLDatabase = null;
        }

        this.config = null;
        if (metrics != null) {
            metrics.shutdown();
            this.metrics = null;
        }
    }
}
