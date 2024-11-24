package me.solarlego.bridgewars.bridgewars;

import me.solarlego.bridgewars.BridgeWars;
import me.solarlego.bridgewars.gui.ItemShopGUI;
import me.solarlego.bridgewars.gui.TeamGUI;
import me.solarlego.bridgewars.gui.TeamShopGUI;
import me.solarlego.bridgewars.gui.ToolsGUI;
import me.solarlego.solarmain.Stats;
import me.solarlego.solarmain.commands.CommandChat;
import me.solarlego.solarmain.hub.Hub;
import net.minecraft.server.v1_8_R3.EntityFireball;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityEquipment;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftFireball;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class BridgeEvents implements Listener {

    private final BridgeGame game;
    private final HashMap<Player, Long> cooldown = new HashMap<>();
    private final HashMap<String, List<Integer>> traps = new HashMap<>();
    private final HashMap<String, List<Integer>> goals = new HashMap<>();

    public BridgeEvents(BridgeGame bridgeWars) {
        game = bridgeWars;
        for (String team : game.yml.getConfigurationSection("teams").getKeys(false)) {
            goals.put(team, game.yml.getIntegerList("teams." + team + ".goal"));
            traps.put(team, game.yml.getIntegerList("teams." + team + ".trap"));
        }
    }

    @EventHandler
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
        if (game.checkWorld(event.getPlayer().getWorld())) {
            Hub.resetPlayer(event.getPlayer());

            FileConfiguration playersFile = YamlConfiguration.loadConfiguration(new File(BridgeWars.getPlugin().getDataFolder(), "players.yml"));
            playersFile.addDefault(event.getPlayer().getUniqueId() + ".coins", 0);
            playersFile.addDefault(event.getPlayer().getUniqueId() + ".finals", 0);
            playersFile.addDefault(event.getPlayer().getUniqueId() + ".goals", 0);
            playersFile.options().copyDefaults(true);
            try {
                playersFile.save(new File(BridgeWars.getPlugin().getDataFolder(), "players.yml"));
            } catch (IOException e) {
                e.printStackTrace();
            }

            game.playerJoin(event.getPlayer());
        } else if (game.checkWorld(event.getFrom())) {
            game.playerLeave(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (game.checkWorld(event.getPlayer().getWorld()) && game.time > 0) {
            if (Math.abs(event.getTo().getBlockX()) > 90 || Math.abs(event.getTo().getBlockZ()) > 90) {
                event.setTo(event.getFrom());
                return;
            }
            PlayerInfo mInfo = game.players.get(event.getPlayer().getUniqueId());
            if (mInfo.isSpec() || mInfo.isDead()) {
                return;
            }
            if (event.getTo().getBlockY() < 40) {
                String dieColor = "\u00A7" + mInfo.getTeamPrefix();
                String deathMessage = dieColor + event.getPlayer().getName() + " \u00A7ffell into the void.";
                ArrayList<String> killerMessage = new ArrayList<>();
                if (game.time - mInfo.getDmgTime() <= 10 && mInfo.getDmgPlayer() != null) {
                    PlayerInfo killInfo = game.players.get(mInfo.getDmgPlayer().getUniqueId());
                    deathMessage = dieColor + event.getPlayer().getName() + "\u00A7f was knocked into the void by \u00A7" + killInfo.getTeamPrefix() + killInfo.getPlayer().getName() + "\u00A7f!";
                    killInfo.getPlayer().playSound(killInfo.getPlayer().getLocation(), Sound.SUCCESSFUL_HIT, 1, 1);
                    killerMessage = killerResources(killInfo.getPlayer(), event.getPlayer());
                }
                deathMessage += game.goals.get(mInfo.getTeam()) == 0 ? " \u00A7b\u00A7lFINAL KILL" : "";
                for (PlayerInfo pInfo : game.players.values()) {
                    pInfo.getPlayer().sendMessage(deathMessage);
                }
                if (game.time - mInfo.getDmgTime() <= 10 && mInfo.getDmgPlayer() != null) {
                    PlayerInfo killInfo = game.players.get(mInfo.getDmgPlayer().getUniqueId());
                    for (String msg : killerMessage) {
                        killInfo.getPlayer().sendMessage(msg);
                    }
                    if (game.goals.get(mInfo.getTeam()) == 0) {
                        killInfo.addFinals(1);
                        killInfo.getPlayer().sendMessage("\u00A76+10 coins! (Final Kill)");
                        killInfo.addCoins(10);
                    } else {
                        killInfo.addKills(1);
                    }
                }
                game.playerDeath(event.getPlayer());
            } else {
                for (String team : goals.keySet()) {
                    if (!mInfo.getTeam().toLowerCase().equals(team)) {
                        String teamCap = team.substring(0, 1).toUpperCase() + team.substring(1);
                        if (goals.get(team).size() > 2 && Math.abs(event.getTo().getBlockX() - goals.get(team).get(0)) <= 1 && Math.abs(event.getTo().getBlockY() - goals.get(team).get(1)) <= 2 && Math.abs(event.getTo().getBlockZ() - goals.get(team).get(2)) <= 1) {
                            game.score(teamCap, event.getPlayer());
                        }
                        if (traps.get(team).size() > 3 && event.getTo().getBlockX() > traps.get(team).get(0) && event.getTo().getBlockX() < traps.get(team).get(1) && event.getTo().getBlockZ() > traps.get(team).get(2) && event.getTo().getBlockZ() < traps.get(team).get(3)) {
                            game.trap(teamCap, event.getPlayer());
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (game.checkWorld(event.getPlayer().getWorld())) {
            if (game.time > 0) {
                Material[] noDrop = new Material[] {Material.WOOD_SWORD, Material.WOOD_PICKAXE, Material.IRON_PICKAXE, Material.DIAMOND_PICKAXE, Material.WOOD_AXE, Material.IRON_AXE, Material.DIAMOND_AXE, Material.SHEARS};
                if (Arrays.asList(noDrop).contains(event.getItemDrop().getItemStack().getType())) {
                    event.setCancelled(true);
                }
                for (Material sword : new Material[] {Material.WOOD_SWORD, Material.STONE_SWORD, Material.IRON_SWORD, Material.DIAMOND_SWORD}) {
                    if (event.getPlayer().getInventory().contains(sword)) {
                        return;
                    }
                }
                if (event.getItemDrop().getItemStack().getType() != Material.WOOD_SWORD) {
                    event.getPlayer().getInventory().addItem(new ItemStack(Material.WOOD_SWORD));
                }
            } else {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (game.checkWorld(event.getEntity().getWorld())) {
            PlayerInfo dieInfo = game.players.get(event.getEntity().getUniqueId());
            String dieColor = "\u00A7" + dieInfo.getTeamPrefix();
            String deathMessage = dieColor + event.getEntity().getName() + "\u00A7f died.";
            event.setDeathMessage("");
            ArrayList<String> killerMessage = new ArrayList<>();
            ArrayList<ItemStack> kept = new ArrayList<ItemStack>() {};
            for (ItemStack d : event.getDrops()) {
                if (Arrays.asList(new Material[] {Material.IRON_INGOT, Material.GOLD_INGOT, Material.DIAMOND, Material.PRISMARINE_CRYSTALS}).contains(d.getType())) {
                    kept.add(d);
                }
            }
            event.getDrops().clear();
            event.getDrops().addAll(kept);
            if (game.time - dieInfo.getDmgTime() <= 10 && dieInfo.getDmgPlayer() != null) {
                PlayerInfo killInfo = game.players.get(dieInfo.getDmgPlayer().getUniqueId());
                String killColor = "\u00A7" + killInfo.getTeamPrefix();
                deathMessage = dieColor + event.getEntity().getName() + "\u00A7f was killed by " + killColor + killInfo.getPlayer().getName() + "\u00A7f!";
                killInfo.getPlayer().playSound(killInfo.getPlayer().getLocation(), Sound.SUCCESSFUL_HIT, 1, 1);
                killerMessage = killerResources(killInfo.getPlayer(), event.getEntity());
                event.getDrops().clear();
            } else if (event.getEntity().getLastDamageCause().getCause() == EntityDamageEvent.DamageCause.FALL) {
                deathMessage = dieColor + event.getEntity().getName() + "\u00A7f fell to their death.";
            } else if (event.getEntity().getLastDamageCause().getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
                deathMessage = dieColor + event.getEntity().getName() + "\u00A7f blew up.";
            } else if (event.getEntity().getLastDamageCause().getCause() == EntityDamageEvent.DamageCause.DROWNING) {
                deathMessage = dieColor + event.getEntity().getName() + "\u00A7f drowned.";
            }
            deathMessage += game.goals.get(dieInfo.getTeam()) == 0 ? " \u00A7b\u00A7lFINAL KILL" : "";
            for (PlayerInfo pInfo : game.players.values()) {
                pInfo.getPlayer().sendMessage(deathMessage);
            }
            for (String msg : killerMessage) {
                dieInfo.getDmgPlayer().sendMessage(msg);
            }
            if (game.time - dieInfo.getDmgTime() <= 10 && dieInfo.getDmgPlayer() != null) {
                PlayerInfo killInfo = game.players.get(dieInfo.getDmgPlayer().getUniqueId());
                if (game.goals.get(dieInfo.getTeam()) == 0) {
                    killInfo.addFinals(1);
                    killInfo.getPlayer().sendMessage("\u00A76+10 coins! (Final Kill)");
                    killInfo.addCoins(10);
                } else {
                    killInfo.addKills(1);
                }
            }
            for (ItemStack item : event.getDrops()) {
                game.worldBridge.dropItem(event.getEntity().getLocation(), item);
            }
            event.getDrops().clear();
            event.getEntity().spigot().respawn();
            game.playerDeath(event.getEntity());
        }
    }

    private ArrayList<String> killerResources(Player killer, Player player) {
        ArrayList<String> killerMessage = new ArrayList<>();
        Integer[] mats = new Integer[] {0, 0, 0, 0};
        for (ItemStack item : player.getInventory()) {
            if (item != null) {
                if (item.getType() == Material.IRON_INGOT) {
                    mats[0] += item.getAmount();
                } else if (item.getType() == Material.GOLD_INGOT) {
                    mats[1] += item.getAmount();
                } else if (item.getType() == Material.DIAMOND) {
                    mats[2] += item.getAmount();
                } else if (item.getType() == Material.PRISMARINE_CRYSTALS) {
                    mats[3] += item.getAmount();
                }
            }
        }
        Material[] types = new Material[] {Material.IRON_INGOT, Material.GOLD_INGOT, Material.DIAMOND, Material.PRISMARINE_CRYSTALS};
        for (int i = 0; i < 4; i++) {
            if (mats[i] > 0) {
                String color = i == 0 ? "f" : i == 1 ? "6" : i == 2 ? "b" : "9";
                killerMessage.add("\u00A7" + color + "+" + mats[i] + " " + (i == 0 ? "Iron" : i == 1 ? "Gold" : i == 2 ? "Diamonds" : "Crystals"));
            }
            while (mats[i] > 0) {
                killer.getInventory().addItem(new ItemStack(types[i], mats[i] % 64));
                mats[i] -= mats[i] % 64;
            }
        }
        return killerMessage;
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (game.checkWorld(event.getPlayer().getWorld()) && event.getRightClicked() instanceof Villager) {
            PlayerInfo pInfo = game.players.get(event.getPlayer().getUniqueId());
            if (pInfo.isSpec() || pInfo.isDead()) {
                return;
            } else if (((Villager) event.getRightClicked()).getProfession() == Villager.Profession.BLACKSMITH) {
                new ItemShopGUI(game, event.getPlayer());
            } else if (((Villager) event.getRightClicked()).getProfession() == Villager.Profession.BUTCHER) {
                new TeamShopGUI(game, event.getPlayer());
            }
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (game.checkWorld(event.getEntity().getWorld())) {
            if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
                PlayerInfo hitInfo = game.players.get(event.getEntity().getUniqueId());
                PlayerInfo dmgInfo = game.players.get(event.getDamager().getUniqueId());
                if (hitInfo.getTeam().equals(dmgInfo.getTeam())) {
                    event.setCancelled(true);
                } else {
                    hitInfo.setDmgPlayer((Player) event.getDamager());
                    hitInfo.setDmgTime(game.time);
                }
                if (((Player) event.getEntity()).hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                    EntityPlayer entityDrank = ((CraftPlayer) event.getEntity()).getHandle();
                    for (PlayerInfo pInfo : game.players.values()) {
                        if (!pInfo.getTeam().equals(game.players.get(event.getEntity().getUniqueId()).getTeam())) {
                            EntityPlayer entityPlayer = ((CraftPlayer) pInfo.getPlayer()).getHandle();
                            entityPlayer.playerConnection.sendPacket(new PacketPlayOutEntityEquipment(entityDrank.getId(), 1, CraftItemStack.asNMSCopy(((Player) event.getEntity()).getInventory().getHelmet())));
                            entityPlayer.playerConnection.sendPacket(new PacketPlayOutEntityEquipment(entityDrank.getId(), 2, CraftItemStack.asNMSCopy(((Player) event.getEntity()).getInventory().getChestplate())));
                            entityPlayer.playerConnection.sendPacket(new PacketPlayOutEntityEquipment(entityDrank.getId(), 3, CraftItemStack.asNMSCopy(((Player) event.getEntity()).getInventory().getLeggings())));
                            entityPlayer.playerConnection.sendPacket(new PacketPlayOutEntityEquipment(entityDrank.getId(), 4, CraftItemStack.asNMSCopy(((Player) event.getEntity()).getInventory().getBoots())));
                        }
                    }
                    ((CraftPlayer) event.getEntity()).removePotionEffect(PotionEffectType.INVISIBILITY);
                }
            } else if (event.getDamager() instanceof TNTPrimed && event.getEntity() instanceof Player) {
                event.setDamage(event.getDamage() / 7);
                Vector direct = event.getEntity().getLocation().subtract(event.getDamager().getLocation()).toVector();
                direct.setY(direct.getY() + 0.7);
                event.getEntity().setVelocity(event.getEntity().getVelocity().add(direct.normalize().multiply(1.2)));
            } else if (event.getDamager() instanceof Fireball && event.getEntity() instanceof Player) {
                event.setDamage(event.getDamage() / 10);
                Vector direct = event.getEntity().getLocation().subtract(event.getDamager().getLocation()).toVector();
                direct.setY(direct.getY() + 1);
                event.getEntity().setVelocity(event.getEntity().getVelocity().add(direct.normalize().multiply(1.5)));
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (game.checkWorld(event.getBlock().getWorld())) {
            if (event.getBlock().getY() < 95 || event.getBlock().getY() > 130 || Math.abs(event.getBlock().getX()) > 54 || Math.abs(event.getBlock().getZ()) > 54) {
                event.setCancelled(true);
            } else if (event.getBlock().getType() == Material.TNT) {
                Location loc = new Location(game.worldBridge, event.getBlock().getX() + 0.5, event.getBlock().getY(), event.getBlock().getZ() + 0.5);
                Entity tnt = game.worldBridge.spawnEntity(loc, EntityType.PRIMED_TNT);
                ((TNTPrimed) tnt).setFuseTicks(40);
                event.getBlock().setType(Material.AIR);
            } else {
                event.getBlock().setMetadata("isPlaced", new FixedMetadataValue(BridgeWars.getPlugin(), true));
            }
        }
    }

    @EventHandler
    public void onBreakBlock(BlockBreakEvent event) {
        if (game.checkWorld(event.getBlock().getWorld())) {
            if (!event.getBlock().hasMetadata("isPlaced")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (game.checkWorld(event.getWhoClicked().getWorld()) && (event.getSlotType() == InventoryType.SlotType.ARMOR || game.time < 0)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (game.checkWorld(event.getPlayer().getWorld())) {
            if (event.getAction() != Action.PHYSICAL && game.time < 0 && event.getPlayer().getGameMode() == GameMode.ADVENTURE) {
                if (event.getPlayer().getItemInHand().getType() == Material.WOOL) {
                    new TeamGUI(game, event.getPlayer());
                } else if (event.getPlayer().getItemInHand().getType() == Material.IRON_PICKAXE) {
                    new ToolsGUI(game, event.getPlayer());
                }
            }
            if (event.getPlayer().getItemInHand().getType() == Material.FIREBALL && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
                if (cooldown.containsKey(event.getPlayer()) && System.currentTimeMillis() - cooldown.get(event.getPlayer()) < 500) {
                    return;
                }
                if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
                    if (event.getPlayer().getItemInHand().getAmount() > 1) {
                        event.getPlayer().getItemInHand().setAmount(event.getPlayer().getItemInHand().getAmount() - 1);
                    } else {
                        event.getPlayer().setItemInHand(new ItemStack(Material.AIR));
                    }
                }
                Fireball fb = event.getPlayer().launchProjectile(Fireball.class);
                fb.setIsIncendiary(true);
                fb.setYield(3);

                EntityFireball nms = ((CraftFireball) fb).getHandle();
                Vector dir = event.getPlayer().getLocation().getDirection().multiply(0.1);
                nms.dirX = dir.getX() * 3;
                nms.dirY = dir.getY() * 3;
                nms.dirZ = dir.getZ() * 3;

                event.setCancelled(true);
                cooldown.remove(event.getPlayer());
                cooldown.put(event.getPlayer(), System.currentTimeMillis());
            }
        }
    }

    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        if (game.checkWorld(event.getView().getPlayer().getWorld())) {
            event.getRecipe().getResult().setType(Material.AIR);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (game.checkWorld(event.getPlayer().getWorld())) {
            game.playerLeave(event.getPlayer());
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (game.checkWorld(event.getEntity().getWorld()) && (game.time <= 0 || event.getEntity() instanceof Villager)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (game.checkWorld(event.getLocation().getWorld()) && event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemMerge(ItemMergeEvent event) {
        if (game.checkWorld(event.getEntity().getWorld())) {
            List<String> lore = event.getEntity().getItemStack().getItemMeta().getLore();
            if (lore != null && lore.contains("Forge Item")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        if (game.checkWorld(event.getItem().getWorld())) {
            List<String> lore = event.getItem().getItemStack().getItemMeta().getLore();
            if (lore != null && lore.contains("Forge Item")) {
                ItemMeta meta = event.getItem().getItemStack().getItemMeta();
                meta.setLore(null);
                event.getItem().getItemStack().setItemMeta(meta);
                if (event.getItem().getItemStack().getType() == Material.IRON_INGOT || event.getItem().getItemStack().getType() == Material.GOLD_INGOT) {
                    for (Entity entity : event.getItem().getNearbyEntities(2, 2, 2)) {
                        if (entity instanceof Player && game.players.get(entity.getUniqueId()).getTeam().equals(game.players.get(event.getPlayer().getUniqueId()).getTeam()) && event.getPlayer() != entity) {
                            ((Player) entity).getInventory().addItem(event.getItem().getItemStack());
                        }
                    }
                }
            }
            for (Material sword : new Material[] {Material.STONE_SWORD, Material.IRON_SWORD, Material.DIAMOND_SWORD}) {
                if (event.getItem().getItemStack().getType() == sword && event.getPlayer().getInventory().contains(Material.WOOD_SWORD)) {
                    event.getPlayer().getInventory().removeItem(new ItemStack(Material.WOOD_SWORD));
                }
            }
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (game.checkWorld(event.getEntity().getWorld())) {
            event.blockList().removeIf(block -> !block.hasMetadata("isPlaced"));
        }
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        if (game.checkWorld(event.getBlock().getWorld())) {
            event.blockList().removeIf(block -> !block.hasMetadata("isPlaced"));
        }
    }

    @EventHandler
    public void onPlayerItemDamage(PlayerItemDamageEvent event) {
        if (game.checkWorld(event.getPlayer().getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerConsumeItem(PlayerItemConsumeEvent event) {
        if (game.checkWorld(event.getPlayer().getWorld())) {
            if (event.getItem().getType() == Material.POTION) {
                int slot = event.getPlayer().getInventory().getHeldItemSlot();
                Bukkit.getScheduler().scheduleSyncDelayedTask(BridgeWars.getPlugin(), () -> event.getPlayer().getInventory().setItem(slot, new ItemStack(Material.AIR)), 1);
                if (((PotionMeta) event.getItem().getItemMeta()).hasCustomEffect(PotionEffectType.INVISIBILITY)) {
                    EntityPlayer entityDrank = ((CraftPlayer) event.getPlayer()).getHandle();
                    for (PlayerInfo pInfo : game.players.values()) {
                        if (!pInfo.getTeam().equals(game.players.get(event.getPlayer().getUniqueId()).getTeam())) {
                            EntityPlayer entityPlayer = ((CraftPlayer) pInfo.getPlayer()).getHandle();
                            entityPlayer.playerConnection.sendPacket(new PacketPlayOutEntityEquipment(entityDrank.getId(), 1, CraftItemStack.asNMSCopy(new ItemStack(Material.AIR))));
                            entityPlayer.playerConnection.sendPacket(new PacketPlayOutEntityEquipment(entityDrank.getId(), 2, CraftItemStack.asNMSCopy(new ItemStack(Material.AIR))));
                            entityPlayer.playerConnection.sendPacket(new PacketPlayOutEntityEquipment(entityDrank.getId(), 3, CraftItemStack.asNMSCopy(new ItemStack(Material.AIR))));
                            entityPlayer.playerConnection.sendPacket(new PacketPlayOutEntityEquipment(entityDrank.getId(), 4, CraftItemStack.asNMSCopy(new ItemStack(Material.AIR))));
                            Bukkit.getScheduler().scheduleSyncDelayedTask(BridgeWars.getPlugin(), () -> {
                                if (!event.getPlayer().hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                                    entityPlayer.playerConnection.sendPacket(new PacketPlayOutEntityEquipment(entityDrank.getId(), 1, CraftItemStack.asNMSCopy(event.getPlayer().getInventory().getHelmet())));
                                    entityPlayer.playerConnection.sendPacket(new PacketPlayOutEntityEquipment(entityDrank.getId(), 2, CraftItemStack.asNMSCopy(event.getPlayer().getInventory().getChestplate())));
                                    entityPlayer.playerConnection.sendPacket(new PacketPlayOutEntityEquipment(entityDrank.getId(), 3, CraftItemStack.asNMSCopy(event.getPlayer().getInventory().getLeggings())));
                                    entityPlayer.playerConnection.sendPacket(new PacketPlayOutEntityEquipment(entityDrank.getId(), 4, CraftItemStack.asNMSCopy(event.getPlayer().getInventory().getBoots())));
                                }
                            }, 600);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onBlockSpread(BlockSpreadEvent event) {
        if (game.checkWorld(event.getBlock().getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (game.checkWorld(event.getEntity().getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        if (game.checkWorld(event.getPlayer().getWorld()) && CommandChat.getChat(event.getPlayer()).equals("team")) {
            event.setCancelled(true);
            Stats info = Stats.get(event.getPlayer().getUniqueId());
            PlayerInfo sInfo = game.players.get(event.getPlayer().getUniqueId());
            String msg = "\u00A7" + sInfo.getTeamPrefix() + "[" + sInfo.getTeam() + "] " + info.getPrefix() + info.getName() + ": " + event.getMessage();
            for (PlayerInfo pInfo : game.players.values()) {
                if (pInfo.getTeam().equals(sInfo.getTeam())) {
                    pInfo.getPlayer().sendMessage(msg);
                }
            }
        }
    }

}
