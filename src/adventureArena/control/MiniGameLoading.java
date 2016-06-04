package adventureArena.control;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import adventureArena.ConfigAccess;
import adventureArena.Util;
import adventureArena.enums.ConfigPaths;
import adventureArena.messages.MessageSystem;
import adventureArena.miniGameComponents.MiniGame;


public class MiniGameLoading {

	private static List<MiniGame>	miniGames	= null;


	public static void loadMiniGamesFromConfig() {
		miniGames = new ArrayList<MiniGame>();

		File[] minigameConfigFiles = ConfigAccess.getMiniGameConfigFolder().listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".yml");
			}
		});

		for (File mgcf: minigameConfigFiles) {
			try {
				int id = Integer.parseInt(Util.stripExtension(mgcf.getName()));
				MiniGame mg = MiniGame.loadFromConfig(id);
				if (mg != null) {
					miniGames.add(mg);
					if (mg.isAnySessionActive()) {
						MessageSystem.consoleWarn("miniGame was still in progress: '" + mg.getName() + "', cleaning up...");
						mg.wipeEntities();
						mg.wipeSessionVariables();
						//mg.restoreEnvironmentBackup(); //FIXME rethink
					}
				}
			}
			catch (NumberFormatException e) {
				MessageSystem.consoleError("filename does not comprise an integer ID: " + mgcf.getAbsolutePath());
			}
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
