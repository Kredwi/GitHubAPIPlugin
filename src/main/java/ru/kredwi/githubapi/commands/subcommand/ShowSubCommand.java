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
import lombok.extern.java.Log;
import lombok.val;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import ru.kredwi.githubapi.MessageSource;
import ru.kredwi.githubapi.api.Profile;
import ru.kredwi.githubapi.api.github.AsyncGitHubProfileManager;
import ru.kredwi.githubapi.db.impl.CommonAsyncDatabase;

import java.util.Optional;

@AllArgsConstructor
@Log
public class ShowSubCommand implements SubCommand {

    @NotNull
    private CommonAsyncDatabase databaseBridge;
    @NotNull
    private AsyncGitHubProfileManager gitManager;
    @NotNull
    private MessageSource messageSource;

    @Override
    public @NotNull String apply(String[] strings, CommandSender sender) {
        if (!databaseBridge.isLoaded(sender.getName())) {
            databaseBridge.createSession(sender.getName(), (Runnable) null, () -> {
                Optional<String> dbSession = databaseBridge.getSession(sender.getName());
                dbSession.ifPresent(s -> gitManager.createSession(s, null));
            });
            return "messages.command.api.loading.loading";
        }
        val githubUsername = databaseBridge.getSession(sender.getName());

        if (!githubUsername.isPresent())
            return "messages.command.api.not_linked_profile";

        Optional<Profile> profile = gitManager.getSession(githubUsername.get());
        return profile.map(value -> String.format(messageSource.get("messages.command.api.show_link"),
                value.getName())).orElse("messages.command.api.profile_not_found");

    }
}
