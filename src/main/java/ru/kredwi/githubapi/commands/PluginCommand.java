package ru.kredwi.githubapi.commands;

import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.kredwi.githubapi.MessageSource;
import ru.kredwi.githubapi.api.SessionGitHubProfileManager;
import ru.kredwi.githubapi.api.exception.ProfileNotFoundException;
import ru.kredwi.githubapi.db.DatabaseInitializeException;
import ru.kredwi.githubapi.db.DatabaseValueNotFoundException;
import ru.kredwi.githubapi.db.impl.mysql.SessionMySQLDatabase;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class PluginCommand implements CommandExecutor, TabCompleter {

    public static final String SUBCOMMAND_TEMPLATE = "githubapi.command.%s";

    private final MessageSource messageSource;
    private final SessionGitHubProfileManager gitManager;
    private final Map<String, BiFunction<String[], CommandSender, @NotNull String>> commands = new HashMap<>();

    public PluginCommand(MessageSource messageSource,
                         SessionMySQLDatabase databaseBridge,
                         SessionGitHubProfileManager gitHubProfileManager) {
        this.messageSource = messageSource;
        this.gitManager = gitHubProfileManager;

        commands.put("link", (args, sender) -> {
            if (args.length < 1)
                return "messages.command.no-args";
            databaseBridge.saveProfile(sender.getName(), args[0],
                    (result) -> {
                        try {
                            databaseBridge.createSession(sender.getName(), () ->
                                    gitManager.createSession(databaseBridge.getProfile(sender.getName()), null));
                        } catch (DatabaseInitializeException e) {
                            messageSource.sendMessage(sender, messageSource.get("messages.command.api.not_linked_profile"));
                        }

                        messageSource.sendMessage(sender, messageSource.get(
                                result
                                        ? "messages.command.api.success_linked"
                                        : "messages.command.api.fail_linked"
                        ));
                    });
            return "messages.command.api.loading.save";
        });

        commands.put("show", (args, sender) -> {
            try {

                val githubUsername = databaseBridge.getProfile(sender.getName());

                if (githubUsername == null || githubUsername.isEmpty())
                    return "messages.command.api.not_linked_profile";

                return String.format(messageSource.get("messages.command.api.show_link"),
                        gitManager.getProfile(githubUsername)
                                .getName());
            } catch (ProfileNotFoundException e) {
                return "messages.command.api.profile_not_found";
            } catch (DatabaseValueNotFoundException e) {
                return "messages.command.api.not_linked_profile";

            }
        });
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
        val commandInstance = commands.get(commandName);
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
            return commands.keySet()
                    .stream()
                    .filter(commandName -> sender.hasPermission(String.format(SUBCOMMAND_TEMPLATE,
                            commandName.toLowerCase())))
                    .collect(Collectors.toList());

        if (args.length == 2)
            return Bukkit.getOnlinePlayers()
                    .stream()
                    .map(Player::getName)
                    .filter(playerName -> playerName.startsWith(args[1]))
                    .collect(Collectors.toList());;

        return commands.keySet()
                .stream()
                .filter(commandName -> commandName.trim().toLowerCase().startsWith(args[0].toLowerCase()))
                .filter(commandName -> sender.hasPermission(String.format(SUBCOMMAND_TEMPLATE, commandName)))
                .collect(Collectors.toList());
    }
}
