package me.c0wg0d.caketime;

import be.maximvdw.titlemotd.ui.Title;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class CakeTime extends JavaPlugin implements Listener {

    private String VERSION = "1.0";
    private String WORLD_NAME = "CakeTimeWorld";
    private Scoreboard board;
    private Objective pregameObjective;
    private Objective gameObjective;
    private Team Ghosts;
    private String GAME_DESCRIPTION;
    private List<ItemStack> GOALS;
    private int MIN_PLAYERS = 1;
    private int SECONDS_BEFORE_GAME_STARTS = 60;
    private int SECONDS_AFTER_GAME_ENDS = 60;
    private int MAX_SECONDS_PER_GAME = 1200;
    private String pregameTimerDisplay = ChatColor.GREEN + "Starting in: ";
    private String gameTimerDisplay = ChatColor.GREEN + "Time: ";
    private String numPlayersScoreDisplay = ChatColor.GREEN + "Players: ";
    private Timer pregameTimer = null;
    private Timer gameTimer = null;
    private Timer maxGameTimeTimer = null;
    private Timer restartTimer = null;
    private List<Player> participatingPlayers;
    private Title winnerTitle;
    private Title loserTitle;
    private boolean isGameStarted;
    private boolean hasWinner = false;

    private SettingsManager settings = SettingsManager.getInstance();

    public void onEnable() {
        // Settings
        settings.init(this);
        MIN_PLAYERS = settings.getConfig().getInt("general.minPlayers");
        SECONDS_BEFORE_GAME_STARTS = getConfig().getInt("general.secondsBeforeGameStarts");
        SECONDS_AFTER_GAME_ENDS = getConfig().getInt("general.secondsAfterGameEnds");
        MAX_SECONDS_PER_GAME = getConfig().getInt("general.maxSecondsPerGame");
        Set<String> gameList = settings.getConfig().getConfigurationSection("games").getKeys(false);

        String gameSettings = gameList.toArray()[(new Random()).nextInt(gameList.size())].toString();

        GAME_DESCRIPTION = gameSettings;

        if (settings.getConfig().contains("games." + gameSettings + ".minPlayers")) {
            MIN_PLAYERS = settings.getConfig().getInt("general.minPlayers");
        }
        if (settings.getConfig().contains("games." + gameSettings + ".secondsBeforeGameStarts")) {
            SECONDS_BEFORE_GAME_STARTS = getConfig().getInt("games." + gameSettings + ".secondsBeforeGameStarts");
        }
        if (settings.getConfig().contains("games." + gameSettings + ".secondsAfterGameEnds")) {
            SECONDS_AFTER_GAME_ENDS = getConfig().getInt("games." + gameSettings + ".secondsAfterGameEnds");
        }
        if (settings.getConfig().contains("games." + gameSettings + ".maxSecondsPerGame")) {
            MAX_SECONDS_PER_GAME = getConfig().getInt("games." + gameSettings + ".maxSecondsPerGame");
        }

        // Setup goals
        GOALS = new ArrayList<ItemStack>();
        List<String> goalMaterials = settings.getConfig().getStringList("games." + gameSettings + ".goal");
        for (String goalMaterial : goalMaterials) {
            String[] split = goalMaterial.split(":");
            // check broken config settings
            if (split.length == 3) {
                split[0] = split[0] + ":" + split[1];
                split[1] = split[2];
            }
            Bukkit.getLogger().info("item: [" + split[0] + "] amount: [" + Integer.parseInt(split[1]) + "]");
            ItemStack goal = new ItemStack(Material.getMaterial(split[0].toUpperCase()));
            goal.setAmount(Integer.parseInt(split[1]));
            GOALS.add(goal);
        }

        // Events
        Bukkit.getServer().getPluginManager().registerEvents(this, this);

        // Delete old world
        File worldFolder = new File(Bukkit.getServer().getWorldContainer().getAbsolutePath(), WORLD_NAME);
        File worldDat = new File(worldFolder, "level.dat");
        if (!worldDat.exists()) {
            Bukkit.getLogger().info("===== [" + worldDat + "] does not exist!");
        } else {
            Bukkit.getServer().unloadWorld("CakeTimeWorld", false);
            Bukkit.getLogger().info("===== Deleting [" + worldFolder + "]!");
            deleteDirectory(worldFolder);
        }

        // Create new world
        WorldCreator wc = new WorldCreator(WORLD_NAME);
        Long seed = (new Random()).nextLong();
        wc.seed(seed);
        if (Bukkit.getServer().getPluginManager().getPlugin("TerrainControl") != null) {
            wc.generator("TerrainControl");
        }
        wc.createWorld();

        // Generate chunks
        int minX = settings.getConfig().getInt("general.minX");
        int maxX = settings.getConfig().getInt("general.maxX");
        int minZ = settings.getConfig().getInt("general.minZ");
        int maxZ = settings.getConfig().getInt("general.maxZ");
        World world = Bukkit.getServer().getWorld(WORLD_NAME);
        Bukkit.getLogger().info("Loading chunks for new map, this may take a while.");
        for (int x = minX; x < maxX; x = x + 16) {
            for (int z = minZ; z < maxZ; z = z + 16) {
                int y = world.getHighestBlockYAt(x, z);
                world.getChunkAt(new Location(Bukkit.getServer().getWorld(WORLD_NAME), x, y, z)).load();

            }
        }
        Bukkit.getLogger().info("Loading chunks complete.");

        // Set world border using Minecraft 1.8
        int borderSize = Math.abs(minX) + Math.abs(maxX);
        Bukkit.getLogger().info("Setting worldborder using: /worldborder set " + borderSize);
        Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), "minecraft:worldborder center 0 0");
        Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), "minecraft:worldborder set " + borderSize);

        // Setup scoreboards
        board = Bukkit.getServer().getScoreboardManager().getNewScoreboard();
        pregameObjective = board.registerNewObjective("CakeTime", "dummy");
        pregameObjective.setDisplayName(GAME_DESCRIPTION);
        pregameObjective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Create Ghosts team for spectators
        Ghosts = board.registerNewTeam("Ghost");
        Ghosts.setCanSeeFriendlyInvisibles(true);

        // Start pregame timer
        startPreGameTimer();
        isGameStarted = false;

        participatingPlayers = new ArrayList<Player>();

        Bukkit.getServer().getLogger().info(this.getDescription().getName() + " v"
                + this.getDescription().getVersion() + " enabled.");
    }

    public void onDisable() {
        Bukkit.getServer().getLogger().info(this.getDescription().getName() + " v"
                + this.getDescription().getVersion() + " disabled.");
    }

    public boolean onCommand(CommandSender sender, Command cmd,
                             String commandLabel, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("console sent message");
            return true;
        }

        Player p = (Player) sender;

        if (cmd.getName().equalsIgnoreCase("caketime")) {
            p.sendMessage(ChatColor.DARK_PURPLE + "CakeTime version " + VERSION);
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("players")) {
            String players = ChatColor.GRAY.toString();
            for (Player onlinePlayer : Bukkit.getServer().getOnlinePlayers()) {
                if (participatingPlayers.contains(onlinePlayer)) {
                    players += ChatColor.WHITE;
                }
                players += onlinePlayer.getName() + ChatColor.GRAY + ", ";
            }
            players = players.substring(0, players.length() - 2);
            p.sendMessage(ChatColor.GRAY + "Players: " + players);
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("cleartitle")) {
            Title titleClear = new Title(" ", " ", 0, 0, 0);
            titleClear.setTitleColor(ChatColor.GREEN);
            titleClear.setSubtitleColor(ChatColor.YELLOW);
            titleClear.send(p);
        }

        if (cmd.getName().equalsIgnoreCase("goal") || cmd.getName().equalsIgnoreCase("objective")) {
            String msg = ChatColor.WHITE + "You need to find or craft: ";
            for (ItemStack goal : GOALS) {
                msg = msg + ChatColor.GREEN + goal.getAmount() + " " + goal.getType() + ", ";
            }

            p.sendMessage(msg.substring(0, msg.length() - 1));
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("goto")) {
            if (args.length < 1) {
                p.sendMessage(ChatColor.DARK_RED + "You need to specify what player to teleport to!");
                return true;
            }
            if (participatingPlayers.contains(p)) {
                p.sendMessage(ChatColor.DARK_RED + "You cannot teleport to players while playing the game!");
                return true;
            }
            Player toPlayer = Bukkit.getPlayer(args[0]);
            p.teleport(toPlayer);
            return true;
        }

        return true;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        resetPlayer(p);
        Location spawn = new Location(Bukkit.getWorld(WORLD_NAME), 0, 156, 0);
        Bukkit.getServer().getWorld(WORLD_NAME).getChunkAt(spawn).load();
        p.teleport(spawn);
        p.setScoreboard(board);
        if (isGameStarted) {
            for (Player participant : participatingPlayers) {
                participant.hidePlayer(p);
            }
        }
    }

    @EventHandler
    public void onPlayerPickupItemEvent(PlayerPickupItemEvent e) {
        if (hasWinner) {
            return;
        }

        Player p = e.getPlayer();
        //if (item.getItemStack().getType().equals(Material.getMaterial(GOAL.toUpperCase()))) {
        checkForWin(e.getItem().getItemStack(), p);
    }

    @EventHandler
    public void onCraftItemEvent(CraftItemEvent e) {
        if (hasWinner) {
            return;
        }

        Player p = (Player) e.getWhoClicked();
        checkForWin(e.getRecipe().getResult(), p);
    }

    @EventHandler
    public void onPlayerDeathEvent(PlayerDeathEvent e) {
        Player p = e.getEntity();
        resetPlayer(p);
        Title titleDead = new Title("You Died!", ":(", 0, 10, 5);
        titleDead.setTitleColor(ChatColor.RED);
        titleDead.setSubtitleColor(ChatColor.YELLOW);
        titleDead.send(p);

        p.playSound(p.getLocation(), Sound.AMBIENCE_THUNDER, 1, 1);
        for (Player participant : participatingPlayers) {
            participant.hidePlayer(p);
            participant.playSound(participant.getLocation(), Sound.AMBIENCE_THUNDER, 1, 1);
        }

        participatingPlayers.remove(p);

        if (participatingPlayers.size() == 1) {
            declareWinner(participatingPlayers.get(0));
        }

        if (Bukkit.getServer().getOnlinePlayers().size() == 1 && isGameStarted) {
            resetGame();
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player) {
            Player damager = (Player) e.getDamager();
            if (!isGameStarted) {
                e.setCancelled(true);
            }
            // this is supposed to cancel for dead players after game starts, but it doesn't work
            if (!participatingPlayers.contains(damager) && isGameStarted) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        resetPlayer(p);
        if (winnerTitle != null) {
            winnerTitle.clearTitle(p);
        }
        if (loserTitle != null) {
            loserTitle.clearTitle(p);
        }

        if (Bukkit.getServer().getOnlinePlayers().size() == 1 && isGameStarted) {
            resetGame();
        }

        participatingPlayers.remove(p);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();

        // Alive players send chat normally
        if (participatingPlayers.contains(p)) {
            return;
        }

        // If player is dead or a spectator, only send chat to other spectators
        for (Player r : e.getRecipients()) {
            if (!Ghosts.hasPlayer(r)) {
                e.getRecipients().remove(r);
            }
        }
        e.setMessage(ChatColor.DARK_GRAY + "[Spectator] " + ChatColor.GRAY + e.getMessage());
    }

    private boolean checkForWin(ItemStack item, Player p) {
        boolean hasAll = true;

        for (ItemStack goal : GOALS) {
            Bukkit.getLogger().info("checking goal: " + goal.getAmount() + " " + goal.getType());
            Bukkit.getLogger().info("picked up: " + item.getAmount() + " " + item.getType());
            Bukkit.getLogger().info("in inventory: " + p.getInventory().containsAtLeast(goal, goal.getAmount()));
            if (item.getType().equals(goal.getType())) {
                if (goal.getAmount() - item.getAmount() > 0 && !p.getInventory().containsAtLeast(goal, goal.getAmount() - item.getAmount())) {
                    hasAll = false;
                }
            } else {
                if (!p.getInventory().containsAtLeast(goal, goal.getAmount())) {
                    hasAll = false;
                }
            }
        }
        if (hasAll) {
            declareWinner(p);
            return true;
        }

        return false;
    }

    private void resetPlayer(Player p) {
        p.setHealth(20);
        p.setFoodLevel(20);
        p.setFireTicks(0);
        p.getInventory().clear();
        p.getInventory().setArmorContents(new ItemStack[p.getInventory().getArmorContents().length]);
        p.setExp(0);
        for (PotionEffect effect : p.getActivePotionEffects()) {
            p.removePotionEffect(effect.getType());
        }

        if (winnerTitle != null) {
            winnerTitle.clearTitle(p);
        }
        if (loserTitle != null) {
            loserTitle.clearTitle(p);
        }
        setSpectator(p);
    }

    private void resetGame() {
        if (isGameStarted) {
            gameTimer.end();
            startPreGameTimer();
        }
        Bukkit.broadcastMessage(ChatColor.GRAY + "No more players.  Restarting game.");
    }

    private void declareWinner(final Player winner) {
        hasWinner = true;

        Title titleWinner = new Title("You Won!", ":D", 0, 60, 5);
        titleWinner.setTitleColor(ChatColor.GREEN);
        titleWinner.setSubtitleColor(ChatColor.YELLOW);
        titleWinner.send(winner);

        Title titleLosers = new Title("You Lost!", ":'(", 0, 60, 5);
        titleLosers.setTitleColor(ChatColor.RED);
        titleLosers.setSubtitleColor(ChatColor.YELLOW);

        winner.playSound(winner.getLocation(), Sound.LEVEL_UP, 1, 1);
        for (Player loser : Bukkit.getServer().getOnlinePlayers()) {
            if (loser.equals(winner)) {
                continue;
            }
            titleLosers.send(loser);
            loser.playSound(loser.getLocation(), Sound.VILLAGER_NO, 1, 1);
        }

        Timer bragging = new Timer(this, true, 0, 5, SECONDS_AFTER_GAME_ENDS * 20, new Runnable() {
            @Override
            public void run() {
                ItemStack braggingRights = GOALS.get(0); // TODO: Brag with all items
                Location loc = new Location(winner.getWorld(), winner.getLocation().getBlockX(), winner.getLocation().getBlockY() + 2, winner.getLocation().getBlockZ());
                final Item winningItem = winner.getWorld().dropItemNaturally(loc, braggingRights);
                winningItem.setPickupDelay(Integer.MAX_VALUE);
            }
        },
                null, null);


        maxGameTimeTimer.end();
        startRestartTimer(winner);
    }

    private void startPreGameTimer() {
        pregameTimer = new Timer(this, true, 0, 20, SECONDS_BEFORE_GAME_STARTS, new Runnable() {
            @Override
            public void run() {
                int timer = pregameTimer.getTime();

                pregameObjective.getScore(pregameTimerDisplay).setScore(timer);
                pregameObjective.getScore(numPlayersScoreDisplay).setScore(Bukkit.getServer().getOnlinePlayers().size());

                if (timer <= 3) {
                    Bukkit.broadcastMessage("Game is starting in " + timer + "!");
                    Title title = new Title(new Integer(timer).toString(), "", 0, 10, 5);

                    switch (timer) {
                        case 3:
                            title = new Title("Ready", "", 0, 15, 5);
                            title.setTitleColor(ChatColor.RED);
                            break;
                        case 2:
                            title = new Title("Set", "", 0, 15, 5);
                            title.setTitleColor(ChatColor.YELLOW);
                            break;
                        case 1:
                            title = new Title("Go!", "", 0, 15, 5);
                            title.setTitleColor(ChatColor.GREEN);
                            break;
                        default:
                            break;
                    }

                    title.setSubtitleColor(ChatColor.GREEN);
                    title.setTimingsToTicks();

                    for (Player p : Bukkit.getServer().getOnlinePlayers()) {
                        title.send(p);
                    }
                }
            }
        },
                null, new Runnable() {
            @Override
            public void run() {
                Bukkit.broadcastMessage("Players online: " + Bukkit.getServer().getOnlinePlayers().size() + " - Min Players: " + MIN_PLAYERS);
                if (Bukkit.getServer().getOnlinePlayers().size() < MIN_PLAYERS) {
                    Bukkit.broadcastMessage("Not enough players!");
                    pregameTimer.end();
                    startPreGameTimer();
                } else {
                    pregameTimer.end();
                    pregameObjective.unregister();
                    startGame();
                }
            }
        });
    }

    private void startGameTimers() {

        gameTimer = new Timer(this, false, 0, 20, 0, new Runnable() {
            @Override
            public void run() {
                int timer = gameTimer.getTime();

                gameObjective.getScore(gameTimerDisplay).setScore(timer);
                gameObjective.getScore(numPlayersScoreDisplay).setScore(participatingPlayers.size());
            }
        },
                null, new Runnable() {
            @Override
            public void run() {
            }
        });

        maxGameTimeTimer = new Timer(this, true, 0, 20, MAX_SECONDS_PER_GAME, new Runnable() {
            @Override
            public void run() {

            }
        },
                null, new Runnable() {
            @Override
            public void run() {
            }
        });
    }

    private void startRestartTimer(final Player winner) {
        gameTimer.end();

        restartTimer = new Timer(this, true, 0, 20, SECONDS_AFTER_GAME_ENDS, new Runnable() {
            @Override
            public void run() {
                int timer = restartTimer.getTime();
                if (timer % 8 == 0 && timer != SECONDS_AFTER_GAME_ENDS) {
                    //Bukkit.getServer().broadcastMessage(ChatColor.GRAY + "The game has ended."); // TODO: State winner and finish time
                    Bukkit.getServer().broadcastMessage(ChatColor.GREEN + winner.getDisplayName() + " won the game!");
                }
            }
        },
                null, new Runnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getServer().getOnlinePlayers()) {
                    if (winnerTitle != null) {
                        winnerTitle.clearTitle(p);
                    }
                    if (loserTitle != null) {
                        loserTitle.clearTitle(p);
                    }
                    p.kickPlayer("");
                }
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "restart");
            }
        });
    }

    private void startGame() {
        gameObjective = board.registerNewObjective("CakeTime", "dummy");
        gameObjective.setDisplayName(GAME_DESCRIPTION);
        gameObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
        Random random = new Random();

        for (Player p : Bukkit.getServer().getOnlinePlayers()) {
            participatingPlayers.add(p);
            Ghosts.removePlayer(p);
            p.removePotionEffect(PotionEffectType.INVISIBILITY);

            World world = Bukkit.getServer().getWorld(WORLD_NAME);
            int x = random.nextInt(settings.getConfig().getInt("general.maxX") - settings.getConfig().getInt("general.minX")) + settings.getConfig().getInt("general.minX");
            int z = random.nextInt(settings.getConfig().getInt("general.maxZ") - settings.getConfig().getInt("general.minZ")) + settings.getConfig().getInt("general.minZ");
            //int x = settings.getConfig().getInt("general.minX");
            //int z = settings.getConfig().getInt("general.minZ");
            int y = world.getHighestBlockYAt(x, z);
            Location randomLocation = new Location(world, x + 0.5D, y, z + 0.5D);
            world.getChunkAt(randomLocation).load();
            p.teleport(randomLocation);

            p.setAllowFlight(false);
            p.setGameMode(GameMode.SURVIVAL);
            p.setNoDamageTicks(0);
            p.removePotionEffect(PotionEffectType.INVISIBILITY);
        }
        startGameTimers();
        isGameStarted = true;
    }

    private void setSpectator(Player p) {
        p.setGameMode(GameMode.ADVENTURE);
        p.setNoDamageTicks(Integer.MAX_VALUE);
        p.setAllowFlight(true);
        p.setFlying(true);
        Ghosts.addPlayer(p);
        p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 15));
        participatingPlayers.remove(p);
    }

    public static boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        return (path.delete());
    }

	/*@EventHandler
    public void teleport(PlayerInteractEvent e){
        if(e.getPlayer().getGameMode() == GameMode.ADVENTURE){
        	int rand = new Random().nextInt(participatingPlayers.size()); //get a random player
        	Player p = participatingPlayers.get(rand); //choose a player based on our random player
            if(p  == e.getPlayer()){
                e.setCancelled(true);
            } else {
                if(p.getGameMode() == GameMode.ADVENTURE){
                    e.setCancelled(true);
                } else {
                	p.setPassenger(e.getPlayer()); //sets the player in ADVENTURE mode to ride the living player
                }
            }
        }
    }*/
}
