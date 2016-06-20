package net.dungeonrealms.game.mechanics;

import com.connorlinfoot.bountifulapi.BountifulAPI;
import lombok.Getter;
import net.dungeonrealms.API;
import net.dungeonrealms.DungeonRealms;
import net.dungeonrealms.game.achievements.Achievements;
import net.dungeonrealms.game.mastery.AsyncUtils;
import net.dungeonrealms.game.mastery.Utils;
import net.dungeonrealms.game.mechanics.generic.EnumPriority;
import net.dungeonrealms.game.mechanics.generic.GenericMechanic;
import net.dungeonrealms.game.mongo.DatabaseAPI;
import net.dungeonrealms.game.mongo.EnumData;
import net.dungeonrealms.game.mongo.EnumOperators;
import net.dungeonrealms.game.world.spawning.dungeons.DungeonMobCreator;
import net.dungeonrealms.game.world.teleportation.Teleportation;
import net.minecraft.server.v1_9_R2.Entity;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_9_R2.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.ItemStack;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by Nick on 10/19/2015.
 */
public class DungeonManager implements GenericMechanic {

    static DungeonManager instance = null;

    public static DungeonManager getInstance() {
        if (instance == null) {
            instance = new DungeonManager();
        }
        return instance;
    }

    private CopyOnWriteArrayList<DungeonObject> Dungeons = new CopyOnWriteArrayList<>();
    public static volatile ConcurrentHashMap<String, HashMap<Location, String>> instance_mob_spawns = new ConcurrentHashMap<>();
    @Getter
    private ConcurrentHashMap<String, Integer> players_Entering_Dungeon = new ConcurrentHashMap<>();

    public DungeonObject getDungeon(World world) {
        for (DungeonObject dungeon : Dungeons) {
            if (world.getName().equalsIgnoreCase(dungeon.getWorldName()))
                return dungeon;
        }
        return null;
    }

    @Override
    public EnumPriority startPriority() {
        return EnumPriority.ARCHBISHOPS;
    }

    @Override
    public void startInitialization() {
        Utils.log.info("[DUNGEONS] Loading Dungeon Mechanics ... STARTING");
        try {
            FileUtils.forceMkdir(new File(DungeonRealms.getInstance().getDataFolder() + File.separator + "/dungeons/"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        Bukkit.getScheduler().scheduleSyncRepeatingTask(DungeonRealms.getInstance(), () -> {
            for (Map.Entry<String, Integer> entry : players_Entering_Dungeon.entrySet()) {
                if (entry.getValue() > 1) {
                    players_Entering_Dungeon.put(entry.getKey(), (entry.getValue() - 1));
                } else {
                    players_Entering_Dungeon.remove(entry.getKey());
                }
            }
        }, 100L, 20L);

        Bukkit.getScheduler().scheduleAsyncRepeatingTask(DungeonRealms.getInstance(), () -> Dungeons.stream().forEach(dungeon -> dungeon.aliveMonsters.stream().forEach(mob -> {
            if (mob != null) {
                if (!mob.isAlive() || mob.dead) {
                    dungeon.aliveMonsters.remove(mob);
                    dungeon.killed = dungeon.killed + 1;
                }
            }
        })), 0, 10);

        Bukkit.getScheduler().scheduleSyncRepeatingTask(DungeonRealms.getInstance(), () -> Dungeons.stream().forEach(dungeonObject -> {
            int time = dungeonObject.getTime();
            dungeonObject.modifyTime(1);
            if (time < 10) {
                return;
            }
            if (Bukkit.getWorld(dungeonObject.getWorldName()) == null) {
                Dungeons.remove(dungeonObject);
                return;
            }
            int monstersAlive = dungeonObject.maxAlive - dungeonObject.killed;
            int maxAlive = dungeonObject.maxAlive;
            if (!dungeonObject.canSpawnBoss && maxAlive > 0 && monstersAlive > 0) {
                if (monstersAlive <= (maxAlive * 0.2)) {
                    dungeonObject.canSpawnBoss = true;
                    Bukkit.getWorld(dungeonObject.getWorldName()).getPlayers().stream().forEach(player -> {
                        if (player != null) {
                            if (API.getGamePlayer(player).isInDungeon()) {
                                player.sendMessage(ChatColor.RED.toString() + dungeonObject.type.getBossName() + ChatColor.RESET + ": Do you really wish to fight me?");
                            }
                        }
                    });
                }
            }

            if (Bukkit.getWorld(dungeonObject.worldName).getPlayers().size() <= 0) {
                removeInstance(dungeonObject);
                return;
            }

            if (dungeonObject.triedTeleportingOut) {
                dungeonObject.teleportPlayersOut(true);
                return;
            }

            switch (time) {
                // 2h 10 minutes
                case 7500:
                    removeInstance(dungeonObject);
                    break;
                // 2h
                case 7200:
                    Bukkit.getWorld(dungeonObject.getWorldName()).getPlayers().stream()
                            .forEach(player -> {
                                if (player != null) {
                                    if (API.getGamePlayer(player).isInDungeon()) {
                                        player.sendMessage(ChatColor.WHITE + "[" + ChatColor.GOLD
                                                + dungeonObject.type.getBossName() + ChatColor.WHITE + "]" + " " + ChatColor.RED
                                                + "This instance has reached it's max threshold, it will now terminate in (10) minutes.");
                                    }
                                }
                            });
                    break;
                // 1h30 minutes
                case 5400:
                    Bukkit.getWorld(dungeonObject.getWorldName()).getPlayers().stream()
                            .forEach(player -> {
                                if (player != null) {
                                    if (API.getGamePlayer(player).isInDungeon()) {
                                        player.sendMessage(ChatColor.WHITE + "[" + ChatColor.GOLD
                                                + dungeonObject.type.getBossName() + ChatColor.WHITE + "]" + " " + ChatColor.RED
                                                + "This instance has reached (90) minute marker!");
                                    }
                                }
                            });
                    break;
                // 1h
                case 3600:
                    Bukkit.getWorld(dungeonObject.getWorldName()).getPlayers().stream()
                            .forEach(player -> {
                                if (player != null) {
                                    if (API.getGamePlayer(player).isInDungeon()) {
                                        player.sendMessage(ChatColor.WHITE + "[" + ChatColor.GOLD
                                                + dungeonObject.type.getBossName() + ChatColor.WHITE + "]" + " " + ChatColor.RED
                                                + "This instance has reached (60) minute marker!");
                                    }
                                }
                            });
                    break;
                // 30 minutes
                case 1800:
                    Bukkit.getWorld(dungeonObject.getWorldName()).getPlayers().stream()
                            .forEach(player -> {
                                if (player != null) {
                                    if (API.getGamePlayer(player).isInDungeon()) {
                                        player.sendMessage(ChatColor.WHITE + "[" + ChatColor.GOLD
                                                + dungeonObject.type.getBossName() + ChatColor.WHITE + "]" + " " + ChatColor.RED
                                                + "This instance has reached (30) minute marker!");
                                    }
                                }
                            });
                    break;
                // 15 minutes
                case 900:
                    Bukkit.getWorld(dungeonObject.getWorldName()).getPlayers().stream()
                            .forEach(player -> {
                                if (player != null) {
                                    if (API.getGamePlayer(player).isInDungeon()) {
                                        player.sendMessage(ChatColor.WHITE + "[" + ChatColor.GOLD
                                                + dungeonObject.type.getBossName() + ChatColor.WHITE + "]" + " " + ChatColor.RED
                                                + "This instance has reached (15) minute marker!");
                                    }
                                }
                            });
                    break;
            }
            updateDungeonBoard(dungeonObject);
        }), 0, 20L);
        Utils.log.info("[DUNGEONS] Finished Loading Dungeon Mechanics ... OKAY");
    }

    @Override
    public void stopInvocation() {

    }

    /**
     * Update the dungeon scoreboard.
     *
     * @param dungeonObject
     * @since 1.0
     */
    private void updateDungeonBoard(DungeonObject dungeonObject) {
        Bukkit.getWorld(dungeonObject.getWorldName()).getPlayers().forEach(player -> {
            if (player != null) {
                if (API.getGamePlayer(player) != null) {
                    if (API.getGamePlayer(player).isInDungeon()) {
                        BountifulAPI.sendActionBar(player, ChatColor.AQUA + "Time: " + ChatColor.WHITE + ChatColor.GOLD
                                + String.valueOf(dungeonObject.getTime() / 60) + "/120" + " " + ChatColor.AQUA + "Alive: " + ChatColor.WHITE + (dungeonObject.maxAlive - dungeonObject.killed) + ChatColor.GRAY
                                + "/" + ChatColor.RED + dungeonObject.maxAlive);
                    }
                }
            }
        });
    }

    /**
     * Removes the instance dungeon from EVERYTHING.
     *
     * @param dungeonObject The dungeon object.
     * @since 1.0
     */
    public void removeInstance(DungeonObject dungeonObject) {
        Bukkit.getWorld(dungeonObject.getWorldName()).getPlayers().forEach(player -> {
            if (player != null) {
                if (Bukkit.getPlayer(player.getUniqueId()) != null) {
                    if (API.getGamePlayer(player) != null) {
                        if (API.getGamePlayer(player).isInDungeon()) {
                            player.sendMessage(ChatColor.RED.toString() + dungeonObject.type.getBossName() + ChatColor.RESET + ": You have failed, Adventurers.");
                            player.teleport(Teleportation.Cyrennica);
                            for (ItemStack stack : player.getInventory().getContents()) {
                                if (stack != null && stack.getType() != Material.AIR) {
                                    if (isDungeonItem(stack)) {
                                        player.getInventory().remove(stack);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        });
        Bukkit.getWorlds().remove(Bukkit.getWorld(dungeonObject.getWorldName()));
        Utils.log.info("[DUNGEONS] Removing world: " + dungeonObject.getWorldName() + " from worldList().");
        Bukkit.unloadWorld(dungeonObject.getWorldName(), false);
        Utils.log.info("[DUNGEONS] Unloading world: " + dungeonObject.getWorldName() + " in preparation for deletion!");
        Bukkit.getScheduler().cancelTask(dungeonObject.spawningTaskID);
        AsyncUtils.pool.submit(() -> {
            deleteFolder(new File(dungeonObject.worldName));
            Utils.log.info("[DUNGEONS] Deleted world: " + dungeonObject.getWorldName() + " final stage.");
            deleteFolder(new File("plugins/WorldGuard/worlds/" + dungeonObject.worldName));
            Utils.log.info("[DUNGEONS] Removing WorldGuard: " + dungeonObject.getWorldName() + " final stage.");
            if (Dungeons.contains(dungeonObject)) {
                dungeonObject.cleanup();
                Dungeons.remove(dungeonObject);
            }
        });
    }

    /**
     * @param type       DungeonType
     * @param playerList List of players to enter!
     * @since 1.0
     */
    public void createNewInstance(DungeonType type, Map<Player, Boolean> playerList, String instanceName) {
        if (!instance_mob_spawns.containsKey(instanceName)) {
            loadDungeonMobSpawns(instanceName);
        }
        DungeonObject dungeonObject = new DungeonObject(type, 0, playerList, "DUNGEON_" + String.valueOf(System.currentTimeMillis() / 1000L), instanceName);
        Dungeons.add(dungeonObject);
        dungeonObject.load();
    }

    public boolean canCreateInstance() {
        //TODO: Increase on non US-1 shards.
        return Dungeons.size() < 3;
    }

    /**
     * Will extract a players realm .zip to the correct folder.
     *
     * @param zipFile
     * @since 1.0
     */
    private void unZip(ZipFile zipFile, DungeonObject dungeonObject) {
        Utils.log.info("[DUNGEON] Unzipping instance for " + dungeonObject.getWorldName());
        new File(dungeonObject.getWorldName()).mkdir();
        try {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                File entryDestination = new File(dungeonObject.getWorldName(), entry.getName());
                if (entry.isDirectory())
                    entryDestination.mkdirs();
                else {
                    entryDestination.getParentFile().mkdirs();
                    InputStream in = zipFile.getInputStream(entry);
                    OutputStream out = new FileOutputStream(entryDestination);
                    IOUtils.copy(in, out);
                    IOUtils.closeQuietly(in);
                    out.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                zipFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public class DungeonObject {

        private DungeonType type;
        private Integer time;
        private Map<Player, Boolean> playerList;
        private String worldName;
        public CopyOnWriteArrayList<Entity> aliveMonsters = new CopyOnWriteArrayList<>();
        public boolean canSpawnBoss = false;
        public int tier;
        public int maxAlive = 0;
        public int killed = 0;
        private ConcurrentHashMap<Entity, Location> toSpawn = new ConcurrentHashMap<>();
        String instanceName;
        int spawningTaskID;
        public boolean beingRemoved;
        public boolean hasBossSpawned;
        public int keysDropped;
        public boolean triedTeleportingOut;

        DungeonObject(DungeonType type, Integer time, Map<Player, Boolean> playerList, String worldName, String instanceName) {
            this.type = type;
            this.time = time;
            this.playerList = playerList;
            this.worldName = worldName;
            this.instanceName = instanceName;
            switch (type) {
                case BANDIT_TROVE:
                    tier = 1;
                    break;
                case VARENGLADE:
                    tier = 3;
                    break;
                case THE_INFERNAL_ABYSS:
                    tier = 4;
                    break;
                default:
                    break;
            }
        }

        public DungeonType getType() {
            return type;
        }

        public Integer getTime() {
            return time;
        }

        public Map<Player, Boolean> getPlayerList() {
            return playerList;
        }

        public String getWorldName() {
            return worldName;
        }

        public void modifyTime(int second) {
            time += second;
        }

        void load() {
            AsyncUtils.pool.submit(() -> {
                try {
                    unZip(new ZipFile(DungeonRealms.getInstance().getDataFolder() + type.getLocation()), this);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            Bukkit.getScheduler().scheduleSyncDelayedTask(DungeonRealms.getInstance(), () -> {
                loadInWorld(getWorldName(), getPlayerList(), getType());
            }, 20L);
        }

        void cleanup() {
            playerList.clear();
            aliveMonsters.clear();
            toSpawn.clear();
        }

        /**
         *
         */
        public void teleportPlayersOut(boolean secondTry) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(DungeonRealms.getInstance(), () -> {
                Bukkit.getWorld(worldName).getPlayers().stream().filter(p -> p != null && p.isOnline()).forEach(player -> {
                    if (API.getGamePlayer(player) != null && API.getGamePlayer(player).isInDungeon()) {
                        switch (getType()) {
                            case BANDIT_TROVE:
                                Achievements.getInstance().giveAchievement(player.getUniqueId(), Achievements.EnumAchievements.BANDIT_TROVE);
                                break;
                            case VARENGLADE:
                                Achievements.getInstance().giveAchievement(player.getUniqueId(), Achievements.EnumAchievements.VARENGLADE);
                                break;
                            case THE_INFERNAL_ABYSS:
                                Achievements.getInstance().giveAchievement(player.getUniqueId(), Achievements.EnumAchievements.INFERNAL_ABYSS);
                                break;
                            default:
                                break;
                        }
                        player.teleport(Teleportation.Cyrennica);
                        triedTeleportingOut = true;
                        for (ItemStack stack : player.getInventory().getContents()) {
                            if (stack != null && stack.getType() != Material.AIR) {
                                if (isDungeonItem(stack)) {
                                    player.getInventory().remove(stack);
                                }
                            }
                        }
                    }
                });
            }, 15 * 20L);
            if (!secondTry)
                Bukkit.getWorld(worldName).getPlayers().stream().filter(p -> p != null && p.isOnline()).forEach(p -> p.sendMessage(ChatColor.YELLOW + "You will be teleported out in 15 seconds..."));
        }

        /**
         *
         */
        public void giveShards() {
            int shardsToGive = 100;

            switch (tier) {
                case 1:
                    shardsToGive = 750 + new Random().nextInt(150);
                    break;
                case 2:
                    shardsToGive = 900 + new Random().nextInt(300);
                    break;
                case 3:
                    shardsToGive = 1000 + new Random().nextInt(500);
                    break;
                case 4:
                    shardsToGive = 1200 + new Random().nextInt(750);
                    break;
                case 5:
                    shardsToGive = 1500 + new Random().nextInt(1000);
                    break;
            }

            for (Player p : Bukkit.getWorld(worldName).getPlayers()) {
                p.sendMessage(API.getTierColor(tier) + "You have gained " + ChatColor.UNDERLINE + shardsToGive
                        + " Portal Shards" + API.getTierColor(tier) + " for completing this Dungeon.");
                switch (tier) {
                    case 1:
                        DatabaseAPI.getInstance().update(p.getUniqueId(), EnumOperators.$INC, EnumData.PORTAL_SHARDS_T1,
                                shardsToGive, true);
                        break;
                    case 2:
                        DatabaseAPI.getInstance().update(p.getUniqueId(), EnumOperators.$INC, EnumData.PORTAL_SHARDS_T2,
                                shardsToGive, true);
                        break;
                    case 3:
                        DatabaseAPI.getInstance().update(p.getUniqueId(), EnumOperators.$INC, EnumData.PORTAL_SHARDS_T3,
                                shardsToGive, true);
                        break;
                    case 4:
                        DatabaseAPI.getInstance().update(p.getUniqueId(), EnumOperators.$INC, EnumData.PORTAL_SHARDS_T4,
                                shardsToGive, true);
                        break;
                    case 5:
                        DatabaseAPI.getInstance().update(p.getUniqueId(), EnumOperators.$INC, EnumData.PORTAL_SHARDS_T5,
                                shardsToGive, true);
                        break;
                    default:
                        DatabaseAPI.getInstance().update(p.getUniqueId(), EnumOperators.$INC, EnumData.PORTAL_SHARDS_T1,
                                shardsToGive, true);
                        break;

                }
            }
            Bukkit.getScheduler().scheduleSyncDelayedTask(DungeonRealms.getInstance(), () -> {
                teleportPlayersOut(true);
                if (Dungeons.contains(DungeonManager.getInstance().getDungeon(Bukkit.getWorld(worldName)))) {
                    DungeonManager.getInstance().getDungeon(Bukkit.getWorld(worldName)).cleanup();
                    Dungeons.remove(DungeonManager.getInstance().getDungeon(Bukkit.getWorld(worldName)));
                }
            }, 600L);
        }
    }

    /**
     * Loads the nonExistent world and teleports all players to the
     * spawnLocation of that world.
     *
     * @param worldName  Name of the Dungeon world(DUNGEON_unixTime)
     * @param playerList List of players going to Dungeon.
     * @since 1.0
     */
    private void loadInWorld(String worldName, Map<Player, Boolean> playerList, DungeonType type) {
        /*
         * Only creates a world if the contents of a world don't already exist.
		 * This method loadInWorld() is called in the actual object load().
		 */
        AsyncUtils.pool.submit(() -> {
            if (new File(worldName + "/" + "uid.dat").exists()) {
                // Delete that shit.
                new File(worldName + "/" + "uid.dat").delete();
            }
            deleteFolder(new File(worldName + "/players"));
            try {
                FileUtils.copyDirectory(new File("plugins/WorldGuard/worlds/" + type.getWorldGuardName()), new File("plugins/WorldGuard/worlds/" + worldName));
            } catch (IOException e) {
                e.printStackTrace();
            }
            Utils.log.info("Completed setup of Dungeon: " + worldName);
        });

        Bukkit.getScheduler().scheduleSyncDelayedTask(DungeonRealms.getInstance(), () -> {
            World w = Bukkit.getServer().createWorld(new WorldCreator(worldName));
            w.setKeepSpawnInMemory(true);
            w.setAutoSave(false);
            w.setPVP(false);
            w.setStorm(false);
            w.setMonsterSpawnLimit(600);
            w.setGameRuleValue("doFireTick", "false");
            w.setGameRuleValue("randomTickSpeed", "0");
            Bukkit.getWorlds().add(w);

            if (!instance_mob_spawns.containsKey(this.getDungeon(w).instanceName)) {
                loadDungeonMobSpawns(this.getDungeon(w).instanceName);
            }
            DungeonObject object = this.getDungeon(w);
            object.spawningTaskID = Bukkit.getScheduler().scheduleSyncDelayedTask(DungeonRealms.getInstance(), () -> {
                net.minecraft.server.v1_9_R2.World world = ((CraftWorld) w).getHandle();
                object.toSpawn = DungeonMobCreator.getEntitiesToSpawn(object.instanceName, w);
                object.maxAlive = object.toSpawn.size();
                Bukkit.getScheduler().scheduleSyncRepeatingTask(DungeonRealms.getInstance(), () -> {
                    for (Map.Entry<Entity, Location> entry : object.toSpawn.entrySet()) {
                        Location location = entry.getValue();
                        location.setWorld(w);
                        if (!API.getNearbyPlayers(location, 50).isEmpty()) {
                            final Entity entity = entry.getKey();
                            entity.setLocation(location.getX(), location.getY(), location.getZ(), 1, 1);
                            world.addEntity(entity, CreatureSpawnEvent.SpawnReason.CUSTOM);
                            entity.setLocation(location.getX(), location.getY(), location.getZ(), 1, 1);
                            entity.setCustomNameVisible(true);
                            if (entity.isAlive()) {
                                object.aliveMonsters.add(entity);
                                object.toSpawn.remove(entity);
                            }
                        }
                    }
                }, 0L, 10L);
            }, 60L);

            Bukkit.getScheduler().scheduleSyncDelayedTask(DungeonRealms.getInstance(), () -> playerList.keySet().stream().forEach(player -> {
                if (playerList.get(player)) {
                    String locationAsString = "-367,86,390,0,0"; // Cyrennica
                    if (player.getWorld().equals(Bukkit.getWorlds().get(0))) {
                        locationAsString = player.getLocation().getX() + "," + (player.getLocation().getY() + 0.5) + "," + player.getLocation().getZ() + "," + player.getLocation().getYaw() + "," + player.getLocation().getPitch();
                    }
                    DatabaseAPI.getInstance().update(player.getUniqueId(), EnumOperators.$SET, EnumData.CURRENT_LOCATION, locationAsString, true);
                    player.teleport(w.getSpawnLocation());
                    player.sendMessage(ChatColor.RED.toString() + object.type.getBossName() + ChatColor.RESET + ": How dare you enter my domain!");
                } else {
                    player.sendMessage(ChatColor.LIGHT_PURPLE.toString() + "<" + ChatColor.BOLD + "P" + ChatColor.LIGHT_PURPLE + ">" + ChatColor.GRAY + " "
                            + "Your party has started the " + ChatColor.LIGHT_PURPLE + ChatColor.UNDERLINE + object.getType().name().replaceAll("_", " ") + ChatColor.RESET + ChatColor.GRAY + " Dungeon.");
                    if (API.isInSafeRegion(player.getLocation())) {
                        player.sendMessage(ChatColor.GRAY + "Due to your location, you can join them instantly via" + ChatColor.GREEN + ChatColor.UNDERLINE + "/djoin");
                    }
                    //Player isn't nearby so shouldn't be teleported (BUT) can still enter the dungeon at a later date.
                }
            }), 150L);
        }, 20L);
    }

    private void loadDungeonMobSpawns(String instanceName) {
        for (File file : new File("plugins/DungeonRealms/dungeonSpawns/").listFiles()) {
            String fileName = file.getName().replaceAll(".dat", "");
            if (fileName.equalsIgnoreCase(instanceName)) {
                DungeonRealms.getInstance().getLogger().info("Found Dungeon Spawn Template for " + instanceName);
                HashMap<Location, String> dungeonMobData = new HashMap<>();
                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    for (String line; (line = br.readLine()) != null; ) {
                        if (line.equalsIgnoreCase("null")) {
                            continue;
                        }
                        if (line.contains("=")) {
                            String[] coordinates = line.split("=")[0].split(",");
                            Location location = new Location(Bukkit.getWorlds().get(0), Double.parseDouble(coordinates[0]), Double.parseDouble(coordinates[1]),
                                    Double.parseDouble(coordinates[2]));
                            String spawnData = line.split("=")[1];
                            dungeonMobData.put(location, spawnData);
                        }
                    }
                    br.close();
                    instance_mob_spawns.put(instanceName, dungeonMobData);
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        }
    }

    private void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) { // some JVMs return null for empty dirs
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }

    public boolean isDungeonItem(ItemStack stack) {
        if (stack != null && stack.getType() != Material.AIR && stack.hasItemMeta() && stack.getItemMeta().hasLore()) {
            List<String> itemLore = stack.getItemMeta().getLore();
            for (String string : itemLore) {
                if (string.toLowerCase().contains("dungeon item")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Enum type of dungeons includes, zip locations & bossNames
     *
     * @since 1.0
     */
    public enum DungeonType {
        BANDIT_TROVE("Mayel the Cruel", "/dungeons/banditTrove.zip", "banditTrove"),
        VARENGLADE("Burick The Fanatic", "/dungeons/varenglade.zip", "varenglade"),
        THE_INFERNAL_ABYSS("The Infernal Abyss", "/dungeons/theInfernalAbyss.zip", "infernalAbyss");

        /**
         * WIP Dungeons, including The Depths of Aceron and The Crimson
         * Monastery.
         */

        private String bossName;
        private String location;
        private String worldGuardName;

        DungeonType(String bossName, String location, String worldGuardName) {
            this.bossName = bossName;
            this.location = location;
            this.worldGuardName = worldGuardName;
        }

        public String getBossName() {
            return bossName;
        }

        public String getLocation() {
            return location;
        }

        public String getWorldGuardName() {
            return worldGuardName;
        }
    }

}
