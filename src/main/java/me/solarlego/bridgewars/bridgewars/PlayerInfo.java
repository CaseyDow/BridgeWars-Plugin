package me.solarlego.bridgewars.bridgewars;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

public class PlayerInfo {

    public static final HashMap<String, String> teamColors = new HashMap<>();

    private final BridgeGame game;
    private final UUID player;
    private String team;
    private Color teamColor;
    private String teamPrefix;
    private String pickaxe = "None";
    private String axe = "None";
    private boolean shears = false;
    private String armor = "Leather";
    private int kills = 0;
    private int finals = 0;
    private int goals = 0;
    private int coins = 0;
    private Location spawnLoc;
    private UUID dmgPlayer = null;
    private int dmgTime = 0;
    private boolean isDead = false;
    private boolean spec = false;

    public PlayerInfo(BridgeGame bw, Player p) {
        game = bw;
        player = p.getUniqueId();
        spawnLoc = new Location(game.worldBridge, 0.5, 101, 58.5, 180, 0);
        String[] config = game.yml.getConfigurationSection("teams").getKeys(false).toArray(new String[0]);
        if (config.length > 0) {
            setTeam(config[0].substring(0, 1).toUpperCase() + config[0].substring(1));
        }
    }

    public PlayerInfo(BridgeGame bw, Player p, Boolean spec) {
        game = bw;
        player = p.getUniqueId();
        spawnLoc = new Location(game.worldBridge, 0.5, 101, 58.5, 180, 0);
        if (spec) {
            team = "Spectator";
            isDead = true;
            this.spec = true;
            teamPrefix = "7";
        } else {
            new PlayerInfo(bw, p);
        }
    }

    public void setTeam(String team) {
        this.team = team;
        ConfigurationSection tInfo = game.yml.getConfigurationSection("teams." + team.toLowerCase());
        spawnLoc = new Location(game.worldBridge, tInfo.getDouble("x"), tInfo.getDouble("y"), tInfo.getDouble("z"), (float) tInfo.getDouble("yaw"), 0);
        teamColor = Color.fromRGB(tInfo.getInt("color"));
        teamPrefix = tInfo.getString("prefix");
    }

    public String getTeam() {
        return team;
    }

    public String getPickaxe() {
        return pickaxe;
    }

    public void setPickaxe(String pickaxe) {
        this.pickaxe = pickaxe;
    }

    public String getAxe() {
        return axe;
    }

    public void setAxe(String axe) {
        this.axe = axe;
    }

    public boolean hasShears() {
        return shears;
    }

    public void setShears(boolean shears) {
        this.shears = shears;
    }

    public int getKills() {
        return kills;
    }

    public void addKills(int kills) {
        this.kills += kills;
    }

    public int getCoins() {
        return coins;
    }

    public void addCoins(int coins) {
        this.coins += coins;
    }

    public Location getSpawnLoc() {
        return spawnLoc;
    }

    public Player getDmgPlayer() {
        return Bukkit.getPlayer(dmgPlayer);
    }

    public void setDmgPlayer(Player dmgPlayer) {
        this.dmgPlayer = dmgPlayer.getUniqueId();
    }

    public int getDmgTime() {
        return dmgTime;
    }

    public void setDmgTime(int dmgTime) {
        this.dmgTime = dmgTime;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(player);
    }

    public String getArmor() {
        return armor;
    }

    public void setArmor(String armor) {
        this.armor = armor;
    }

    public Color getTeamColor() {
        return teamColor;
    }

    public boolean isDead() {
        return isDead;
    }

    public void setDead(boolean dead) {
        isDead = dead;
    }

    public String getTeamPrefix() {
        return teamPrefix;
    }

    public int getFinals() {
        return finals;
    }

    public void addFinals(int finals) {
        this.finals += finals;
    }

    public int getGoals() {
        return goals;
    }

    public void addGoals(int goals) {
        this.goals += goals;
    }

    public boolean isSpec() {
        return spec;
    }
}
