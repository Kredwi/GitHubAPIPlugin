package ru.kredwi.githubapi;

import lombok.AllArgsConstructor;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@AllArgsConstructor
public class MessageSource {

    @NotNull
    private Plugin plugin;
    @Setter
    @NotNull
    private Configuration config;

    public String get(@NotNull String key) {
        Objects.requireNonNull(key);

        return config.getString(key, key);
    }

    public void sendMessage(@NotNull CommandSender sender, @NotNull String message) {
        Objects.requireNonNull(sender);
        Objects.requireNonNull(message);

        Bukkit.getScheduler()
                .runTask(plugin, () -> sender.sendMessage(message));
    }

}
