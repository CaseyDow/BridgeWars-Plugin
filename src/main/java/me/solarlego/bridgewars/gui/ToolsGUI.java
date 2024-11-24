package me.solarlego.bridgewars.gui;

import me.solarlego.bridgewars.BridgeWars;
import me.solarlego.bridgewars.bridgewars.BridgeGame;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class ToolsGUI implements Listener {

    private final Inventory inv;
    private final BridgeGame game;
    private final Player player;

    public ToolsGUI(BridgeGame bridgeWars, Player p) {
        inv = Bukkit.createInventory(null, 45, "Item Shop");
        game = bridgeWars;
        player = p;
        initializeItems();
        ConfigurationSection toolsConfig = YamlConfiguration.loadConfiguration(new File(BridgeWars.getPlugin().getDataFolder(), "tools.yml")).getConfigurationSection(String.valueOf(player.getUniqueId()));
        for (String tool : toolsConfig.getKeys(false)) {
            if (!toolsConfig.get(tool).equals("")) {
                ItemStack item;
                if (toolsConfig.get(tool).equals("W")) {
                    item = createItem("\u00A7fWeapons", Material.IRON_SWORD);
                } else if (toolsConfig.get(tool).equals("T")) {
                    item = createItem("\u00A7fTools", Material.IRON_PICKAXE);
                } else if (toolsConfig.get(tool).equals("B")) {
                    item = createItem("\u00A7fBlocks", Material.WOOL);
                } else {
                    item = createItem("\u00A7fMiscellaneous", Material.GOLDEN_APPLE);
                }
                inv.setItem(Integer.parseInt(tool.substring(4, 5)) + 27, item);
            }
        }
        Bukkit.getServer().getPluginManager().registerEvents(this, BridgeWars.getPlugin());
        p.openInventory(inv);
    }

    private void initializeItems() {
        ItemStack blank = createItem(" ", Material.STAINED_GLASS_PANE);
        blank.setDurability((short) 15);
        for (int i = 0; i < 10; i++) {
            inv.setItem(i, blank);
        }
        for (int i = 17; i < 22; i++) {
            inv.setItem(i, blank);
        }
        for (int i = 23; i < 27; i++) {
            inv.setItem(i, blank);
        }
        for (int i = 36; i < 40; i++) {
            inv.setItem(i, blank);
        }
        for (int i = 41; i < 45; i++) {
            inv.setItem(i, blank);
        }
        inv.setItem(10, createItem("\u00A7fWeapons", Material.IRON_SWORD));
        inv.setItem(11, createItem("\u00A7fTools", Material.IRON_PICKAXE));
        inv.setItem(12, createItem("\u00A7fBlocks", Material.WOOL));
        inv.setItem(13, createItem("\u00A7fMiscellaneous", Material.GOLDEN_APPLE));
        inv.setItem(14, new ItemStack(Material.AIR));
        inv.setItem(15, new ItemStack(Material.AIR));
        inv.setItem(16, new ItemStack(Material.AIR));
        inv.setItem(22, createItem("\u00A7fInformation", Material.SIGN, "\u00A7fUse these items to set", "\u00A7fup your hotbar."));
        inv.setItem(40, createItem("\u00A7cDelete", Material.LAVA_BUCKET));
        player.updateInventory();
    }

    private ItemStack createItem(String name, Material mat, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setLore(Arrays.asList(lore));
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inv)) {
            return;
        }
        if (!inv.equals(event.getClickedInventory()) || event.getAction() == InventoryAction.DROP_ONE_SLOT || event.getAction() == InventoryAction.DROP_ALL_SLOT) {
            event.setCancelled(true);
            return;
        }

        if (event.getRawSlot() == 40 && event.getAction() != InventoryAction.HOTBAR_MOVE_AND_READD) {
            event.getClickedInventory().setItem(40, null);
        }
        event.setCancelled(event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD || event.getAction() == InventoryAction.HOTBAR_SWAP || event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY || (event.getRawSlot() < 27 || event.getRawSlot() > 35) && (event.getRawSlot() < 10 || event.getRawSlot() > 15) && event.getRawSlot() != 40);
        Bukkit.getScheduler().scheduleSyncDelayedTask(BridgeWars.getPlugin(), this::initializeItems, 1);
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
        event.getPlayer().setItemOnCursor(null);

        ArrayList<String[]> paths = new ArrayList<>();
        for (int i = 27; i < 36; i++) {
            String item = "";
            if (inv.getItem(i) != null) {
                if (inv.getItem(i).getType() == Material.IRON_SWORD) {
                    item = "W";
                } else if (inv.getItem(i).getType() == Material.IRON_PICKAXE) {
                    item = "T";
                } else if (inv.getItem(i).getType() == Material.WOOL) {
                    item = "B";
                } else if (inv.getItem(i).getType() == Material.GOLDEN_APPLE) {
                    item = "M";
                }
            }
            paths.add(new String[] {"slot" + (i - 27), item});
        }
        game.setupTools(player, paths.get(0), paths.get(1), paths.get(2), paths.get(3), paths.get(4), paths.get(5), paths.get(6), paths.get(7), paths.get(8));
    }

}
