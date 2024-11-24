package me.solarlego.bridgewars.bridgewars;

import me.solarlego.bridgewars.BridgeWars;
import me.solarlego.bridgewars.Scoreboard;
import me.solarlego.bridgewars.commands.CommandGame;
import me.solarlego.solarmain.FileUtils;
import me.solarlego.solarmain.Stats;
import me.solarlego.solarmain.hub.Hub;
import net.minecraft.server.v1_8_R3.ChatComponentText;
import net.minecraft.server.v1_8_R3.PacketPlayOutTitle;
import net.minecraft.server.v1_8_R3.PlayerConnection;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class BridgeGame {

    public World worldBridge;
    public YamlConfiguration yml;
    public HashMap<UUID, PlayerInfo> players = new HashMap<>();
    public int time = -30;
    public HashMap<String, HashMap<String, Integer>> upgrades = new HashMap<>();
    public HashMap<String, Integer> goals = new HashMap<>();
    public boolean isRunning = false;
    public final int teams;

    private Timer timer = new Timer();
    private final Scoreboard sb;

    public BridgeGame(String world, int numPlayers, int version) {
        try {
            InputStream inputStream = Objects.requireNonNull(getClass().getResource("/configs/bridgewars." + numPlayers + "." + version + ".yml")).openStream();
            yml = YamlConfiguration.loadConfiguration(new InputStreamReader(inputStream));
        } catch (IOException e) {
            e.printStackTrace();
        }
        Bukkit.getServer().getPluginManager().registerEvents(new BridgeEvents(this), BridgeWars.getPlugin());
        Bukkit.getServer().getPluginManager().registerEvents(new CommandGame(this), BridgeWars.getPlugin());
        BridgeWars.getPlugin().addGame(this);
        teams = numPlayers;
        sb = new Scoreboard(this, teams, version);

        Bukkit.getServer().unloadWorld(worldBridge, false);
        FileUtils.copyResourcesRecursively(BridgeWars.getPlugin(),"/bridgewars." + teams + "." + version, new File("./" + world));
        worldBridge = new WorldCreator(world).createWorld();

        for (String team : yml.getConfigurationSection("teams").getKeys(false)) {
            team = team.substring(0, 1).toUpperCase() + team.substring(1);
            goals.put(team, 0);
            upgrades.put(team, new HashMap<>());
            PlayerInfo.teamColors.put(team, yml.getString("teams." + team.toLowerCase() + ".prefix"));
        }

    }

    public void playerJoin(Player player) {
        player.teleport(worldBridge.getSpawnLocation().add(0.5, 0, 0.5));
        if (time < 0) {
            player.setGameMode(GameMode.ADVENTURE);
            players.put(player.getUniqueId(), new PlayerInfo(this, player));
            int dmg = yml.getInt("teams." + players.get(player.getUniqueId()).getTeam().toLowerCase() + ".damage");
            player.getInventory().setItem(3, Hub.createItemStack(Material.WOOL, "\u00A7fTeam Selector", dmg, "\u00A7eRight Click to Open!"));
            player.getInventory().setItem(5, Hub.createItemStack(Material.IRON_PICKAXE, "\u00A7fTools Menu", 0, "\u00A7eRight Click to Open!"));
            setupTools(player);
            player.setPlayerListName(Stats.get(player.getUniqueId()).getPrefix() + "\u00A7c" + player.getName());
            for (Player p : worldBridge.getPlayers()) {
                p.showPlayer(player);
                p.sendMessage(Stats.get(player.getUniqueId()).getColor() + player.getName() + " \u00A7ehas joined (\u00A7b" + players.size() + "\u00A7e)");
                sb.updateScoreboard(p);
            }
            if (players.size() == 2) {
                timer = new Timer();
                timer.scheduleAtFixedRate(new TimerTask() {
                    public void run() {
                        runTimer();
                    }
                }, 0, 1000);
            }
        } else if (players.containsKey(player.getUniqueId())) {
            String color = players.get(player.getUniqueId()).getTeamPrefix();
            player.setPlayerListName(Stats.get(player.getUniqueId()).getPrefix() + "\u00A7" + color + player.getName());
            if (goals.get(players.get(player.getUniqueId()).getTeam()) > 0) {
                players.get(player.getUniqueId()).setDead(false);
            }
            playerDeath(player);
        } else {
            players.put(player.getUniqueId(), new PlayerInfo(this, player, true));
            player.setPlayerListName(Stats.get(player.getUniqueId()).getPrefix() + player.getName());
            player.setGameMode(GameMode.SPECTATOR);
            for (PlayerInfo pInfo : players.values()) {
                if (!pInfo.isDead()) {
                    pInfo.getPlayer().hidePlayer(player);
                }
            }
        }
    }

    private void runTimer() {
        String color = time > -10 ? "c" : "6";
        if (Arrays.asList(-30, -20, -10, -5, -4, -3, -2, -1).contains(time)) {
            for (Player player : worldBridge.getPlayers()) {
                player.sendMessage("\u00A7eThe game will start in \u00A7" + color + -time + " \u00A7eseconds!");
                player.playSound(player.getLocation(), Sound.CLICK, 1, 1);
            }
        }
        for (int i = 0; i < 2; i++) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(BridgeWars.getPlugin(), () -> {
                for (Player p : worldBridge.getPlayers()) {
                    if (p.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                        worldBridge.playEffect(p.getLocation(), Effect.FOOTSTEP, 0);
                    }
                }
            }, 10 * i);
        }
        Bukkit.getServer().getScheduler().runTask(BridgeWars.getPlugin(), () -> {
            if (time == 0) {
                ArrayList<String> present = new ArrayList<>();
                for (PlayerInfo pInfo : players.values()) {
                    if (!present.contains(pInfo.getTeam())) {
                        present.add(pInfo.getTeam());
                    }
                }
                if (present.size() > 1) {
                    start();
                } else {
                    for (PlayerInfo pInfo : players.values()) {
                        pInfo.getPlayer().sendMessage("\u00A7cNot enough teams. Start canceled.");
                        pInfo.getPlayer().playSound(pInfo.getPlayer().getLocation(), Sound.CLICK, 1, 1);
                    }
                    time -= 15;
                }
            } else if (time > 0) {
                spawnForgeItems();
                if (time % 360 == 0) {
                    for (PlayerInfo pInfo : players.values()) {
                        if (checkWorld(pInfo.getPlayer().getWorld()) && !pInfo.isSpec()) {
                            pInfo.getPlayer().sendMessage("\u00A7cOne of your goals has fadded away to time!");
                            pInfo.getPlayer().playSound(pInfo.getPlayer().getLocation(), Sound.ENDERMAN_HIT, 1, 1);
                            if (goals.get(pInfo.getTeam()) == 1) {
                                sendTitle(pInfo.getPlayer(), "\u00A7cNO GOALS", "\u00A7fYou will no longer respawn!", 5, 40, 10);
                                pInfo.getPlayer().sendMessage("\u00A7cYou will no longer respawn!");
                                pInfo.getPlayer().playSound(pInfo.getPlayer().getLocation(), Sound.ENDERDRAGON_GROWL, 1, 1);
                            }
                        }
                    }
                    for (String team : goals.keySet()) {
                        goals.replace(team, goals.get(team) > 0 ? goals.get(team) - 1 : 0);
                    }
                }
            }
            for (Player player : worldBridge.getPlayers()) {
                sb.updateScoreboard(player);
            }
            time++;
        });
    }

    private void start() {
        isRunning = true;
        for (Player player : worldBridge.getPlayers()) {
            player.setGameMode(GameMode.SURVIVAL);
            respawnPlayer(player);
            player.playSound(player.getLocation(), Sound.ENDERDRAGON_GROWL, 1, 1);
            player.getEnderChest().clear();
        }

        for (PlayerInfo pInfo : players.values()) {
            goals.replace(pInfo.getTeam(), 3);
        }

        for (Object loc : yml.getList("player-blocks", new ArrayList<Integer>())) {
            if (loc instanceof ArrayList && ((ArrayList<?>) loc).size() == 6) {
                ArrayList<Integer> path = new ArrayList<>();
                for (Object pos : ((ArrayList<?>) loc)) {
                    path.add(Integer.parseInt(String.valueOf(pos)));
                }
                for (int x = path.get(0); Math.abs(x) <= Math.abs(path.get(3)); x += Integer.compare(x, 0)) {
                    for (int y = path.get(1); Math.abs(y) <= Math.abs(path.get(4)); y += Integer.compare(y, 0)) {
                        for (int z = path.get(2); Math.abs(z) <= Math.abs(path.get(5)); z += Integer.compare(z, 0)) {
                            worldBridge.getBlockAt(x, y, z).setMetadata("isPlaced", new FixedMetadataValue(BridgeWars.getPlugin(), true));
                            if (Math.abs(z) == Math.abs(path.get(5))) {
                                break;
                            }
                        }
                        if (Math.abs(y) == Math.abs(path.get(4))) {
                            break;
                        }
                    }
                    if (Math.abs(x) == Math.abs(path.get(3))) {
                        break;
                    }
                }
            }
        }

        for (int y = 140; y < 147; y++) {
            int finalY = y;
            Bukkit.getScheduler().scheduleSyncDelayedTask(BridgeWars.getPlugin(), () -> {
                for (int x = -8; x < 9; x++) {
                    for (int z = -8; z < 9; z++) {
                        worldBridge.getBlockAt(x, finalY, z).setType(Material.AIR);
                    }
                }
            }, y - 139);
        }

        for (Object loc : yml.getList("diamond", new ArrayList<Integer>())) {
            if (loc instanceof ArrayList && ((ArrayList<?>) loc).size() > 2) {
                Location holo = new Location(worldBridge, Float.parseFloat(String.valueOf(((ArrayList<?>) loc).get(0))), Float.parseFloat(String.valueOf(((ArrayList<?>) loc).get(1))) + 2, Float.parseFloat(String.valueOf(((ArrayList<?>) loc).get(2))));
                new Hologram(this, holo, "\u00A7bDiamonds", 30);
            }
        }
        for (Object loc : yml.getList("crystal", new ArrayList<Integer>())) {
            if (loc instanceof ArrayList && ((ArrayList<?>) loc).size() > 2) {
                Location holo = new Location(worldBridge, Float.parseFloat(String.valueOf(((ArrayList<?>) loc).get(0))), Float.parseFloat(String.valueOf(((ArrayList<?>) loc).get(1))) + 2, Float.parseFloat(String.valueOf(((ArrayList<?>) loc).get(2))));
                new Hologram(this, holo, "\u00A79Crystals", 60);
            }
        }

    }

    public void playerLeave(Player player) {
        if (time < 0) {
            players.remove(player.getUniqueId());
            if (players.size() < 2) {
                time = -30;
                timer.cancel();
                for (Player p : worldBridge.getPlayers()) {
                    p.sendMessage("\u00A7cNot enough players. Start canceled.");
                    p.playSound(p.getLocation(), Sound.CLICK, 1, 1);
                }
            }
        } else if (players.get(player.getUniqueId()).isSpec()) {
            players.remove(player.getUniqueId());
        } else {
            players.get(player.getUniqueId()).setDead(true);
            checkWin();
        }
        for (PlayerInfo pInfo : players.values()) {
            sb.updateScoreboard(pInfo.getPlayer());
        }
    }

    public void playerDeath(Player player) {
        player.setGameMode(GameMode.SPECTATOR);
        player.setVelocity(new Vector());
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.teleport(new Location(worldBridge, 0.5, 125, 0.5));
        if (goals.get(players.get(player.getUniqueId()).getTeam()) == 0) {
            players.get(player.getUniqueId()).setDead(true);
            checkWin();
            return;
        }
        for (int i = 0; i < 5; i++) {
            int t = 5 - i;
            Bukkit.getScheduler().scheduleSyncDelayedTask(BridgeWars.getPlugin(), () -> {
                sendTitle(player, "\u00A7cYou DIED!", "\u00A7eYou will respawn in \u00A7c" + t + " \u00A7eseconds!", 0, 80, 0);
                player.sendMessage("\u00A7eYou will respawn in \u00A7c" + t + " \u00A7eseconds!");
            }, 20L * i);
        }
        Bukkit.getScheduler().scheduleSyncDelayedTask(BridgeWars.getPlugin(), () -> {
            respawnPlayer(player);
            sendTitle(player, "\u00A7aRESPAWNED!", "", 0, 15, 10);
            player.sendMessage("\u00A7eYou have respawned!");
        }, 100);
    }

    public void score(String team, Player player) {
        PlayerInfo playerInfo = players.get(player.getUniqueId());
        if (goals.get(team) > 0) {
            goals.replace(team, goals.get(team) - 1);
            player.setFallDistance(0);
            player.setHealth(20);
            player.teleport(playerInfo.getSpawnLoc());
            String teamColor = "\u00A7" + playerInfo.getTeamPrefix();
            String lostColor = "\u00A7" + PlayerInfo.teamColors.get(team);
            for (Player p : worldBridge.getPlayers()) {
                p.sendMessage("\u00A76\u00A7m----------------------------------\n" + teamColor + player.getName() + " \u00A7fscored on " + lostColor + team + " Team!\n\u00A76\u00A7m----------------------------------");
                p.playSound(p.getLocation(), Sound.ENDERMAN_HIT, 1, 1);
            }
            if (goals.get(team) == 0) {
                for (PlayerInfo pInfo : players.values()) {
                    if (checkWorld(pInfo.getPlayer().getWorld())) {
                        if (pInfo.getTeam().equals(team)) {
                            sendTitle(pInfo.getPlayer(), "\u00A7cNO GOALS", "\u00A7fYou will no longer respawn!", 5, 40, 10);
                            pInfo.getPlayer().sendMessage("\u00A7cYou will no longer respawn!");
                        }
                        pInfo.getPlayer().playSound(pInfo.getPlayer().getLocation(), Sound.ENDERDRAGON_GROWL, 1, 1);
                    }
                }
            }
            player.playSound(player.getLocation(), Sound.SUCCESSFUL_HIT, 1, 1);
            players.get(player.getUniqueId()).addGoals(1);
            players.get(player.getUniqueId()).addCoins(25);
            player.sendMessage("\u00A76+25 coins! (Goal)");
            checkWin();
        }
    }

    private void respawnPlayer(Player player) {
        PlayerInfo pInfo = players.get(player.getUniqueId());
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(20);
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        if (pInfo.getPickaxe().equals("Diamond")) {
            pInfo.setPickaxe("Iron");
        } else if (pInfo.getPickaxe().equals("Iron")) {
            pInfo.setPickaxe("Wood");
        }
        if (pInfo.getAxe().equals("Diamond")) {
            pInfo.setAxe("Iron");
        } else if (pInfo.getAxe().equals("Iron")) {
            pInfo.setAxe("Wood");
        }

        ItemStack[] armor = new ItemStack[4];
        armor[3] = new ItemStack(Material.LEATHER_HELMET);
        armor[2] = new ItemStack(Material.LEATHER_CHESTPLATE);
        armor[1] = new ItemStack(Material.LEATHER_LEGGINGS);
        armor[0] = new ItemStack(Material.LEATHER_BOOTS);
        for (ItemStack itemStack : armor) {
            LeatherArmorMeta meta = (LeatherArmorMeta) itemStack.getItemMeta();
            meta.setColor(pInfo.getTeamColor());
            itemStack.setItemMeta(meta);
        }
        if (!pInfo.getArmor().equals("Leather")) {
            armor[2] = new ItemStack(pInfo.getArmor().equals("Iron") ? Material.IRON_CHESTPLATE : Material.DIAMOND_CHESTPLATE);
        }
        if (upgrades.get(pInfo.getTeam()).containsKey("Rein")) {
            for (ItemStack piece : armor) {
                piece.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, upgrades.get(pInfo.getTeam()).get("Rein"));
            }
        }
        player.getInventory().setArmorContents(armor);
        ItemStack sword = new ItemStack(Material.WOOD_SWORD);
        ItemMeta swordMeta = sword.getItemMeta();
        swordMeta.spigot().setUnbreakable(true);
        sword.setItemMeta(swordMeta);
        if (upgrades.get(pInfo.getTeam()).containsKey("Shar")) {
            sword.addEnchantment(Enchantment.DAMAGE_ALL, upgrades.get(pInfo.getTeam()).get("Shar"));
        }
        player.getInventory().setItem(0, sword);
        if (!pInfo.getPickaxe().equals("None")) {
            ItemStack pick = new ItemStack(pInfo.getPickaxe().equals("Wood") ? Material.WOOD_PICKAXE : Material.IRON_PICKAXE);
            ItemMeta meta = pick.getItemMeta();
            meta.addEnchant(Enchantment.DIG_SPEED, 1, true);
            meta.spigot().setUnbreakable(true);
            pick.setItemMeta(meta);
            player.getInventory().setItem(player.getInventory().firstEmpty(), pick);
        }
        if (!pInfo.getAxe().equals("None")) {
            ItemStack axe = new ItemStack(pInfo.getAxe().equals("Wood") ? Material.WOOD_AXE : Material.IRON_AXE);
            ItemMeta meta = axe.getItemMeta();
            meta.addEnchant(Enchantment.DIG_SPEED, 1, true);
            meta.spigot().setUnbreakable(true);
            axe.setItemMeta(meta);
            player.getInventory().setItem(player.getInventory().firstEmpty(), axe);
        }
        if (pInfo.hasShears()) {
            ItemStack shears = new ItemStack(Material.SHEARS);
            ItemMeta meta = shears.getItemMeta();
            meta.spigot().setUnbreakable(true);
            shears.setItemMeta(meta);
            player.getInventory().setItem(player.getInventory().firstEmpty(), shears);
        }
        player.teleport(pInfo.getSpawnLoc());
    }

    private void checkWin() {
        if (!isRunning) {
            return;
        }
        String team = "";
        for (PlayerInfo pInfo : players.values()) {
            if (pInfo.isDead()) {
                continue;
            }
            if (team.equals("")) {
                team = pInfo.getTeam();
            } else if (!pInfo.getTeam().equals(team)) {
                return;
            }
        }
        win(team);
    }

    private void win(String team) {
        timer.cancel();
        isRunning = false;

        String startMessage = "\u00A76\u00A7m----------------------------------\n\u00A7fWinner: ";
        int i = 0;
        for (PlayerInfo pInfo : players.values()) {
            if (pInfo.getTeam().equals(team)) {
                if (i % 2 == 0 && i != 0) {
                    startMessage.concat("\n  ");
                }
                startMessage = startMessage.concat(Stats.get(pInfo.getPlayer().getUniqueId()).getPrefix() + pInfo.getPlayer().getName() + ", ");
                i++;
                if (checkWorld(pInfo.getPlayer().getWorld())) {
                    pInfo.getPlayer().playSound(pInfo.getPlayer().getLocation(), Sound.ENDERDRAGON_GROWL, 1, 1);
                    pInfo.addCoins(50);
                    pInfo.getPlayer().sendMessage("\u00A76+50 coins! (Win)");
                    sendTitle(pInfo.getPlayer(), "\u00A76VICTORY!", "", 5, 65, 10);
                }
            } else if (checkWorld(pInfo.getPlayer().getWorld())) {
                pInfo.getPlayer().playSound(pInfo.getPlayer().getLocation(), Sound.ENDERDRAGON_GROWL, 1, 1);
                if (!pInfo.isSpec()) {
                    sendTitle(pInfo.getPlayer(), "\u00A7cDefeat", "", 5, 65, 10);
                }
            }
        }
        for (Player p : worldBridge.getPlayers()) {
            PlayerInfo pInfo = players.get(p.getUniqueId());
            p.sendMessage(
                    startMessage.substring(0, startMessage.length() - 2)
                            .concat("\n\n\u00A7fKills: \u00A74")
                            .concat(pInfo == null ? "0" : String.valueOf(pInfo.getKills()))
                            .concat("\n\n\u00A7fFinals: \u00A74")
                            .concat(pInfo == null ? "0" : String.valueOf(pInfo.getFinals()))
                            .concat("\n\n\u00A7fGoals: \u00A74")
                            .concat(pInfo == null ? "0" : String.valueOf(pInfo.getGoals()))
                            .concat("\n\u00A7fCoins: \u00A76")
                            .concat(pInfo == null ? "0" : String.valueOf(pInfo.getCoins()))
                            .concat("\n\u00A76\u00A7m----------------------------------"));
        }
        for (PlayerInfo pInfo : players.values()) {
            BridgeWars.getPlugin().updatePlayerFile(pInfo.getPlayer().getUniqueId() + ".coins", pInfo.getCoins());
            BridgeWars.getPlugin().updatePlayerFile(pInfo.getPlayer().getUniqueId() + ".finals", pInfo.getFinals());
            BridgeWars.getPlugin().updatePlayerFile(pInfo.getPlayer().getUniqueId() + ".goals", pInfo.getGoals());
        }

        Bukkit.getScheduler().scheduleSyncDelayedTask(BridgeWars.getPlugin(), this::shutdown, 400);
    }

    private void spawnForgeItems() {
        for (String team : yml.getConfigurationSection("teams").getKeys(false)) {
            List<String> loc = yml.getStringList("teams." + team + ".forge");
            Location forge = new Location(worldBridge, Float.parseFloat(loc.get(0)), Float.parseFloat(loc.get(1)), Float.parseFloat(loc.get(2)));
            forgeItem(forge, Material.IRON_INGOT, 960);
            if (time % 8 == 0) {
                forgeItem(forge, Material.GOLD_INGOT, 2560);
            }
        }

        if (time % 30 == 0) {
            for (Object loc : yml.getList("diamond", new ArrayList<Integer>())) {
                if (loc instanceof ArrayList && ((ArrayList<?>) loc).size() > 2) {
                    Location forge = new Location(worldBridge, Float.parseFloat(String.valueOf(((ArrayList<?>) loc).get(0))), Float.parseFloat(String.valueOf(((ArrayList<?>) loc).get(1))), Float.parseFloat(String.valueOf(((ArrayList<?>) loc).get(2))));
                    forgeItem(forge, Material.DIAMOND, 2400);
                }
            }
        }
        if (time % 60 == 0) {
            for (Object loc : yml.getList("crystal", new ArrayList<Integer>())) {
                if (loc instanceof ArrayList && ((ArrayList<?>) loc).size() > 2) {
                    Location forge = new Location(worldBridge, Float.parseFloat(String.valueOf(((ArrayList<?>) loc).get(0))), Float.parseFloat(String.valueOf(((ArrayList<?>) loc).get(1))), Float.parseFloat(String.valueOf(((ArrayList<?>) loc).get(2))));
                    forgeItem(forge, Material.PRISMARINE_CRYSTALS, 4800);
                }
            }
        }

    }

    public void setupTools(Player player, String[]... paths) {
        FileConfiguration toolsFile = YamlConfiguration.loadConfiguration(new File(BridgeWars.getPlugin().getDataFolder(), "tools.yml"));

        for (int i = 0; i < 9; i++) {
            toolsFile.addDefault(player.getUniqueId() + ".slot" + i, "");
        }
        for (String[] path : paths) {
            if (path.length > 1) {
                toolsFile.set(player.getUniqueId() + "." + path[0], path[1]);
            }
        }

        toolsFile.options().copyDefaults(true);
        try {
            toolsFile.save(new File(BridgeWars.getPlugin().getDataFolder(), "tools.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void trap(String team, Player player) {
        if (upgrades.get(team).containsKey("Mine")) {
            for (PlayerInfo pInfo : players.values()) {
                if (pInfo.getTeam().equals(team)) {
                    pInfo.getPlayer().sendMessage("\u00A7cYour Miner Fatigue Trap has been set off by \u00A7" + players.get(player.getUniqueId()).getTeamPrefix() + player.getName());
                    pInfo.getPlayer().playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT, 1, 0.5F);
                    sendTitle(pInfo.getPlayer(), "\u00A7cTRAP TRIGGERED!", "\u00A7fYour Miner Fatigue Trap has been set off!", 5, 20, 10);
                }
            }
            player.sendMessage("\u00A7cYou set off \u00A7" + PlayerInfo.teamColors.get(team) + team + " Team's \u00A7cMiner Fatigue Trap!");
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, 200, 0));
            player.playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT, 1, 0.5F);
            upgrades.get(team).remove("Mine");
        }
    }

    private void forgeItem(Location gen, Material material, int despawn) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setLore(Collections.singletonList("Forge Item"));
        item.setItemMeta(meta);
        Item dropped = worldBridge.dropItem(gen, item);
        dropped.setVelocity(new Vector());
        Bukkit.getScheduler().scheduleSyncDelayedTask(BridgeWars.getPlugin(), dropped::remove, despawn);

    }

    private void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        CraftPlayer craftplayer = (CraftPlayer) player;
        PlayerConnection connection = craftplayer.getHandle().playerConnection;
        ChatComponentText titleJSON = new ChatComponentText(title);
        ChatComponentText subtitleJSON = new ChatComponentText(subtitle);
        PacketPlayOutTitle duraPacket = new PacketPlayOutTitle(fadeIn, stay, fadeOut);
        PacketPlayOutTitle titlePacket = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.TITLE, titleJSON);
        PacketPlayOutTitle subtitlePacket = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.SUBTITLE, subtitleJSON);
        connection.sendPacket(duraPacket);
        connection.sendPacket(titlePacket);
        connection.sendPacket(subtitlePacket);
    }

    public ItemStack createItemStack(Material material, String name, Integer amount, Integer damage) {
        ItemStack item = new ItemStack(material, amount, damage.shortValue());

        ItemMeta meta = item.getItemMeta();
        if (meta instanceof Damageable) {
            ((Damageable) meta).damage(damage);
        }
        if (!name.equals("")) {
            meta.setDisplayName(name);
        }
        item.setItemMeta(meta);

        return item;
    }

    public boolean checkWorld(World world) {
        return world == worldBridge;
    }

    public String getVar(String name) {
        if ("time".equals(name)) {
            if (time >= 0) {
                return (int) Math.floor((double) time / 60) + ":" + (String.valueOf(time % 60).length() == 1 ? "0" : "") + time % 60;
            } else if (players.size() < 2) {
                return "Waiting";
            } else {
                return -time + "s";
            }
        } else if (goals.containsKey(name)) {
            return String.valueOf(goals.get(name));
        } else if ("event".equals(name)) {
            if (time < 0) {
                return "Start";
            }
            int tTil = 360 - time % 360;
            String tUntil = (int) Math.floor((double) tTil / 60) + ":" + (String.valueOf(tTil % 60).length() == 1 ? "0" : "") + tTil % 60;
            return "Lose Goal: &3" + tUntil;
        }
        return "";
    }

    public void shutdown() {
        isRunning = false;
        for (Player player : worldBridge.getPlayers()) {
            Hub.sendHub(player);
        }
        Bukkit.getServer().unloadWorld(worldBridge, false);
        FileUtils.deleteDirectory(new File("./" + worldBridge.getName()));
        timer.cancel();
    }

}
