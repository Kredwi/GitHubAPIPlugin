package ru.kredwi.githubapi.events;

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
import lombok.var;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.kredwi.githubapi.api.exception.db.DatabaseValueNotFoundException;
import ru.kredwi.githubapi.api.github.AsyncGitHubProfileManager;
import ru.kredwi.githubapi.db.impl.CommonAsyncDatabase;

@AllArgsConstructor
public class PlayerListener implements Listener {

    private CommonAsyncDatabase sessionDatabase;
    private AsyncGitHubProfileManager gitManager;

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        var username = e.getPlayer().getName();
        try {
            sessionDatabase.createSession(username, (Runnable) null, () -> sessionDatabase.getSession(username)
                    .ifPresent(gitUsername -> gitManager.createSession(gitUsername, null)));

        } catch (DatabaseValueNotFoundException exception) { /* empty */ }

    }

    @EventHandler
    public void onPlayerDisconnect(PlayerQuitEvent e) {
        var username = e.getPlayer().getName();
        try {
            var gitUsername = sessionDatabase.getSession(username);

            sessionDatabase.removeSession(username);

            gitUsername.ifPresent(s -> gitManager.removeSession(s));
        } catch (DatabaseValueNotFoundException __) { /*empty*/ }
    }

}
