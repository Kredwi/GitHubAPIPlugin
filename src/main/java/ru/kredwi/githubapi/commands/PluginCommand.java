package ru.kredwi.githubapi.commands;

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


import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.kredwi.githubapi.MessageSource;
import ru.kredwi.githubapi.api.github.AsyncGitHubProfileManager;
import ru.kredwi.githubapi.commands.subcommand.LinkSubCommand;
import ru.kredwi.githubapi.commands.subcommand.ShowSubCommand;
import ru.kredwi.githubapi.commands.subcommand.SubCommand;
import ru.kredwi.githubapi.db.impl.AsyncMySQLDatabase;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Main plugin command /githubapi. Managing with sub commands
 *
 * @author Kredwi
 * @since 1.0
 *
 */
public class PluginCommand implements CommandExecutor, TabCompleter {

    public static final String SUBCOMMAND_TEMPLATE = "githubapi.command.%s";

    private final MessageSource messageSource;
    private final Map<String, SubCommand> subCommands = new HashMap<>();

    public PluginCommand(MessageSource messageSource,
                         AsyncMySQLDatabase databaseBridge,
                         AsyncGitHubProfileManager gitHubProfileManager) {
        this.messageSource = messageSource;

        subCommands.put("link", new LinkSubCommand(databaseBridge, gitHubProfileManager, messageSource));
        subCommands.put("show", new ShowSubCommand(databaseBridge, gitHubProfileManager, messageSource));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("githubapi.command")) {
            sender.sendMessage(messageSource.get("messages.command.no-permissions"));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(messageSource.get("messages.command.no-args"));
            return true;
        }

        if (args[0].trim().isEmpty()) {
            sender.sendMessage(messageSource.get("messages.command.api.subcommand.not-found"));
            return true;
        }

        val commandName = args[0].trim().toLowerCase();
        val commandInstance = subCommands.get(commandName);
        if (commandInstance != null) {
            if (!sender.hasPermission(String.format(SUBCOMMAND_TEMPLATE, commandName))) {
                sender.sendMessage(messageSource.get("messages.command.api.subcommand.no-permissions"));
                return true;
            }

            String[] subCommandArgument = Arrays.copyOfRange(args, 1, args.length);
            sender.sendMessage(messageSource.get(commandInstance.apply(subCommandArgument, sender)));
            return true;
        }

        sender.sendMessage(messageSource.get("messages.command.api.subcommand.not-found"));

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 0)
            return subCommands.keySet()
                    .stream()
                    .filter(commandName -> sender.hasPermission(String.format(SUBCOMMAND_TEMPLATE,
                            commandName.toLowerCase())))
                    .collect(Collectors.toList());

        if (args.length == 2)
            return Bukkit.getOnlinePlayers()
                    .stream()
                    .map(Player::getName)
                    .filter(playerName -> playerName.startsWith(args[1]))
                    .collect(Collectors.toList());
        ;

        return subCommands.keySet()
                .stream()
                .filter(commandName -> commandName.trim().toLowerCase().startsWith(args[0].toLowerCase()))
                .filter(commandName -> sender.hasPermission(String.format(SUBCOMMAND_TEMPLATE, commandName)))
                .collect(Collectors.toList());
    }
}
