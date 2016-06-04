package adventureArena.control;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import adventureArena.AA_SignCommand;
import adventureArena.ConfigAccess;
import adventureArena.PluginManagement;
import adventureArena.TerrainHelper;
import adventureArena.messages.MessageSystem;
import adventureArena.miniGameComponents.MiniGame;
import adventureArena.score.HighScoreManager;


public class MiniGameManagement {

	public static void addAllowedEditor(final int id, final Player player, final CommandSender sender) {
		MiniGame mg = MiniGameLoading.getMiniGame(id);
		if (mg == null) {
			MessageSystem.error("miniGameId not found: " + id, sender);
		}
		else {
			mg.addAllowedEditor(player.getName());
			mg.persist();
			MessageSystem.success("gave " + player.getName() + " editing power for miniGame #" + id, sender);
		}
	}

	public static void removeAllowedEditor(final int id, final String playerName, final CommandSender sender) {
		MiniGame mg = MiniGameLoading.getMiniGame(id);
		if (mg == null) {
			MessageSystem.error("miniGameId not found: " + id, sender);
		}
		else {
			mg.removeAllowedEditor(playerName);
			mg.persist();
			MessageSystem.success("took " + playerName + "'s editing power for miniGame #" + id, sender);
		}
	}

	public static void surroundingMiniGameInfo(final Player commandExecutor) {
		MiniGame mg = MiniGameLoading.getMiniGameContainingLocation(commandExecutor.getLocation());
		if (mg != null) {
			MessageSystem.sideNote("Inside minigame: " + mg.toString(), commandExecutor);
		}
		else {
			MessageSystem.error("You are not inside a miniGame area", commandExecutor);
		}
	}

	public static void surroundingMiniGameSessionWipe(final Player commandExecutor) {
		final MiniGame mg = MiniGameLoading.getMiniGameContainingLocation(commandExecutor.getLocation());
		if (mg != null) {
			for (Player p: Bukkit.getOnlinePlayers()) {
				MiniGameSessions.kickIfInsideSpecificMiniGame(mg, p);
			}
			for (Player p: mg.getPlayersRemaining()) {
				MiniGameSessions.kickIfInsideMiniGame(p);
			}
			if (mg.isAnySessionActive()) {
				mg.wipeEntities();
				mg.wipeSessionVariables();
			}
			MessageSystem.success("Wiped session!", commandExecutor);
		}
	}

	public static void surroundingMiniGameAllowAllSpectators(final Player commandExecutor) {
		final MiniGame mg = MiniGameLoading.getMiniGameContainingLocation(commandExecutor.getLocation());
		if (mg != null) {
			for (Player p: mg.getSpectators()) {
				mg.addAllowedEditor(p.getName());
				MessageSystem.success("gave " + p.getName() + " editing power for " + mg.getName(), commandExecutor);
			}
			mg.persist();
		}
	}

	public static void surroundingMiniGameRoomReset(Player player) {
		MiniGame mg = MiniGameLoading.getMiniGameContainingLocation(player.getLocation());
		if (mg != null) {
			surroundingMiniGameSessionWipe(player);
			mg.resetRoom();
			MiniGameManagement.surroundingMiniGameScoreReset(player);
		}
		else {
			MessageSystem.error("no minigame found in this area", player);
		}
	}

	public static void surroundingMiniGameFixSigns(Player player) {
		MiniGame mg = MiniGameLoading.getMiniGameContainingLocation(player.getLocation());
		if (mg != null) {
			if (mg.isEditSessionActive()) {
				TerrainHelper.fixSigns(mg);
			}
			else {
				MessageSystem.error("miniGame needs to be locked by an edit session", player);
			}
		}
		else {
			MessageSystem.error("no minigame found in this area", player);
		}
	}

	public static void surroundingMiniGameScoreReset(final Player player) {
		MiniGame mg = MiniGameLoading.getMiniGameContainingLocation(player.getLocation());
		if (mg != null) {
			MessageSystem.sideNote("Resetting highScore for " + mg.getName(), player);
			ConfigAccess.getHighscoreConfig().set(String.valueOf(mg.getID()), null);
			ConfigAccess.saveHighscoreConfig();
			HighScoreManager.updateHighScoreList(mg);
		}
		else {
			MessageSystem.error("You are not inside a miniGame area", player);
		}
	}



	public static void rebuildMiniGameCfgFromSigns(final String worldName) {
		final World world = Bukkit.getWorld(worldName);
		if (world == null) {
			MessageSystem.consoleError("world not found: " + worldName);
			return;
		}

		//backup editors and hs sign locs
		Map<Integer, List<String>> allowedEditorsBackup = new HashMap<Integer, List<String>>();
		Map<Integer, List<Vector>> signLocsBackup = new HashMap<Integer, List<Vector>>();
		for (MiniGame mg: MiniGameLoading.getMiniGames()) {
			allowedEditorsBackup.put(mg.getID(), mg.getAllowedEditors());
			signLocsBackup.put(mg.getID(), mg.getHighScoreSignLocations());
			ConfigAccess.deleteMiniGameConfigFile(mg.getID());
		}
		MiniGameLoading.getMiniGames().clear();


		Block block;
		int numBlocks = 0;
		final List<AA_SignCommand> foundSignCommands = new ArrayList<AA_SignCommand>();
		MessageSystem.consoleDebug("searching SignCommands...");
		for (int x = HubControl.MINIGAME_HUB_MIN.getBlockX(); x <= HubControl.MINIGAME_HUB_MAX.getBlockX(); x++) {
			for (int y = HubControl.MINIGAME_HUB_MIN.getBlockY(); y <= HubControl.MINIGAME_HUB_MAX.getBlockY(); y++) {
				for (int z = HubControl.MINIGAME_HUB_MIN.getBlockZ(); z <= HubControl.MINIGAME_HUB_MAX.getBlockZ(); z++) {
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
		PluginManagement.executeDelayed(2, new Runnable() {

			@Override
			public void run() {
				MessageSystem.consoleDebug("loading borders...");
				for (AA_SignCommand cmd: foundSignCommands) {
					if (cmd.getCommand().equals("border")) {
						cmd.executeOnCreation(null, world);
					}
				}
				MessageSystem.consoleDebug("done...");
				PluginManagement.executeDelayed(2, new Runnable() {

					@Override
					public void run() {
						MessageSystem.consoleDebug("loading other commands...");
						for (AA_SignCommand cmd: foundSignCommands) {
							if (!cmd.getCommand().equals("border") &&
								!cmd.getCommand().equals("highScore") &&
								!cmd.isClickCommand()) {
								cmd.executeOnCreation(null, world);
							}
						}

						for (MiniGame mg: MiniGameLoading.getMiniGames()) {
							if (allowedEditorsBackup.containsKey(mg.getID())) {
								mg.setAllowedEditors(allowedEditorsBackup.get(mg.getID()));
							}
							if (signLocsBackup.containsKey(mg.getID())) {
								mg.addHighScoreSignLocations(signLocsBackup.get(mg.getID()));
							}
							mg.persist();
						}
						PluginManagement.executeDelayed(1, new Runnable() {

							@Override
							public void run() {
								HighScoreManager.updateHighScoreLists();
								MessageSystem.consoleDebug("all done.");
							}

						});
					}
				});
			}
		});
	}

}
