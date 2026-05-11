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


import lombok.AllArgsConstructor;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Message utils for a localization and send messages
 *
 * @author Kredwi
 * @since 1.0
 *
 */
@AllArgsConstructor
public class MessageSource {

    @NotNull
    private Plugin plugin;
    @Setter
    @NotNull
    private Configuration config;

    /**
     * Get localization message from a config file
     *
     * @param key of localization message (example: com.example.message.helloworld)
     * @return localizated message or key if localization message is not found in config file
     * @throws NullPointerException if key is null
     *
     */
    public String get(@NotNull String key) {
        Objects.requireNonNull(key);

        return config.getString(key, key);
    }

    /**
     * Send message for user in main server thread
     *
     * <pre>
     *     {@code Bukkit.getScheduler()
     *              .runTask(plugin, () -> sender.sendMessage(message));}
     * </pre>
     *
     * @param sender  instance of command sender
     * @param message text of message
     * @throws NullPointerException     if any param is null
     * @throws IllegalArgumentException – if plugin or task is null
     *                                  See: {@link org.bukkit.scheduler.BukkitScheduler#runTask(Plugin, Runnable)}
     *
     */
    public void sendMessage(@NotNull CommandSender sender, @NotNull String message) {
        Objects.requireNonNull(sender);
        Objects.requireNonNull(message);

        Bukkit.getScheduler()
                .runTask(plugin, () -> sender.sendMessage(message));
    }

}
