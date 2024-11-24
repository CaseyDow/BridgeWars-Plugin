package me.solarlego.bridgewars.gui;

import me.solarlego.bridgewars.BridgeWars;
import me.solarlego.bridgewars.bridgewars.BridgeGame;
import me.solarlego.solarmain.Party;
import me.solarlego.solarmain.hub.Hub;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public class PlayGUI implements Listener {

    private final Inventory inv;

    public PlayGUI(Player player) {
        inv = Bukkit.createInventory(null, 45, "Bridgewars");
        initializeItems();
        Bukkit.getServer().getPluginManager().registerEvents(this, BridgeWars.getPlugin());
        player.openInventory(inv);
    }

    public void initializeItems() {
        ItemStack blank = Hub.createItemStack(Material.STAINED_GLASS_PANE, " ", 15);
        for (int i = 0; i < 10; i++) {
            inv.setItem(i, blank);
        }
        inv.setItem(11, blank);
        for (int i = 17; i < 19; i++) {
            inv.setItem(i, blank);
        }
        inv.setItem(20, blank);
        for (int i = 26; i < 45; i++) {
            inv.setItem(i, blank);
        }
        inv.setItem(10, Hub.createItemStack(Material.WOOL, "\u00A7fNew Two Teams", 14, "\u00A77Create a New Game"));
        inv.setItem(19, Hub.createItemStack(Material.WOOL, "\u00A7fNew Four Teams", 11, "\u00A77Create a New Game"));
        int twoPos = 12;
        int fourPos = 21;
        for (BridgeGame game : BridgeWars.getPlugin().getGames()) {
            if (Bukkit.getWorld(game.worldBridge.getName()) == null) {
                continue;
            }
            if (game.teams == 2 && twoPos < 17) {
                inv.setItem(twoPos, Hub.createItemStack(Material.WOOL, "\u00A7fTwo Teams", 14, "\u00A77Join " + game.worldBridge.getName()));
                twoPos++;
            } else if (game.teams == 4 && fourPos < 26) {
                inv.setItem(fourPos, Hub.createItemStack(Material.WOOL, "\u00A7fTwo Teams", 11, "\u00A77Join " + game.worldBridge.getName()));
                fourPos++;
            }
        }
        inv.setItem(39, Hub.createItemStack(Material.ARROW, "\u00A7fBack", 0));
        inv.setItem(40, Hub.createItemStack(Material.BARRIER, "\u00A7cClose", 0));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inv)) {
            return;
        }
        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR || clickedItem.getType() == Material.STAINED_GLASS_PANE) {
            return;
        }

        if (event.getRawSlot() == 39) {
            new me.solarlego.solarmain.gui.PlayGUI((Player) event.getWhoClicked());
        } else if (event.getRawSlot() == 40) {
            event.getWhoClicked().closeInventory();
        } else {
            String worldName;
            if (event.getRawSlot() == 10) {
                worldName = new BridgeGame("miniBT" + BridgeWars.getPlugin().getGames().size(), 2, 1).worldBridge.getName();
            } else if (event.getRawSlot() == 19) {
                worldName = new BridgeGame("miniBF" + BridgeWars.getPlugin().getGames().size(), 4, 1).worldBridge.getName();
            } else {
                worldName = event.getCurrentItem().getItemMeta().getLore().get(0).split(" ")[1];
            }
            Party party = Party.getParty((Player) event.getWhoClicked());
            if (party == null || party.getLeader() != event.getWhoClicked()) {
                join((Player) event.getWhoClicked(), worldName);
            } else {
                for (Player player : party.getPlayers()) {
                    join(player, worldName);
                }
            }
        }
    }

    private void join(Player player, String worldName) {
        if (Bukkit.getWorld(worldName) == null) {
            player.sendMessage("\u00A7cThis game does not exist!");
            return;
        }
        for (BridgeGame game : BridgeWars.getPlugin().getGames()) {
            if (Objects.equals(game.worldBridge.getName(), worldName) && !game.checkWorld(player.getWorld())) {
                player.sendMessage("\u00A77Sending you to " + game.worldBridge.getName() + "...");
                player.sendMessage("\n");
                player.teleport(game.worldBridge.getSpawnLocation().add(0.5, 0, 0.5));
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().equals(inv)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory() == inv) {
            HandlerList.unregisterAll(this);
        }
    }

}
