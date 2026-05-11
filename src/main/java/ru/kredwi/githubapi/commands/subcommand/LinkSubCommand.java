package ru.kredwi.githubapi.commands.subcommand;

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
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import ru.kredwi.githubapi.MessageSource;
import ru.kredwi.githubapi.api.exception.db.DatabaseValueNotFoundException;
import ru.kredwi.githubapi.api.github.AsyncGitHubProfileManager;
import ru.kredwi.githubapi.db.impl.AsyncMySQLDatabase;

import java.util.Optional;

@AllArgsConstructor
public class LinkSubCommand implements SubCommand {

    @NotNull
    private AsyncMySQLDatabase databaseBridge;
    @NotNull
    private AsyncGitHubProfileManager gitManager;
    @NotNull
    private MessageSource messageSource;

    @Override
    public @NotNull String apply(String[] args, CommandSender sender) {
        if (args.length < 1)
            return "messages.command.no-args";
        databaseBridge.updateSession(sender.getName(), args[0], () -> {
                    try {
                        Optional<String> sessionOptional = databaseBridge.getSession(sender.getName());
                        if (sessionOptional.isPresent()) {
                            String session = sessionOptional.get();

                            gitManager.createSession(session, null);
                            messageSource.sendMessage(sender, messageSource.get("messages.command.api.success_linked"));
                        } else
                            messageSource.sendMessage(sender, messageSource.get("messages.command.api.fail_linked"));

                    } catch (DatabaseValueNotFoundException e) {
                        messageSource.sendMessage(sender, messageSource.get("messages.command.api.not_linked_profile"));
                    }
                });
        return "messages.command.api.loading.save";
    }
}
