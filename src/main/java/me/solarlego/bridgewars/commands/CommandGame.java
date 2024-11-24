package me.solarlego.bridgewars.commands;

import me.solarlego.bridgewars.bridgewars.BridgeGame;
import me.solarlego.bridgewars.gui.TeamGUI;
import me.solarlego.bridgewars.gui.ToolsGUI;
import me.solarlego.solarmain.hub.Hub;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class CommandGame implements Listener {

    private final BridgeGame game;

    public CommandGame(BridgeGame bw) {
        game = bw;
    }

    @EventHandler
    public void onPlayerCommandPreprocessEvent(PlayerCommandPreprocessEvent event) {
        if (game.checkWorld(event.getPlayer().getWorld())) {
            String cmd = event.getMessage().toLowerCase();
            switch (cmd) {
                case "/team":
                case "/teams":
                    if (game.time < 0) {
                        new TeamGUI(game, event.getPlayer());
                    } else {
                        event.getPlayer().sendMessage("\u00A7cYou can not use this anymore!");
                    }
                    event.setCancelled(true);
                    break;
                case "/tool":
                case "/tools":
                    new ToolsGUI(game, event.getPlayer());
                    event.setCancelled(true);
                    break;
                case "/end":
                    if (event.getPlayer().hasPermission("solarlego.command.end")) {
                        game.shutdown();
                        event.setCancelled(true);
                    }
                    break;
            }
        }
    }

}
