package adventureArena.control;

import java.util.ArrayList;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import adventureArena.AA_SignCommand;
import adventureArena.ConfigAccess;
import adventureArena.InventorySaver;
import adventureArena.enums.ConfigPaths;
import adventureArena.enums.PlayerState;
import adventureArena.messages.MessageSystem;


public class HubControl {

	public final static String		DISABLE_CLIENT_RADAR_MAGIC_SUFFIX	= " §3 §6 §3 §6 §3 §6 §e";
	public final static String		DISABLE_CLIENT_CAVEMAP_MAGIC_SUFFIX	= " §3 §6 §3 §6 §3 §6 §d";

	static final Vector				MINIGAME_HUB_MIN					= new Vector(97, 9, -253);
	static final Vector				MINIGAME_HUB_MAX					= new Vector(303, 47, -145);

	public static final GameMode	MINIGAME_HUB_GAMEMODE				= GameMode.ADVENTURE;
	static final PotionEffect		PERMANENT_NIGHTVISION				= new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, true, false);
	static final PotionEffect		PERMANENT_SPEED						= new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, true, false);
	static final PotionEffect		PERMANENT_SATURATION				= new PotionEffect(PotionEffectType.SATURATION, Integer.MAX_VALUE, 0, true, false);



	public static void joinMiniGameHub(final Player player, Location target) {
		if (!HubControl.isInMiniGameHub(player)) {
			FileConfiguration config = ConfigAccess.getPluginConfig();
			if (target == null) {
				target = HubControl.getMiniGameHubSpawn();
			}
			if (target != null && InventorySaver.saveInventoryAndPlayerMeta(player, ConfigPaths.savedPlayerData)) {
				player.setLevel(0);
				player.setExp(0);
				config.set(ConfigPaths.isInMiniGameHub + "." + player.getName(), true);
				ConfigAccess.savePluginConfig();
				HubControl.becomeSpectator(player, false, target, true);
				player.setBedSpawnLocation(target, true);
				MessageSystem.sideNote("MiniGames documentation @forum: " + AA_SignCommand.WIKI_HELP, player);
				MessageSystem.sideNote("Please don't use entity-radars in pvp." + DISABLE_CLIENT_RADAR_MAGIC_SUFFIX + DISABLE_CLIENT_CAVEMAP_MAGIC_SUFFIX, player);
			}
		}
	}

	public static void becomeSpectator(final Player player, final boolean onDeath, Location optionalBackportLocation, boolean isHubJoin) {
		PlayerControl.setPlayerState(player, PlayerState.IS_WATCHING, null);
		player.setGameMode(HubControl.MINIGAME_HUB_GAMEMODE);
		if (optionalBackportLocation == null) {
			optionalBackportLocation = HubControl.getMiniGameHubSpawn();
		}
		if (!onDeath) {
			PlayerControl.clearInventoryAndBuffs(player);
			PlayerControl.teleportSafeAndShove(player, optionalBackportLocation, !isHubJoin);
			//spectator buffs
			player.addPotionEffect(HubControl.PERMANENT_SPEED, true);
			player.addPotionEffect(HubControl.PERMANENT_SATURATION, true);
			//player.addPotionEffect(PERMANENT_NIGHTVISION, true);
		}
	}

	public static void leaveMiniGameHub(final Player player, Location target) {
		MiniGameSessions.kickIfInsideMiniGame(player);
		FileConfiguration config = ConfigAccess.getPluginConfig();
		if (target == null) {
			target = getMiniGameHubWorld().getSpawnLocation();
		}
		config.set(ConfigPaths.isInMiniGameHub + "." + player.getName(), false);
		config.set(ConfigPaths.playerStates + "." + player.getName(), null);
		ConfigAccess.savePluginConfig();
		player.setGameMode(Bukkit.getDefaultGameMode());
		player.setBedSpawnLocation(target, true);
		PlayerControl.teleportSafeAndShove(player, target, false);
		InventorySaver.restoreInventoryAndPlayerMeta(player, ConfigPaths.savedPlayerData);
	}

	public static void kickFromMiniGameAndHub(Player p) {
		if (p.isDead()) {
			p.spigot().respawn();
		}
		MiniGameSessions.kickIfInsideMiniGame(p);
		if (isInMiniGameHub(p)) {
			leaveMiniGameHub(p, null);
		}
	}



	public static boolean isInMiniGameHub(final Player player) {
		FileConfiguration config = ConfigAccess.getPluginConfig();
		return config.getBoolean(ConfigPaths.isInMiniGameHub + "." + player.getName(), false);
	}

	public static boolean isInMgHubAABB(final Location location) {
		return location.toVector().isInAABB(HubControl.MINIGAME_HUB_MIN, HubControl.MINIGAME_HUB_MAX);
	}



	static ArrayList<Player> getAllGameHubSpectators() {
		ArrayList<Player> gameHubSpectators = new ArrayList<Player>();
		for (Player p: Bukkit.getOnlinePlayers()) {
			if (PlayerControl.isWatchingMiniGames(p)) {
				gameHubSpectators.add(p);
			}
		}
		return gameHubSpectators;
	}

	public static Location getMiniGameHubSpawn() {
		FileConfiguration config = ConfigAccess.getPluginConfig();
		if (!config.contains(ConfigPaths.miniGameHubSpawnX)) {
			MessageSystem.consoleError("miniGameHubSpawn not found in config");
			return null;
		}
		double x = config.getDouble(ConfigPaths.miniGameHubSpawnX);
		double y = config.getDouble(ConfigPaths.miniGameHubSpawnY);
		double z = config.getDouble(ConfigPaths.miniGameHubSpawnZ);
		return new Location(getMiniGameHubWorld(), x, y, z);
	}

	public static World getMiniGameHubWorld() {
		FileConfiguration config = ConfigAccess.getPluginConfig();
		return Bukkit.getWorld(config.getString(ConfigPaths.miniGameHubWorldName));
	}

}
