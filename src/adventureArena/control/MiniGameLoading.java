package adventureArena.control;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import adventureArena.ConfigAccess;
import adventureArena.enums.ConfigPaths;
import adventureArena.messages.MessageSystem;
import adventureArena.miniGameComponents.MiniGame;


public class MiniGameLoading {

	private static List<MiniGame>	miniGames	= null;


	public static void loadMiniGamesFromConfig() {
		miniGames = new ArrayList<MiniGame>();
		for (String path: ConfigAccess.getMiniGameConfig().getRoot().getKeys(false)) {
			MiniGame mg = MiniGame.loadFromConfig(Integer.parseInt(path));
			if (mg != null) {
				miniGames.add(mg);
				if (mg.isPlaySessionActive()) {
					MessageSystem.consoleWarn("miniGame was still in progress: '" + mg.getName() + "', cleaning up...");
					mg.setInProgress(false);
					mg.wipeEntities();
					mg.wipeSessionVariables();
					//mg.restoreEnvironmentBackup();
				}
			}
		}
		for (Player p: Bukkit.getOnlinePlayers()) {
			MiniGameSessions.kickIfInsideMiniGame(p);
		}
	}


	public static List<MiniGame> getMiniGames() {
		if (miniGames == null) {
			loadMiniGamesFromConfig();
		}
		return miniGames;
	}

	public static void addMiniGame(final MiniGame miniGame) {
		getMiniGames().add(miniGame);
	}

	public static MiniGame getMiniGame(final int id) {
		for (MiniGame mg: getMiniGames()) {
			if (mg.getID() == id) return mg;
		}
		return null;
	}

	public static MiniGame getMiniGameForPlayer(final Player player) {
		FileConfiguration config = ConfigAccess.getPluginConfig();
		MiniGame mg = null;
		if (config.contains(ConfigPaths.activeMinigameId + "." + player.getName())) {
			int id = config.getInt(ConfigPaths.activeMinigameId + "." + player.getName());
			mg = getMiniGame(id);
		}
		//		if (mg == null) {
		//			mg = getMiniGameContainingLocation(player.getLocation());
		//		}
		return mg;
	}

	public static MiniGame getMiniGameContainingLocation(final Location location) {
		for (MiniGame miniGame: getMiniGames()) {
			if (miniGame.isInsideBounds(location))
				return miniGame;
		}
		return null;
	}

	public static int getNumberOfMiniGames() {
		return getMiniGames() != null ? getMiniGames().size() : 0;
	}

}
