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
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;
import ru.kredwi.githubapi.api.GithubPlugin;
import ru.kredwi.githubapi.api.exception.db.DatabaseInitializeException;
import ru.kredwi.githubapi.api.github.AsyncGitHubProfileManager;
import ru.kredwi.githubapi.commands.PluginCommand;
import ru.kredwi.githubapi.db.impl.AsyncMySQLDatabase;
import ru.kredwi.githubapi.db.impl.AsyncSQLiteDatabase;
import ru.kredwi.githubapi.db.impl.CommonAsyncDatabase;
import ru.kredwi.githubapi.events.PlayerListener;
import ru.kredwi.githubapi.placeholdersapi.PluginExpansion;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

public class GitHubAPIPlugin extends JavaPlugin implements GithubPlugin {

    public static final int METRICS_ID = 31202;
    public static Logger LOGGER;
    private static GitHubAPIPlugin INSTANCE;
    private Metrics metrics;

    @Getter
    private CommonAsyncDatabase database;
    private Configuration config;
    @Getter
    private AsyncGitHubProfileManager gitSessionManager;

    public GitHubAPIPlugin() {
        INSTANCE = this;
    }

    @Nullable
    public static GithubPlugin getPluginInstance() {
        return INSTANCE;
    }

    @Override
    public void onLoad() {
        LOGGER = getLogger();
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.config = getConfig();
        if (config.getInt("version") != 3) {
            getLogger().warning("Invalid config version. Please remove config.yml file in plugin folder");
            getServer().getPluginManager()
                    .disablePlugin(this);
            return;
        }

        var debug = isDebug();
        if (debug)
            getLogger().info("[DEBUG] Debug logging is enabled");

        MessageSource messageSource = new MessageSource(this, getConfig());
        this.gitSessionManager = new AsyncGitHubProfileManager();
        gitSessionManager.setDebug(debug);
        gitSessionManager.setTimeout(config.getInt("github.http.timeout", 1000));


        val enableToken = config.getBoolean("github.token.enable", false);
        if (enableToken) {
            val token = config.getString("github.token.value");
            if (token != null && !token.isEmpty()) {
                if (debug)
                    getLogger().info("[DEBUG] Token is enabled");
                gitSessionManager.setToken(token);
            }
        }

        var databaseType = config.getString("database.type");
        if (databaseType.equalsIgnoreCase("mysql"))
            setMySQLDatabase();
        else if (databaseType.equalsIgnoreCase("sqlite"))
            setSQLiteDatabase();
        else {
            getLogger().severe("Unknown type of database");
            Bukkit.getPluginManager()
                    .disablePlugin(this);
            return;
        }

        getServer().getPluginManager()
                .registerEvents(new PlayerListener(database, gitSessionManager), this);
        if (isDebug())
            LOGGER.info("[DEBUG] Register PlaceholdersAPI Expansion");
        PlaceholderExpansion expansion = new PluginExpansion(database, gitSessionManager, messageSource);
        expansion.register();

        val pluginCommand = getServer().getPluginCommand("githubapi");
        if (pluginCommand != null) {
            val command = new PluginCommand(messageSource, database,
                    gitSessionManager);
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }

        if (isDebug())
            getLogger().info("[DEBUG] Loading metrics");
        this.metrics = new Metrics(this, METRICS_ID);
    }

    @Override
    public void onDisable() {
        if (this.gitSessionManager != null) {
            this.gitSessionManager.stopSession();
            this.gitSessionManager = null;
        }
        if (this.database != null) {
            this.database.stopSession();
            this.database = null;
        }

        this.config = null;
        if (metrics != null) {
            metrics.shutdown();
            this.metrics = null;
        }
    }

    private void setSQLiteDatabase() {
        if (isDebug())
            getLogger().info("[DEBUG] [DATABASE] initiliaze SQLite database");

        File dbFile = getSQLiteFile();

        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();

        AsyncSQLiteDatabase database = new AsyncSQLiteDatabase();
        database.setUrl(url);
        database.setDebug(isDebug());
        try {
            database.init();
        } catch (DatabaseInitializeException e) {
            getLogger().severe("Error of database initiliaze: " + e.getMessage());
            getServer().getPluginManager()
                    .disablePlugin(this);
            return;
        }
        this.database = database;
    }

    @SuppressWarnings("unused")
    private File getSQLiteFile() {
        File datafolder = getDataFolder();
        if (!datafolder.exists())
            datafolder.mkdirs();
        File dbFile = new File(datafolder, "data.db");
        if (!dbFile.exists()) {
            try {
                dbFile.createNewFile();
            } catch (IOException e) {
                throw new DatabaseInitializeException("Error of creating SQLite file", e);
            }
        }
        return dbFile;
    }

    private void setMySQLDatabase() {
        if (isDebug())
            getLogger().info("[DEBUG] [DATABASE] initiliaze MySQL database");
        var name = config.getString("database.name");
        var host = config.getString("database.host");
        var port = config.getString("database.port");
        var password = config.getString("database.password");
        var username = config.getString("database.username");
        var url = String.format("jdbc:mysql://%s:%s/%s" +
                        "?useSSL=false" +
                        "&allowPublicKeyRetrieval=true" +
                        "&connectTimeout=5000" +
                        "&socketTimeout=30000" +
                        "&autoReconnect=true",
                host, port, name);

        AsyncMySQLDatabase mySQLDatabase = new AsyncMySQLDatabase();
        mySQLDatabase.setUrl(url);
        mySQLDatabase.setUsername(username);
        mySQLDatabase.setPassword(password);
        mySQLDatabase.setDebug(isDebug());

        try {
            mySQLDatabase.init();
        } catch (DatabaseInitializeException e) {
            getLogger().severe("Error of database initiliaze: " + e.getMessage());
            getServer().getPluginManager()
                    .disablePlugin(this);
            return;
        }
        this.database = mySQLDatabase;
    }

    @Override
    public boolean isDebug() {
        if (config == null)
            return false;
        return config.getBoolean("debug");
    }
}
