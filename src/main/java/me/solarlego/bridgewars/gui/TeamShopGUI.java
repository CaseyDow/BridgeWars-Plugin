package me.solarlego.bridgewars.gui;

import me.solarlego.bridgewars.BridgeWars;
import me.solarlego.bridgewars.bridgewars.PlayerInfo;
import me.solarlego.bridgewars.bridgewars.BridgeGame;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class TeamShopGUI implements Listener {

    private final Inventory inv;
    private final BridgeGame game;
    private final Player player;

    public TeamShopGUI(BridgeGame bridgeWars, Player p) {
        inv = Bukkit.createInventory(null, 27, "Item Shop");
        game = bridgeWars;
        player = p;
        initializeItems();
        Bukkit.getServer().getPluginManager().registerEvents(this, BridgeWars.getPlugin());
        p.openInventory(inv);
    }

    private void initializeItems() {
        HashMap<String, Integer> teamUpgrades = game.upgrades.get(game.players.get(player.getUniqueId()).getTeam());
        inv.setItem(10, shopItem("Sharpened Swords", Material.IRON_SWORD, teamUpgrades.containsKey("Shar") ? 8 : 4));
        inv.setItem(12, shopItem("Reinforced Armor", Material.IRON_CHESTPLATE, teamUpgrades.containsKey("Rein") ? (teamUpgrades.get("Rein") == 4 ? 16 : (int) Math.pow(2, teamUpgrades.get("Rein") + 1)) : 2));
        inv.setItem(14, shopItem("Maniac Miner", Material.IRON_PICKAXE, teamUpgrades.containsKey("Mani") ? 4 : 2));
        inv.setItem(16, shopItem("Miner Fatigue Trap", Material.TRIPWIRE_HOOK, 1));
    }

    private ItemStack shopItem(String name, Material mat, Integer cost) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        String message = "\u00A7eClick to purchase!";
        HashMap<String, Integer> maxLevel = new HashMap<>();
        maxLevel.put("Shar", 2);
        maxLevel.put("Rein", 4);
        maxLevel.put("Mani", 2);
        maxLevel.put("Mine", 1);
        HashMap<String, Integer> teamUpgrades = game.upgrades.get(game.players.get(player.getUniqueId()).getTeam());
        if (teamUpgrades.get(name.substring(0, 4)) != null && teamUpgrades.get(name.substring(0, 4)).equals(maxLevel.get(name.substring(0, 4)))) {
            message = "\u00A7aUNLOCKED";
        }
        meta.setLore(Arrays.asList("\u00A7fCost: \u00A7b" + cost + " Diamonds", "", message));
        meta.setDisplayName("\u00A7" + (player.getInventory().contains(Material.DIAMOND, cost) ? "a" : "c") + name);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inv) || event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.AIR) {
            return;
        }
        event.setCancelled(true);

        String itemName = event.getCurrentItem().getItemMeta().getDisplayName();
        HashMap<String, Integer> teamUpgrades = game.upgrades.get(game.players.get(player.getUniqueId()).getTeam());
        String[] cost = event.getCurrentItem().getItemMeta().getLore().get(0).split(" ");

        if (player.getInventory().contains(Material.DIAMOND, Integer.parseInt(cost[1].substring(2)))) {
            int lvl = 1;
            if (teamUpgrades.containsKey(itemName.substring(2, 6))) {
                if (event.getCurrentItem().getItemMeta().getLore().contains("\u00A7aUNLOCKED")) {
                    player.playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT, 1, 0.5F);
                    player.sendMessage("\u00A7cYou already have this upgrade!");
                    return;
                }
                lvl = teamUpgrades.get(itemName.substring(2, 6)) + 1;
                teamUpgrades.replace(itemName.substring(2, 6), lvl);
            }
            teamUpgrades.put(itemName.substring(2, 6), lvl);
            player.playSound(player.getLocation(), Sound.NOTE_PLING, 1, 2);
            player.getInventory().removeItem(new ItemStack(Material.DIAMOND, Integer.parseInt(cost[1].substring(2))));
            if (event.getRawSlot() == 10) {
                ArrayList<Material> swords = new ArrayList<>();
                swords.add(Material.WOOD_SWORD);
                swords.add(Material.STONE_SWORD);
                swords.add(Material.IRON_SWORD);
                swords.add(Material.DIAMOND_SWORD);
                for (PlayerInfo pInfo : game.players.values()) {
                    if (pInfo.getTeam().equals(game.players.get(player.getUniqueId()).getTeam())) {
                        for (ItemStack item : player.getInventory()) {
                            if (item != null && swords.contains(item.getType())) {
                                item.addEnchantment(Enchantment.DAMAGE_ALL, lvl);
                            }
                        }
                    }
                }
            } else if (event.getRawSlot() == 12) {
                for (PlayerInfo pInfo : game.players.values()) {
                    if (pInfo.getTeam().equals(game.players.get(player.getUniqueId()).getTeam())) {
                        for (ItemStack armor : player.getInventory().getArmorContents()) {
                            armor.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, lvl);
                        }
                    }
                }
            } else if (event.getRawSlot() == 14) {
                for (PlayerInfo pInfo : game.players.values()) {
                    if (pInfo.getTeam().equals(game.players.get(player.getUniqueId()).getTeam())) {
                        pInfo.getPlayer().removePotionEffect(PotionEffectType.FAST_DIGGING);
                        pInfo.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, Integer.MAX_VALUE, lvl - 1));
                    }
                }
            }
            initializeItems();
        } else {
            player.playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT, 1, 0.5F);
            player.sendMessage("\u00A7cYou do not have enough resources to buy this!");
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
