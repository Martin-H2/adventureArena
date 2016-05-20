package adventureArena.control;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import adventureArena.*;
import adventureArena.enums.ConfigPaths;
import adventureArena.enums.PlayerState;


public class HubControl {


	static final Vector				MINIGAME_HUB_MIN		= new Vector(97, 9, -253);
	static final Vector				MINIGAME_HUB_MAX		= new Vector(303, 47, -145);

	public static final GameMode	MINIGAME_HUB_GAMEMODE	= GameMode.ADVENTURE;
	static final PotionEffect		PERMANENT_NIGHTVISION	= new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, true, false);
	static final PotionEffect		PERMANENT_SPEED			= new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, true, false);
	static final PotionEffect		PERMANENT_SATURATION	= new PotionEffect(PotionEffectType.SATURATION, Integer.MAX_VALUE, 0, true, false);



	public static void joinMiniGameHub(final Player player, Location target) {
		if (!HubControl.isInMiniGameHub(player)) {
			FileConfiguration config = ConfigAccess.getPluginConfig();
			if (target == null) {
				target = HubControl.getMiniGameHubSpawn(player.getWorld());
			}
			if (target != null && InventorySaver.saveInventoryAndPlayerMeta(player, ConfigPaths.savedPlayerData)) {
				player.setLevel(0);
				player.setExp(0);
				config.set(ConfigPaths.isInMiniGameHub + "." + player.getName(), true);
				ConfigAccess.savePluginConfig();
				HubControl.becomeSpectator(player, false, target, true);
				player.setBedSpawnLocation(target, true);
				MessageSystem.sideNote("MiniGames documentation @forum: " + AA_SignCommand.WIKI_HELP, player);
			}
		}
	}

	public static void becomeSpectator(final Player player, final boolean onDeath, Location optionalBackportLocation, boolean isHubJoin) {
		PlayerControl.setPlayerState(player, PlayerState.IS_WATCHING, null);
		player.setGameMode(HubControl.MINIGAME_HUB_GAMEMODE);
		if (optionalBackportLocation == null) {
			optionalBackportLocation = HubControl.getMiniGameHubSpawn(player.getWorld());
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
			target = player.getWorld().getSpawnLocation();
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



	public static void rebuildMiniGameCfgFromSigns(final String worldName) { //TODO cache allowed Editors
		final World world = Bukkit.getWorld(worldName);
		if (world == null) {
			MessageSystem.consoleError("world not found: " + worldName);
			return;
		}
		Block block;
		int numBlocks = 0;
		final List<AA_SignCommand> foundSignCommands = new ArrayList<AA_SignCommand>();
		MessageSystem.consoleDebug("searching SignCommands...");
		for (int x = MINIGAME_HUB_MIN.getBlockX(); x <= MINIGAME_HUB_MAX.getBlockX(); x++) {
			for (int y = MINIGAME_HUB_MIN.getBlockY(); y <= MINIGAME_HUB_MAX.getBlockY(); y++) {
				for (int z = MINIGAME_HUB_MIN.getBlockZ(); z <= MINIGAME_HUB_MAX.getBlockZ(); z++) {
					numBlocks++;
					block = world.getBlockAt(x, y, z);
					AA_SignCommand signCommand = AA_SignCommand.createFrom(block);
					if (signCommand != null) {
						foundSignCommands.add(signCommand);
					}
				}
			}
		}
		MessageSystem.consoleDebug("done. found " + foundSignCommands.size() + " cmds in " + numBlocks + " blocks.");
		AdventureArena.executeDelayed(5, new Runnable() {

			@Override
			public void run() {
				MessageSystem.consoleDebug("loading borders...");
				for (AA_SignCommand cmd: foundSignCommands) {
					if (cmd.getCommand().equals("border")) {
						cmd.executeOnCreation(null, world);
					}
				}
				MessageSystem.consoleDebug("done...");
				AdventureArena.executeDelayed(5, new Runnable() {

					@Override
					public void run() {
						MessageSystem.consoleDebug("loading other commands...");
						for (AA_SignCommand cmd: foundSignCommands) {
							if (!cmd.getCommand().equals("border")) {
								cmd.executeOnCreation(null, world);
							}
						}
						MessageSystem.consoleDebug("done...");
					}
				});
			}
		});
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

	public static Location getMiniGameHubSpawn(final World world) {
		FileConfiguration config = ConfigAccess.getPluginConfig();
		if (!config.contains(ConfigPaths.miniGameHubSpawnX)) {
			MessageSystem.consoleError("miniGameHubSpawn not found in config");
			return null;
		}
		double x = config.getDouble(ConfigPaths.miniGameHubSpawnX);
		double y = config.getDouble(ConfigPaths.miniGameHubSpawnY);
		double z = config.getDouble(ConfigPaths.miniGameHubSpawnZ);
		return new Location(world, x, y, z);
	}

}
