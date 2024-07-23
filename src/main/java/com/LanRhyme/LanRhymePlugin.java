package com.LanRhyme;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class LanRhymePlugin extends JavaPlugin implements CommandExecutor {

    // 定义一个Map来存储玩家的点券数和名称
    private Map<UUID, Integer> pointpay_consumeMap = new HashMap<>();
    private Map<UUID, String> playerNameMap = new HashMap<>();
    // 定义一个FileConfiguration来存储点券数据
    private FileConfiguration pointpay_consumeData;
    // 定义一个File来存储点券数据文件
    private File pointpay_consumeDataFile;

    @Override
    public void onEnable() {
        // 初始化数据文件
        pointpay_consumeDataFile = new File(getDataFolder(), "pointpay_consumeData.yml");
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        loadPointpay_consumeData();

        // 注册命令
        Objects.requireNonNull(this.getCommand("addpointpay_consume")).setExecutor(this);
        Objects.requireNonNull(this.getCommand("showleaderboard")).setExecutor(this);
        Objects.requireNonNull(this.getCommand("refreshleaderboard")).setExecutor(this);
    }

    @Override
    public void onDisable() {
        // 保存数据
        savePointpay_consumeData();
    }

    // 加载点券数据
    private void loadPointpay_consumeData() {
        pointpay_consumeData = new YamlConfiguration();
        try {
            if (!pointpay_consumeDataFile.exists()) {
                pointpay_consumeDataFile.createNewFile();
            }
            pointpay_consumeData.load(pointpay_consumeDataFile);
            for (String key : pointpay_consumeData.getKeys(false)) {
                UUID uuid = UUID.fromString(key);
                int pointpay_consume = pointpay_consumeData.getInt(key);
                pointpay_consumeMap.put(uuid, pointpay_consume);
                String playerName = pointpay_consumeData.getString(uuid.toString() + ".name");
                playerNameMap.put(uuid, playerName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 保存点券数据
    private void savePointpay_consumeData() {
        try {
            for (Map.Entry<UUID, Integer> entry : pointpay_consumeMap.entrySet()) {
                pointpay_consumeData.set(entry.getKey().toString(), entry.getValue());
                pointpay_consumeData.set(entry.getKey().toString() + ".name", playerNameMap.get(entry.getKey()));
            }
            pointpay_consumeData.save(pointpay_consumeDataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("addpointpay_consume")) {
            if (args.length < 2) {
                sender.sendMessage("Usage: /addpointpay_consume <player> <amount>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("未找到玩家");
                return true;
            }
            int amount;
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage("无效的数字格式");
                return true;
            }
            addPointpay_consume(target.getUniqueId(), amount);
            sender.sendMessage(target.getName() + " 已被给予 " + amount + " 点券点数.");
            return true;
        } else if (command.getName().equalsIgnoreCase("showleaderboard")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("This command can only be used by a player.");
                return true;
            }
            Player player = (Player) sender;
            showLeaderboard(player);
            return true;
        } else if (command.getName().equalsIgnoreCase("refreshleaderboard")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("This command can only be used by a player.");
                return true;
            }
            Player player = (Player) sender;
            refreshLeaderboard(player);
            return true;
        }
        return false;
    }

    // 添加点券
    private void addPointpay_consume(UUID uuid, int amount) {
        pointpay_consumeMap.put(uuid, pointpay_consumeMap.getOrDefault(uuid, 0) + amount);
        playerNameMap.put(uuid, Bukkit.getPlayer(uuid).getName());
        savePointpay_consumeData(); // 保存更新后的数据
    }

    // 显示排行榜
    private void showLeaderboard(Player player) {
        ArmorStand armorStand = getArmorStand(player);
        if (armorStand == null) {
            armorStand = player.getWorld().spawn(player.getLocation().add(0, 2, 0), ArmorStand.class);
            armorStand.setVisible(false);
            armorStand.setGravity(false);
            armorStand.setCustomNameVisible(true);
        }

        String leaderboard = buildLeaderboardString();
        armorStand.setCustomName(ChatColor.translateAlternateColorCodes('&', leaderboard));
    }

    // 刷新排行榜
    private void refreshLeaderboard(Player player) {
        ArmorStand armorStand = getArmorStand(player);
        if (armorStand != null) {
            String leaderboard = buildLeaderboardString();
            armorStand.setCustomName(ChatColor.translateAlternateColorCodes('&', leaderboard));
        }
    }

    // 获取排行榜上的ArmorStand
    private ArmorStand getArmorStand(Player player) {
        for (ArmorStand armorStand : player.getWorld().getEntitiesByClass(ArmorStand.class)) {
            if (armorStand.getCustomName() != null && armorStand.getCustomName().startsWith("历史充值点券排行榜:")) {
                return armorStand;
            }
        }
        return null;
    }

    // 构建排行榜字符串
    private String buildLeaderboardString() {
        StringBuilder leaderboard = new StringBuilder("历史充值点券排行榜:\n");
        int rank = 1;
        for (Map.Entry<UUID, Integer> entry : pointpay_consumeMap.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(10)
                .toList()) {
            String playerName = playerNameMap.get(entry.getKey());
            if (playerName != null) {
                leaderboard.append(rank).append(". ").append(playerName).append(": ").append(entry.getValue()).append(" 点券点数\n");
                if (rank == 1) {
                    leaderboard.append("第一名: ").append(playerName).append(" - ").append(entry.getValue()).append(" 点券点数\n");
                }
                rank++;
            }
        }
        return leaderboard.toString();
    }

}

