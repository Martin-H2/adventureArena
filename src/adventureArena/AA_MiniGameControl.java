package adventureArena;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class AA_MiniGameControl {


	private static final String MINIGAME_CONFIGNAME = "miniGames.yml";
	private static File configFile = null;
	private static YamlConfiguration config = null;

	public static final GameMode MINIGAME_HUB_GAMEMODE = GameMode.ADVENTURE;
	private static final PotionEffect PERMANENT_NIGHTVISION = new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, true, false);
	private static List<AA_MiniGame> miniGames = new ArrayList<AA_MiniGame>();

	public static List<AA_MiniGame> getMiniGames() {
		return miniGames;
	}




	// ################ MINIGAME HUB ##################

	public static void joinMiniGameHub(final Player player, Location target) {
		FileConfiguration config = getPluginConfig();
		if (target == null) {
			target = getMiniGameHubSpawn(player.getWorld());
		}
		if (target != null && AA_InventorySaver.saveInventoryAndPlayerMeta(player, AA_ConfigPaths.savedPlayerData)) {
			player.setLevel(0);
			player.setExp(0);
			config.set(AA_ConfigPaths.isInMiniGameHub + "." + player.getName(), true);
			savePluginConfig();
			setMiniGameSpectator(player, false);
		}
	}
	public static void setMiniGameSpectator(final Player player, final boolean onDeath) {
		setPlayerState(player, PlayerState.IS_WATCHING, null);
		player.setGameMode(MINIGAME_HUB_GAMEMODE);
		Location target = getMiniGameHubSpawn(player.getWorld());
		player.setBedSpawnLocation(target, true);
		if (!onDeath) {
			setNeutralPlayerState(player);
			teleportSafe(player, target);
		}
	}
	public static void leaveMiniGameHub(final Player player, Location target) {
		FileConfiguration config = getPluginConfig();
		if (target == null) {
			target = player.getWorld().getSpawnLocation();
		}
		config.set(AA_ConfigPaths.isInMiniGameHub + "." + player.getName(), false);
		config.set(AA_ConfigPaths.playerStates + "." + player.getName(), null);
		savePluginConfig();
		player.setGameMode(Bukkit.getDefaultGameMode());
		player.setBedSpawnLocation(target, true);
		teleportSafe(player, target);
		AA_InventorySaver.restoreInventoryAndPlayerMeta(player, AA_ConfigPaths.savedPlayerData);
	}
	public static boolean isInMiniGameHub(final Player player) {
		FileConfiguration config = getPluginConfig();
		return config.getBoolean( AA_ConfigPaths.isInMiniGameHub + "." + player.getName(), false);
	}
	public static Location getMiniGameHubSpawn(final World world) {
		FileConfiguration config = getPluginConfig();
		if(!config.contains(AA_ConfigPaths.miniGameHubSpawnX)) {
			AA_MessageSystem.consoleError("miniGameHubSpawn not found in config");
			return null;
		}
		double x = config.getDouble(AA_ConfigPaths.miniGameHubSpawnX);
		double y = config.getDouble(AA_ConfigPaths.miniGameHubSpawnY);
		double z = config.getDouble(AA_ConfigPaths.miniGameHubSpawnZ);
		return new Location(world, x, y, z);
	}



	// ################ MINIGAME MANAGEMENT ##################

	public static void loadMiniGamesFromConfig() {
		for (String path: getMiniGameConfig().getRoot().getKeys(false)) {
			AA_MiniGame mg = AA_MiniGame.loadFromConfig(Integer.parseInt(path));
			if (mg!=null) {
				miniGames.add(mg);
				if (mg.isInProgress()) {
					AA_MessageSystem.consoleWarn("miniGame was still in progress: '" + mg.getName() + "', cleaning up...");
					mg.wipePlaySession();
				}
			}
		}
		for (Player p: Bukkit.getOnlinePlayers()) {
			kickIfPlayingMiniGame(p);
		}
	}
	public static void kickIfPlayingMiniGame(final Player p) {
		if (isPlayingMiniGame(p)) {
			AA_MiniGame mg = getMiniGameForPlayer(p);
			AA_MessageSystem.consoleWarn("player "+ p.getName() + " still in miniGame '" + mg.getName() + "', kicking...");
			leaveCurrentMiniGame(p, false);
		}
	}
	public static void addMiniGame(final AA_MiniGame miniGame) {
		miniGames.add(miniGame);
	}
	public static AA_MiniGame getMiniGame(final int id) {
		for (AA_MiniGame mg: miniGames) {
			if (mg.getID()==id) return mg;
		}
		return null;
	}
	public static AA_MiniGame getMiniGameContainingLocation(final Location location) {
		for (AA_MiniGame miniGame: miniGames) {
			if (miniGame.isInsideBounds(location))
				return miniGame;
		}
		return null;
	}
	public static int getNumberOfMiniGames() {
		return miniGames!=null ? miniGames.size() : 0;
	}
	public static void startEditing(final AA_MiniGame miniGame, final Player player) {
		if (!isWatchingMiniGames(player)) {
			AA_MessageSystem.error("You are not flagged as spectator. Please inform admin !", player);
			return;
		}

		if (miniGame.isInProgress()) {
			AA_MessageSystem.error("This minigame is in progress and can't be edited. (Your changes would be nullified by the area-rollback)", player);
			return;
		}

		if (isPlayerInsideHisEditableArea(player)) {
			Block target = AA_TerrainHelper.getAirBlockAboveGround(player.getLocation().getBlock().getRelative(BlockFace.DOWN, 3), false);
			teleportSafe(player, target);
			player.setGameMode(GameMode.CREATIVE);
			AA_InventorySaver.restoreInventoryAndPlayerMeta(player, AA_ConfigPaths.savedCreativeData);
			player.setLevel(0);
			player.setExp(0);
			player.addPotionEffect(PERMANENT_NIGHTVISION, true);
			player.setFlying(true);
			player.setVelocity(new Vector(0, 3, 0));
			PlayerInventory inv = player.getInventory();
			if(!inv.contains(Material.MILK_BUCKET)) {
				inv.addItem(new ItemStack(Material.MILK_BUCKET,1));
			}
			setPlayerState(player, PlayerState.IS_EDITING, miniGame);
			miniGame.addPlayer(AA_TeamManager.EDITORS_TEAM, player);
			miniGame.wipeEntities();
		} else {
			AA_MessageSystem.error("You are not allowed to edit this miniGame", player);
		}
	}
	public static void addAllowedEditor(final int id, final Player player, final CommandSender sender) {
		AA_MiniGame mg = getMiniGame(id);
		if (mg==null) {
			AA_MessageSystem.error("miniGameId not found: " + id, sender);
		} else {
			mg.addAllowedEditor(player.getName());
			mg.persist();
			AA_MessageSystem.success("gave " + player.getName() + " editing power for miniGame #" + id, sender);
		}
	}
	public static void removeAllowedEditor(final int id, final Player player, final CommandSender sender) {
		AA_MiniGame mg = getMiniGame(id);
		if (mg==null) {
			AA_MessageSystem.error("miniGameId not found: " + id, sender);
		} else {
			mg.removeAllowedEditor(player.getName());
			mg.persist();
			AA_MessageSystem.success("took " + player.getName() + "'s editing power for miniGame #" + id, sender);
		}
	}
	public static boolean isPlayerInsideHisEditableArea(final Player player) {
		AA_MiniGame mg = getMiniGameContainingLocation(player.getLocation());
		return mg==null ? false : mg.isEditableByPlayer(player);
	}
	public static void surroundingMiniGameInfo(final Player player) {
		AA_MiniGame mg = getMiniGameContainingLocation(player.getLocation());
		if (mg!=null) {
			AA_MessageSystem.sideNote("Inside minigame: " + mg.toString(), player);
		} else {
			AA_MessageSystem.error("You are not inside a miniGame area", player);
		}
	}






	// ################ MINIGAME PROGRESS ##################

	static boolean canJoinMiniGame(final AA_MiniGame miniGame, final ArrayList<Player> players) {
		// EDIT SESSION CHECK
		if (miniGame.isLockedByEditSession()) {
			AA_MessageSystem.errorForGroup("This minigame is currently locked by an edit-session.", players);
			AA_MessageSystem.errorForGroup("Sombody wanted to play your " + miniGame.getName() + ", but it's locked by an edit-session.", miniGame.getAllowedEditors());
			return false;
		}
		// IN PROGRESS CHECK
		if (miniGame.isInProgress()) {
			AA_MessageSystem.errorForGroup("This minigame is already in progress.", players);
			return false;
		}
		// ENVIRONMENT BACKUP CHECK
		if (miniGame.needsEnvironmentBackup()) {
			AA_MessageSystem.sideNoteForGroup("Saving environment backup of " + miniGame.getName(), players);
			if(!miniGame.doEnvironmentBackup()) {
				AA_MessageSystem.errorAll("Exception while performing area backup of " + miniGame.getName() + ", please inform admin.");
				return false;
			}
		}
		return true;
	}


	static void joinMiniGame(final AA_MiniGame miniGame, final String teamName, final Player p, final Vector vector) {
		if (!isWatchingMiniGames(p)) return;
		AA_MessageSystem.success("Starting " + miniGame.getName() + " for you...", p);
		setNeutralPlayerState(p);
		teleportSafe(p, AA_TerrainHelper.getAirBlockAboveGround(vector.toLocation(p.getWorld()), true));
		setPlayerState(p, PlayerState.IS_PLAYING,miniGame);
		miniGame.addPlayer(teamName, p);
		p.getInventory().addItem(miniGame.getSpawnEquip());
	}

	static void setPlayerState(final Player p, final PlayerState playerState, final AA_MiniGame optionalMiniGame) {
		getPluginConfig().set(AA_ConfigPaths.playerStates + "." + p.getName(), playerState.toString());
		if (optionalMiniGame!=null) {
			if (playerState==PlayerState.IS_EDITING) {
				optionalMiniGame.setLockedByEditSession(true);
				getPluginConfig().set(AA_ConfigPaths.activeMinigameId + "." + p.getName(), optionalMiniGame.getID());
			}
			else if (playerState==PlayerState.IS_PLAYING) {
				optionalMiniGame.setInProgress(true);
				getPluginConfig().set(AA_ConfigPaths.activeMinigameId + "." + p.getName(), optionalMiniGame.getID());
			}
		}
		if (playerState==PlayerState.IS_WATCHING) {
			getPluginConfig().set(AA_ConfigPaths.activeMinigameId + "." + p.getName(), null);
		}
		savePluginConfig();
	}

	public static void leaveCurrentMiniGame(final Player player, final boolean onDeath) {
		AA_MiniGame mg = getMiniGameForPlayer(player);
		//Team team = AA_TeamManager.getTeam(player);
		mg.removePlayer(player);
		AA_ScoreManager.onPlayerLeft(mg, player);
		if (isEditingMiniGame(player)) {
			if (mg.getNumberOfPlayersRemaining()==0) {
				mg.setLockedByEditSession(false);
				mg.wipeEntities();
			}
			AA_InventorySaver.saveInventoryAndPlayerMeta(player, AA_ConfigPaths.savedCreativeData);
		} else if (isPlayingMiniGame(player)) {
			if (mg.isVictory()) {
				win(mg);
				mg.setInProgress(false);
				mg.wipeEntities();
				mg.wipePlaySession();
				AA_MessageSystem.sideNoteForGroup("All players left your " + mg.getName() + ". Rolling back environment...", mg.getAllowedEditors());
				mg.restoreEnvironmentBackup();
			}
		}
		setMiniGameSpectator(player, onDeath);
		AA_ScoreManager.updateHighScoreList(mg);
	}

	private static void win(final AA_MiniGame mg) {
		for (Player p: Bukkit.getOnlinePlayers()) {
			AA_MiniGame playersMG = getMiniGameForPlayer(p);
			if (mg.equals(playersMG)) {
				AA_MessageSystem.success("You won " + mg.getName(), p);
				AA_ScoreManager.onPlayerWin(mg, p);
				mg.removePlayer(p);
				setMiniGameSpectator(p, false);
			}
		}

	}




	public static AA_MiniGame getMiniGameForPlayer(final Player player) {
		FileConfiguration config = getPluginConfig();
		AA_MiniGame mg = null;
		if (config.contains(AA_ConfigPaths.activeMinigameId + "." + player.getName())) {
			int id = config.getInt(AA_ConfigPaths.activeMinigameId + "." + player.getName());
			mg = getMiniGame(id);
		}
		if (mg==null){
			mg = getMiniGameContainingLocation(player.getLocation());
		}
		return mg;
	}

	public static boolean isPlayingMiniGame(final Player player) {
		return PlayerState.IS_PLAYING.toString().equals(getPluginConfig().getString(AA_ConfigPaths.playerStates + "." + player.getName()));
	}
	public static boolean isEditingMiniGame(final Player player) {
		return PlayerState.IS_EDITING.toString().equals(getPluginConfig().getString(AA_ConfigPaths.playerStates + "." + player.getName()));
	}
	public static boolean isWatchingMiniGames(final Player player) {
		return PlayerState.IS_WATCHING.toString().equals(getPluginConfig().getString(AA_ConfigPaths.playerStates + "." + player.getName()));
	}

	static ArrayList<Player> getAllGameHubSpectators() {
		ArrayList<Player> gameHubSpectators = new ArrayList<Player>();
		for (Player p: Bukkit.getOnlinePlayers()) {
			if (isWatchingMiniGames(p)) {
				gameHubSpectators.add(p);
			}
		}
		return gameHubSpectators;
	}







	// ################ PLAYER UTIL ##################

	@SuppressWarnings("deprecation")
	public static void setNeutralPlayerState(final Player player) {
		PlayerInventory inventory = player.getInventory();
		player.setHealth(player.getMaxHealth());
		player.setFoodLevel(20);
		player.setSaturation(20);
		inventory.clear();
		inventory.setArmorContents(new ItemStack[inventory.getArmorContents().length]);
		for (PotionEffect pe: player.getActivePotionEffects()) {
			player.removePotionEffect(pe.getType());
		}
		player.updateInventory();
	}

	public static void teleportSafe(final Player player, final Location target) {
		target.add(0.5, 0.0, 0.5);
		player.teleport(target);
	}
	public static void teleportSafe(final Player player, final Block target) {
		player.teleport(target.getLocation().add(0.5, 0.0, 0.5));
	}









	// ################ CONFIG UTIL ##################

	public static FileConfiguration getPluginConfig() {
		return AdventureArena.getInstance().getConfig();
	}
	public static void savePluginConfig() {
		AdventureArena.getInstance().saveConfig();
	}
	public static FileConfiguration getMiniGameConfig() {
		if (config==null) {
			File dataFolder = AdventureArena.getInstance().getDataFolder();
			if (!dataFolder.exists()) {
				dataFolder.mkdirs();
			}
			configFile = new File(dataFolder, MINIGAME_CONFIGNAME);
			config = YamlConfiguration.loadConfiguration(configFile);
		}
		return config;
	}
	public static void saveMiniGameConfig() {
		if (config!=null) {
			try {
				config.save(configFile);
			} catch (IOException e) {
				AA_MessageSystem.consoleError(MINIGAME_CONFIGNAME + "cannot be overwritten or created");
				e.printStackTrace();
			}
		}
	}





}
