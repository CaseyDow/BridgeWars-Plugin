package me.solarlego.bridgewars.bridgewars;

import me.solarlego.bridgewars.BridgeWars;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;

import java.util.Timer;
import java.util.TimerTask;

public class Hologram implements Listener {

    private final BridgeGame game;
    private final ArmorStand stand;
    private int time;
    private final Timer timer;

    public Hologram(BridgeGame bridgeWars, Location loc, String name, int timePer) {
        Bukkit.getServer().getPluginManager().registerEvents(this, BridgeWars.getPlugin());
        game = bridgeWars;
        timer = new Timer();
        time = timePer;
        stand = (ArmorStand) game.worldBridge.spawnEntity(loc, EntityType.ARMOR_STAND);
        stand.setGravity(false);
        stand.setCanPickupItems(false);
        stand.setCustomNameVisible(true);
        stand.setVisible(false);
        stand.setMarker(true);
        ArmorStand nameStand = (ArmorStand) game.worldBridge.spawnEntity(loc.add(0, 0.4, 0), EntityType.ARMOR_STAND);
        nameStand.setGravity(false);
        nameStand.setCanPickupItems(false);
        nameStand.setCustomName(name);
        nameStand.setCustomNameVisible(true);
        nameStand.setVisible(false);
        nameStand.setMarker(true);

        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                updateTime();
                time--;
                if (time == 0) {
                    time = timePer;
                }
                if (!game.isRunning) {
                    timer.cancel();
                    HandlerList.unregisterAll(Hologram.this);
                }
            }
        }, 0, 1000);

    }

    private void updateTime() {
        stand.setCustomName("\u00A7eSpawns in \u00A7c" + time + " \u00A7eseconds");
    }

    @EventHandler
    public void onPlayerArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        if (game.checkWorld(event.getPlayer().getWorld())) {
            if(!event.getRightClicked().isVisible()) {
                event.setCancelled(true);
            }
        }
    }

}
