package adventureArena.control;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import adventureArena.ConfigAccess;
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

}
