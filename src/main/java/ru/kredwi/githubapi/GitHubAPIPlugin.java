package ru.kredwi.githubapi;

import lombok.Getter;
import lombok.val;
import lombok.var;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bstats.bukkit.Metrics;
import org.bukkit.configuration.Configuration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.kredwi.githubapi.api.SessionGitHubProfileManager;
import ru.kredwi.githubapi.commands.PluginCommand;
import ru.kredwi.githubapi.db.DatabaseInitializeException;
import ru.kredwi.githubapi.db.impl.mysql.CommonMySQLDatabase;
import ru.kredwi.githubapi.db.impl.mysql.SessionMySQLDatabase;
import ru.kredwi.githubapi.events.PlayerListener;
import ru.kredwi.githubapi.placeholdersapi.PluginExpansion;

import java.util.logging.Logger;

public class GitHubAPIPlugin extends JavaPlugin {

    public static final int METRICS_ID = 31202;

    public static Logger LOGGER;

    private Metrics metrics;

    @Getter
    private CommonMySQLDatabase mySQLDatabase;
    private Configuration config;
    private SessionGitHubProfileManager gitHubProfileManager;

    @Override
    public void onLoad() {
        LOGGER = getLogger();
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.config = getConfig();

        if (config.getInt("version") != 1) {
            getLogger().warning("Invalid config version.");
            getServer().getPluginManager()
                    .disablePlugin(this);
            return;
        }

        var debug = getConfig().getBoolean("debug");
        if (debug)
            getLogger().info("[DEBUG] Debug logging is enabled");

        MessageSource messageSource = new MessageSource(this, getConfig());
        this.gitHubProfileManager = new SessionGitHubProfileManager();
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

        SessionMySQLDatabase mySQLDatabase = SessionMySQLDatabase
                .builder()
                .url(url)
                .username(username)
                .password(password)
                .debug(debug)
                .build();
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
            this.gitHubProfileManager.close();
            this.gitHubProfileManager = null;
        }
        if (this.mySQLDatabase != null) {
            this.mySQLDatabase.disconnect();
            this.mySQLDatabase = null;
        }

        this.config = null;


        if (metrics != null) {
            metrics.shutdown();
            this.metrics = null;
        }
    }
}
