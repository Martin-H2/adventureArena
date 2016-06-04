package adventureArena.control;

import java.util.ArrayList;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;
import adventureArena.InventorySaver;
import adventureArena.ItemHelper;
import adventureArena.PluginManagement;
import adventureArena.TerrainHelper;
import adventureArena.enums.ConfigPaths;
import adventureArena.enums.PlayerState;
import adventureArena.messages.MessageSystem;
import adventureArena.messages.OnScreenMessages;
import adventureArena.miniGameComponents.MiniGame;
import adventureArena.score.HighScoreManager;
import adventureArena.score.ScoreManager;

public class MiniGameSessions {



	public static void prepareSession(MiniGame miniGame) {
		miniGame.wipeEntities();
		miniGame.loadEnvironmentBackup();
	}

	static void joinPlaySession(final MiniGame miniGame, final String teamName, final Player p, final Vector spawnPoint) {
		if (!p.isOnline() || !PlayerControl.isWatchingMiniGames(p)) return;
		MessageSystem.sideNote("Starting " + miniGame.getName() + " for you...", p);
		PlayerControl.clearInventoryAndBuffs(p);
		p.setGameMode(HubControl.MINIGAME_HUB_GAMEMODE);
		p.setBedSpawnLocation(miniGame.getSpectatorRespawnPoint(), true);
		PlayerControl.teleportSafe(p, TerrainHelper.getAirBlockAboveGround(spawnPoint.toLocation(miniGame.getWorld()), true, miniGame), miniGame.getPlayableAreaMidpoint());
		PlayerControl.setPlayerState(p, PlayerState.IS_PLAYING, miniGame);
		miniGame.addPlayer(teamName, p);
		for (ItemStack item: miniGame.getSpawnEquip()) {
			ItemHelper.addItemSmart(p, item);
		}
		p.updateInventory();
	}

	public static void joinEditSession(final MiniGame miniGame, final Player player) {
		if (!PlayerControl.isWatchingMiniGames(player)) {
			MessageSystem.error("You are not flagged as spectator. Please inform admin !", player);
			return;
		}

		if (miniGame.isPlaySessionActive()) {
			MessageSystem.error("This minigame is in progress and can't be edited. (Your changes would be nullified by the area-rollback)", player);
			return;
		}

		if (PlayerControl.isPlayerInsideHisEditableArea(player)) {
			// START EDITING NOW
			if (!miniGame.isEditSessionActive()) {
				// 1st editor comes in
				prepareSession(miniGame);
			}
			Block target = TerrainHelper.getAirBlockAboveGround(player.getLocation().getBlock().getRelative(BlockFace.DOWN, 3), false, miniGame);
			player.setBedSpawnLocation(miniGame.getSpectatorRespawnPoint(), true);
			PlayerControl.teleportSafe(player, target, miniGame.getPlayableAreaMidpoint());
			player.setGameMode(GameMode.CREATIVE);
			InventorySaver.restoreInventoryAndPlayerMeta(player, ConfigPaths.savedCreativeData);
			player.setLevel(0);
			player.setExp(0);
			player.addPotionEffect(HubControl.PERMANENT_NIGHTVISION, true);
			player.setFlying(true);
			player.setVelocity(new Vector(0, 1, 0));
			PlayerInventory inv = player.getInventory();
			if (!inv.contains(Material.MILK_BUCKET)) {
				inv.addItem(new ItemStack(Material.MILK_BUCKET, 1));
			}
			PlayerControl.setPlayerState(player, PlayerState.IS_EDITING, miniGame);
			miniGame.addPlayer(TeamManager.EDITORS_TEAM, player);
		}
		else {
			MessageSystem.error("You are not allowed to edit this miniGame", player);
		}
	}

	public static void leaveCurrentSession(final Player player, final boolean onDeath) {
		final MiniGame mg = MiniGameLoading.getMiniGameForPlayer(player);
		mg.removePlayer(player);
		if (PlayerControl.isEditingMiniGame(player)) {
			InventorySaver.saveInventoryAndPlayerMeta(player, ConfigPaths.savedCreativeData);
			if (mg.getNumberOfPlayersRemaining() == 0) {
				mg.saveEnvironmentBackup();
				mg.wipeSessionVariables();
			}
		}
		else if (PlayerControl.isPlayingMiniGame(player)) {
			ScoreManager.onPlayerLeft(mg, player);
			if (mg.isVictory()) {
				mg.setOver();
				double sessionEndDelay = mg.getNumberOfPlayersRemaining() >= 1 ? 5.0 : 3.0;
				PluginManagement.executeDelayed(sessionEndDelay, new Runnable() {

					@Override
					public void run() {
						endPlaySession(mg);
					}
				});
			}
			HighScoreManager.updateHighScoreList(mg);
		}
		HubControl.becomeSpectator(player, onDeath, mg.getSpectatorRespawnPoint(), false);
	}

	private static void endPlaySession(final MiniGame miniGame) {
		for (Player p: miniGame.getPlayersInArea()) {
			if (!p.isDead()) {
				if (PlayerControl.isPlayingMiniGame(p)) {
					//AA_MessageSystem.success("You won " + mg.getName(), p);
					OnScreenMessages.sendGameWonMessage(miniGame, p);
					ScoreManager.onPlayerWin(miniGame, p);
					miniGame.removePlayer(p);
					HubControl.becomeSpectator(p, false, miniGame.getSpectatorRespawnPoint(), false);
				}
			}
			else {
				p.spigot().respawn(); // will get teleported etc after respawn
			}
		}
		PluginManagement.executeDelayed(0.2, new Runnable() {

			@Override
			public void run() {
				HighScoreManager.updateHighScoreList(miniGame);
			}
		});
		miniGame.wipeEntities();
		miniGame.wipeSessionVariables();
		miniGame.loadEnvironmentBackup(); // not really needed, but looks better
	}


	public static void kickIfInsideMiniGame(final Player p) {
		if (PlayerControl.isPlayingMiniGame(p) || PlayerControl.isEditingMiniGame(p)) {
			MiniGame mg = MiniGameLoading.getMiniGameForPlayer(p);
			MessageSystem.consoleWarn(p.getName() + " was kicked from '" + mg.getName() + "' id: " + mg.getID());
			leaveCurrentSession(p, false);
		}
	}

	public static void kickIfInsideSpecificMiniGame(final MiniGame mg, final Player p) {
		if (mg.equals(MiniGameLoading.getMiniGameForPlayer(p))) {
			kickIfInsideMiniGame(p);
		}
	}

	static boolean canJoinMiniGame(final MiniGame miniGame, final ArrayList<Player> players) {
		// EDIT SESSION CHECK
		if (miniGame.isEditSessionActive()) {
			MessageSystem.errorToGroup("This minigame is currently locked by an edit-session.", players);
			MessageSystem.errorToGroup("Sombody wanted to play your " + miniGame.getName() + ", but it's locked by an edit-session.", miniGame.getOnlineAllowedEditors());
			return false;
		}
		// IN PROGRESS CHECK
		if (miniGame.isPlaySessionActive()) {
			if (miniGame.isOver()) {
				MessageSystem.warningToGroup("This minigame will end soon. Try again in 4s", players);
			}
			else {
				MessageSystem.errorToGroup("This minigame is already in progress.", players);
			}
			return false;
		}
		return true;
	}



}
