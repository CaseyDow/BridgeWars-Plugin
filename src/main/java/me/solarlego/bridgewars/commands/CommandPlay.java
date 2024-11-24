package me.solarlego.bridgewars.commands;

import me.solarlego.bridgewars.gui.PlayGUI;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class CommandPlay implements Listener {

    @EventHandler
    public void onPlayerCommandPreprocessEvent(PlayerCommandPreprocessEvent event) {
        String cmd = event.getMessage().toLowerCase();
        if (cmd.startsWith("/play bridgewars") || cmd.startsWith("/play bw")) {
            new PlayGUI(event.getPlayer());
            event.setCancelled(true);
        }
    }

}
