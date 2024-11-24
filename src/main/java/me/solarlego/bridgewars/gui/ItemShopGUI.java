package me.solarlego.bridgewars.gui;

import me.solarlego.bridgewars.BridgeWars;
import me.solarlego.bridgewars.bridgewars.PlayerInfo;
import me.solarlego.bridgewars.bridgewars.BridgeGame;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
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
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;

public class ItemShopGUI implements Listener {

    private final Inventory inv;
    private final BridgeGame game;
    private final Player player;
    private final HashMap<Material, String> itemTypes;

    public ItemShopGUI(BridgeGame bridgeWars, Player p) {
        inv = Bukkit.createInventory(null, 45, "Team Shop");
        game = bridgeWars;
        player = p;
        itemTypes = new HashMap<>();
        initializeItems();
        Bukkit.getServer().getPluginManager().registerEvents(this, BridgeWars.getPlugin());

        p.openInventory(inv);
    }

    private void initializeItems() {
        inv.setItem(10, shopItem("Wool", new ItemStack(Material.WOOL, 16), 4, "Iron", "B"));
        inv.setItem(11, shopItem("End Stone", new ItemStack(Material.ENDER_STONE, 16), 16, "Iron", "B"));
        inv.setItem(12, shopItem("Wood", new ItemStack(Material.WOOD, 16), 4, "Gold", "B"));
        inv.setItem(13, shopItem("TNT", new ItemStack(Material.TNT, 1), 4, "Gold", "M"));
        inv.setItem(14, shopItem("Ladder", new ItemStack(Material.LADDER, 8), 4, "Iron", "B"));
        inv.setItem(15, shopItem("Permenant Iron Chestplate", new ItemStack(Material.IRON_CHESTPLATE, 1), 8, "Gold", ""));
        inv.setItem(16, shopItem("Permenant Diamond Chestplate", new ItemStack(Material.DIAMOND_CHESTPLATE, 1), 4, "Crystals", ""));
        inv.setItem(19, shopItem("Stone Sword", new ItemStack(Material.STONE_SWORD, 1), 10, "Iron", "W"));
        inv.setItem(20, shopItem("Iron Sword", new ItemStack(Material.IRON_SWORD, 1), 4, "Gold", "W"));
        inv.setItem(21, shopItem("Diamond Sword", new ItemStack(Material.DIAMOND_SWORD, 1), 1, "Crystals", "W"));
        inv.setItem(22, shopItem("Golden Apple", new ItemStack(Material.GOLDEN_APPLE, 1), 2, "Gold", "M"));
        if (player.getInventory().contains(Material.IRON_PICKAXE)) {
            inv.setItem(23, shopItem("Diamond Pickaxe", new ItemStack(Material.DIAMOND_PICKAXE, 1), 4, "Gold", "T"));
        } else if (player.getInventory().contains(Material.WOOD_PICKAXE)) {
            inv.setItem(23, shopItem("Iron Pickaxe", new ItemStack(Material.IRON_PICKAXE, 1), 10, "Iron", "T"));
        } else {
            inv.setItem(23, shopItem("Wood Pickaxe", new ItemStack(Material.WOOD_PICKAXE, 1), 10, "Iron", "T"));
        }
        if (player.getInventory().contains(Material.IRON_AXE)) {
            inv.setItem(24, shopItem("Diamond Axe", new ItemStack(Material.DIAMOND_AXE, 1), 4, "Gold", "T"));
        } else if (player.getInventory().contains(Material.WOOD_AXE)) {
            inv.setItem(24, shopItem("Iron Axe", new ItemStack(Material.IRON_AXE, 1), 10, "Iron", "T"));
        } else {
            inv.setItem(24, shopItem("Wood Axe", new ItemStack(Material.WOOD_AXE, 1), 10, "Iron", "T"));
        }
        inv.setItem(25, shopItem("Permenant Shears", new ItemStack(Material.SHEARS, 1), 20, "Iron", "T"));
        inv.setItem(28, shopItem("Invisibility (30 seconds)", potion(PotionType.INVISIBILITY, PotionEffectType.INVISIBILITY, 600, 0), 1, "Crystals", "M"));
        inv.setItem(29, shopItem("Jump Boost IV (45 seconds)", potion(PotionType.JUMP, PotionEffectType.JUMP, 900, 3), 1, "Crystals", "M"));
        inv.setItem(30, shopItem("Speed II (45 seconds)", potion(PotionType.SPEED, PotionEffectType.SPEED, 900, 1), 1, "Crystals", "M"));
        inv.setItem(31, shopItem("Ender Pearl", new ItemStack(Material.ENDER_PEARL), 2, "Crystals", "M"));
        inv.setItem(32, shopItem("Fireball", new ItemStack(Material.FIREBALL), 32, "Iron", "M"));
        inv.setItem(33, shopItem("Bow", new ItemStack(Material.BOW, 1), 8, "Gold", "W"));
        inv.setItem(34, shopItem("Arrow", new ItemStack(Material.ARROW, 8), 2, "Gold", "M"));
    }

    private ItemStack shopItem(String name, ItemStack item, Integer cost, String type, String itemType) {
        ItemMeta meta = item.getItemMeta();
        Material material = type.equals("Iron") ? Material.IRON_INGOT : type.equals("Gold") ? Material.GOLD_INGOT : Material.PRISMARINE_CRYSTALS;
        String color = type.equals("Iron") ? "f" : type.equals("Gold") ? "6" : "9";
        meta.setLore(Arrays.asList("\u00A7fCost: \u00A7" + color + cost + " " + type, "", "\u00A7eClick to purchase!"));
        meta.setDisplayName("\u00A7" + (player.getInventory().contains(material, cost) ? "a" : "c") + name);
        item.setItemMeta(meta);
        if (!itemTypes.containsKey(item.getType())) {
            itemTypes.put(item.getType(), itemType);
        }
        return item;
    }

    private ItemStack potion(PotionType type, PotionEffectType effect, Integer duration, Integer amp) {
        Potion potion = new Potion(type, 1);
        ItemStack item = potion.toItemStack(1);
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        meta.addCustomEffect(new PotionEffect(effect, duration, amp), true);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inv) || event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.AIR) {
            return;
        }
        event.setCancelled(true);

        ItemStack item = new ItemStack(event.getCurrentItem().getType(), event.getCurrentItem().getAmount());
        String[] cost = event.getCurrentItem().getItemMeta().getLore().get(0).split(" ");
        Material material = cost[2].equals("Iron") ? Material.IRON_INGOT : cost[2].equals("Gold") ? Material.GOLD_INGOT : Material.PRISMARINE_CRYSTALS;
        if (player.getInventory().contains(material, Integer.parseInt(cost[1].substring(2)))) {
            Material replItem = Material.AIR;
            PlayerInfo pInfo = game.players.get(player.getUniqueId());
            if (event.getRawSlot() == 10) {
                item.setDurability((short) game.yml.getInt("teams." + pInfo.getTeam().toLowerCase() + ".damage"));
            } else if (event.getRawSlot() == 15 || event.getRawSlot() == 16) {
                for (ItemStack piece : player.getInventory().getArmorContents()) {
                    if (piece.getType() == Material.IRON_CHESTPLATE && event.getRawSlot() == 15 || piece.getType() == Material.DIAMOND_CHESTPLATE) {
                        player.playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT, 1, 0.5F);
                        player.sendMessage("You already have this armor piece!");
                        return;
                    }
                }
                replItem = null;
                ItemStack chest = new ItemStack(event.getRawSlot() == 15 ? Material.IRON_CHESTPLATE : Material.DIAMOND_CHESTPLATE);
                if (game.upgrades.get(pInfo.getTeam()).containsKey("Rein")) {
                    chest.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, game.upgrades.get(pInfo.getTeam()).get("Rein"));
                }
                pInfo.setArmor(event.getRawSlot() == 15 ? "Iron" : "Diamond");
                player.getInventory().setChestplate(chest);
            } else if (event.getRawSlot() > 18 && event.getRawSlot() < 22) {
                if (player.getInventory().contains(Material.WOOD_SWORD)) {
                    replItem = Material.WOOD_SWORD;
                }
                if (game.upgrades.get(pInfo.getTeam()).containsKey("Shar")) {
                    item.addEnchantment(Enchantment.DAMAGE_ALL, game.upgrades.get(pInfo.getTeam()).get("Shar"));
                }
                ItemMeta swordMeta = item.getItemMeta();
                swordMeta.spigot().setUnbreakable(true);
                item.setItemMeta(swordMeta);
            } else if (event.getRawSlot() == 23) {
                if (player.getInventory().contains(Material.DIAMOND_PICKAXE)) {
                    player.playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT, 1, 0.5F);
                    player.sendMessage("\u00A7cYou already have the highest tier of this item!");
                    return;
                } else if (player.getInventory().contains(Material.IRON_PICKAXE)) {
                    pInfo.setPickaxe("Diamond");
                    replItem = Material.IRON_PICKAXE;
                } else if (player.getInventory().contains(Material.WOOD_PICKAXE)) {
                    pInfo.setPickaxe("Iron");
                    replItem = Material.WOOD_PICKAXE;
                } else {
                    pInfo.setPickaxe("Wood");
                }
                ItemMeta meta = item.getItemMeta();
                meta.addEnchant(Enchantment.DIG_SPEED, 1, true);
                meta.spigot().setUnbreakable(true);
                item.setItemMeta(meta);
            } else if (event.getRawSlot() == 24) {
                if (player.getInventory().contains(Material.DIAMOND_AXE)) {
                    player.playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT, 1, 0.5F);
                    player.sendMessage("\u00A7cYou already have the highest tier of this item!");
                    return;
                } else if (player.getInventory().contains(Material.IRON_AXE)) {
                    pInfo.setAxe("Diamond");
                    replItem = Material.IRON_AXE;
                } else if (player.getInventory().contains(Material.WOOD_AXE)) {
                    pInfo.setAxe("Iron");
                    replItem = Material.WOOD_AXE;
                } else {
                    pInfo.setAxe("Wood");
                }
                ItemMeta meta = item.getItemMeta();
                meta.addEnchant(Enchantment.DIG_SPEED, 1, true);
                meta.spigot().setUnbreakable(true);
                item.setItemMeta(meta);
            } else if (event.getRawSlot() == 25) {
                if (player.getInventory().contains(Material.SHEARS)) {
                    player.playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT, 1, 0.5F);
                    player.sendMessage("\u00A7cYou already have this item!");
                    return;
                }
                pInfo.setShears(true);
                ItemMeta meta = item.getItemMeta();
                meta.spigot().setUnbreakable(true);
                item.setItemMeta(meta);
            } else if (event.getRawSlot() == 28) {
                item = potion(PotionType.INVISIBILITY, PotionEffectType.INVISIBILITY, 600, 0);
            } else if (event.getRawSlot() == 29) {
                item = potion(PotionType.JUMP, PotionEffectType.JUMP, 900, 5);
            } else if (event.getRawSlot() == 30) {
                item = potion(PotionType.SPEED, PotionEffectType.SPEED, 900, 1);
            } else if (event.getRawSlot() == 32) {
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName("\u00A7fFireball");
                item.setItemMeta(meta);
            }
            if (replItem == Material.AIR) {
                int add = item.getAmount();
                for (ItemStack i : player.getInventory()) {
                    if (i != null && i.getType() == item.getType() && i.getDurability() == item.getDurability()) {
                        if (i.getAmount() + add > i.getMaxStackSize()) {
                            add -= i.getMaxStackSize() - i.getAmount();
                            i.setAmount(i.getMaxStackSize());
                        } else {
                            i.setAmount(i.getAmount() + add);
                            add = 0;
                        }
                    }
                }
                if (add > 0) {
                    ConfigurationSection toolsConfig = YamlConfiguration.loadConfiguration(new File(BridgeWars.getPlugin().getDataFolder(), "tools.yml")).getConfigurationSection(String.valueOf(player.getUniqueId()));
                    for (String tool : toolsConfig.getKeys(false)) {
                        int slot = Integer.parseInt(tool.substring(4, 5));
                        if (toolsConfig.get(tool).equals(itemTypes.get(item.getType()))) {
                            if (player.getInventory().getItem(slot) == null) {
                                player.getInventory().setItem(slot, item);
                                add -= item.getAmount();
                            } else if (player.getInventory().getItem(slot).getType() == item.getType() && player.getInventory().getItem(slot).getDurability() == item.getDurability()) {
                                if (player.getInventory().getItem(slot).getAmount() + add > item.getMaxStackSize()) {
                                    add -= item.getMaxStackSize() - player.getInventory().getItem(slot).getAmount();
                                    player.getInventory().getItem(slot).setAmount(item.getMaxStackSize());
                                } else {
                                    player.getInventory().getItem(slot).setAmount(item.getAmount() + add);
                                    add = 0;
                                }
                            } else if (player.getInventory().firstEmpty() != -1) {
                                Material slotType = player.getInventory().getItem(slot).getType();
                                if (slotType == Material.IRON_INGOT || slotType == Material.GOLD_INGOT || slotType == Material.DIAMOND || slotType == Material.PRISMARINE_CRYSTALS) {
                                    ItemStack before = player.getInventory().getItem(slot);
                                    player.getInventory().setItem(slot, item);
                                    player.getInventory().addItem(before);
                                    add -= item.getAmount();
                                }
                            }
                            if (add == 0) {
                                break;
                            }
                        }
                    }
                }
                if (add > 0) {
                    if (player.getInventory().firstEmpty() != -1) {
                        player.getInventory().addItem(item);
                    } else {
                        int canAdd = 0;
                        for (ItemStack i : player.getInventory()) {
                            if (i.getType() == item.getType() && i.getDurability() == item.getDurability()) {
                                canAdd += i.getMaxStackSize() - i.getAmount();
                            }
                        }
                        if (canAdd >= item.getAmount()) {
                            player.getInventory().addItem(item);
                        } else {
                            player.sendMessage("\u00A7cYou do not have enough space in your inventory!");
                            player.playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT, 1, 0.5F);
                            return;
                        }
                    }
                }
            } else if (replItem != null) {
                for (int i = 0; i < player.getInventory().getContents().length; i++) {
                    if (player.getInventory().getContents()[i] != null && player.getInventory().getContents()[i].getType() == replItem) {
                        player.getInventory().setItem(i, item);
                        break;
                    }
                }
            }
            player.getInventory().removeItem(new ItemStack(material, Integer.parseInt(cost[1].substring(2))));
            player.playSound(player.getLocation(), Sound.NOTE_PLING, 1, 2);
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