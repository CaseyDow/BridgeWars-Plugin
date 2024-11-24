package me.solarlego.bridgewars;

import me.solarlego.bridgewars.bridgewars.BridgeGame;
import me.solarlego.bridgewars.commands.CommandPlay;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class BridgeWars extends JavaPlugin {

    private static BridgeWars instance;
    private ArrayList<BridgeGame> games;

    @Override
    public void onEnable() {
        instance = this;
        games = new ArrayList<>();
        Bukkit.getServer().getPluginManager().registerEvents(new CommandPlay(), this);

        saveDefaultConfig();
        File players = new File(getDataFolder(), "players.yml");
        players.getParentFile().mkdirs();
        try {
            players.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        File tools = new File(getDataFolder(), "tools.yml");
        tools.getParentFile().mkdirs();
        try {
            tools.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void onDisable() {
        for (BridgeGame game : games) {
            game.shutdown();
        }
    }
    
    public static BridgeWars getPlugin() {
        return instance;
    }

    public void updatePlayerFile(String path, Integer val) {
        FileConfiguration playersFile = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "players.yml"));

        playersFile.set(path, val + playersFile.getInt(path));
        playersFile.options().copyDefaults(true);
        try {
            playersFile.save(new File(getDataFolder(), "players.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<BridgeGame> getGames() {
        return games;
    }

    public void addGame(BridgeGame game) {
        this.games.add(game);
    }

}
