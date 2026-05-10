package ru.kredwi.githubapi.events;

import lombok.AllArgsConstructor;
import lombok.val;
import lombok.var;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.kredwi.githubapi.api.SessionGitHubProfileManager;
import ru.kredwi.githubapi.api.exception.ProfileNotFoundException;
import ru.kredwi.githubapi.db.DatabaseValueNotFoundException;
import ru.kredwi.githubapi.db.impl.mysql.SessionMySQLDatabase;

@AllArgsConstructor
public class PlayerListener implements Listener {

    private SessionMySQLDatabase sessionDatabase;
    private SessionGitHubProfileManager gitManager;

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        var username = e.getPlayer().getName();
        try {
            sessionDatabase.createSession(username, () -> {
                gitManager.createSession(sessionDatabase.getProfile(username), null);
            });
        } catch (DatabaseValueNotFoundException exception) { /* empty */ }

    }

    @EventHandler
    public void onPlayerDisconnect(PlayerQuitEvent e) {
        var username = e.getPlayer().getName();
        try {
            var gitUsername = sessionDatabase.getProfile(username);

            sessionDatabase.removeSession(username);

            if (gitUsername != null)
                gitManager.removeSession(gitUsername);
        } catch (DatabaseValueNotFoundException __) { /*empty*/ }
    }

}
