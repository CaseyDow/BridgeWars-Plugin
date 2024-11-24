package me.solarlego.bridgewars.gui;

import me.solarlego.bridgewars.BridgeWars;
import me.solarlego.bridgewars.bridgewars.BridgeGame;
import me.solarlego.bridgewars.bridgewars.PlayerInfo;
import me.solarlego.solarmain.Stats;
import me.solarlego.solarmain.hub.Hub;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;

public class TeamGUI implements Listener {

    private final Inventory inv;
    private final BridgeGame game;

    public TeamGUI(BridgeGame bridgeWars, Player player) {
        inv = Bukkit.createInventory(null, 27, "Teams");
        game = bridgeWars;
        initializeItems();
        Bukkit.getServer().getPluginManager().registerEvents(this, BridgeWars.getPlugin());
        player.openInventory(inv);
    }

    public void initializeItems() {
        String[] teams = game.yml.getConfigurationSection("teams").getKeys(false).toArray(new String[]{});
        int total = teams.length;
        ArrayList<Integer> poses = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            if (total < 5) {
                poses.add(5 - total + 2 * i);
            } else if (total % 2 == 0) {
                poses.add(4 - total / 2 + i + (total / 2 > i ? 1 : 0));
            } else {
                poses.add(4 - (total - 1) / 2 + i);
            }
        }
        for (int i = 0; i < total; i++) {
            String capName = teams[i].substring(0, 1).toUpperCase() + teams[i].substring(1);
            String name = "\u00A7" + PlayerInfo.teamColors.get(capName) + capName + " Team";
            int dmg = game.yml.getInt("teams." + teams[i] + ".damage");
            inv.setItem(poses.get(i) + 9, game.createItemStack(Material.WOOL, name, 1, dmg));
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inv)) {
            return;
        }
        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        Player p = (Player) event.getWhoClicked();

        String itemName = event.getCurrentItem().getItemMeta().getDisplayName();
        game.players.get(p.getUniqueId()).setTeam(itemName.substring(2, itemName.indexOf(" ")));
        p.sendMessage("\u00A7eYou joined " + itemName + "\u00A7e!");
        p.playSound(p.getLocation(), Sound.CLICK, 1, 1);

        int dmg = game.yml.getInt("teams." + game.players.get(p.getUniqueId()).getTeam().toLowerCase() + ".damage");
        p.getInventory().setItem(3, Hub.createItemStack(Material.WOOL, "\u00A7fTeam Selector", dmg, "\u00A7eRight Click to Open!"));

        p.setPlayerListName(Stats.get(p.getUniqueId()).getPrefix() + itemName.substring(0, 2) + p.getName());
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
