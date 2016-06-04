package adventureArena;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import adventureArena.messages.MessageSystem;


public class ConfigAccess {

	//	private static final String			MINIGAMES_CONFIGNAME			= "miniGames.yml";
	//	private static File					minigamesConfigFile				= null;
	//	private static YamlConfiguration	minigamesConfig					= null;
	private static final String						MINIGAMES_CONFIG_FOLDER			= "miniGameConfigs";
	private static Map<Integer, YamlConfiguration>	minigamesConfigs				= new HashMap<Integer, YamlConfiguration>();

	private static final String						HIGHSCORES_CONFIGNAME			= "highScores.yml";
	private static File								highScoreConfigFile				= null;
	private static YamlConfiguration				highScoreConfig					= null;
	private static boolean							highScoreConfigNeedsSaving		= false;
	private static int								highScoreSavingTaskId			= -1;

	private static final String						SAVED_INVENTORIES_CONFIG_NAME	= "savedPlayerInventories.yml";
	private static File								savedInventoriesConfigFile		= null;
	private static YamlConfiguration				savedInventoriesConfig			= null;



	public static FileConfiguration getPluginConfig() {
		return PluginManagement.getInstance().getConfig();
	}

	public static void savePluginConfig() {
		PluginManagement.getInstance().saveConfig();
	}



	public static FileConfiguration getMiniGameConfig(int id) {
		YamlConfiguration mgc = null;
		if (minigamesConfigs.containsKey(id)) {
			mgc = minigamesConfigs.get(id);
		}
		else {
			MessageSystem.consoleDebug("loading minigame cfg #" + id);
			mgc = YamlConfiguration.loadConfiguration(getMiniGameConfigFile(id));
			minigamesConfigs.put(id, mgc);
		}
		return mgc;
	}

	public static void saveMiniGameConfig(int id) {
		if (minigamesConfigs.containsKey(id)) {
			try {
				//MessageSystem.consoleDebug("saving minigame cfg #" + id); //FIXME sync
				minigamesConfigs.get(id).save(getMiniGameConfigFile(id));
			}
			catch (IOException e) {
				MessageSystem.consoleError(getMiniGameConfigFile(id) + "cannot be overwritten or created");
				e.printStackTrace();
			}
		}
	}

	private static File getMiniGameConfigFile(final int id) {
		return new File(getMiniGameConfigFolder(), File.separator + id + ".yml");
	}

	public static File getMiniGameConfigFolder() {
		File miniGameConfigFolder = new File(PluginManagement.getInstance().getDataFolder(), MINIGAMES_CONFIG_FOLDER);
		if (!miniGameConfigFolder.exists()) {
			miniGameConfigFolder.mkdirs();
		}
		return miniGameConfigFolder;
	}

	public static boolean miniGameConfigFileExists(final int id) {
		return getMiniGameConfigFile(id).length() > 0L;
	}

	public static void deleteMiniGameConfigFile(int id) {
		MessageSystem.consoleDebug("deleting minigame cfgfile #" + id);
		getMiniGameConfigFile(id).delete();
	}



	public static FileConfiguration getHighscoreConfig() {
		if (highScoreConfig == null) {
			highScoreConfigFile = new File(getConfigFolder(), HIGHSCORES_CONFIGNAME);
			highScoreConfig = YamlConfiguration.loadConfiguration(highScoreConfigFile);
		}
		return highScoreConfig;
	}

	public static void saveHighscoreConfig() {
		if (highScoreConfig != null) {
			highScoreConfigNeedsSaving = true;
			if (highScoreSavingTaskId == -1) {
				highScoreSavingTaskId = PluginManagement.executePeriodically(5, new Runnable() {

					// preventing HDD access spam on mob kills due to score updates
					@Override
					public void run() {
						if (highScoreConfigNeedsSaving) {
							try {
								highScoreConfig.save(highScoreConfigFile);
								//MessageSystem.consoleDebug("saving " + highScoreConfigFile.getName());
							}
							catch (IOException e) {
								MessageSystem.consoleError(HIGHSCORES_CONFIGNAME + "cannot be overwritten or created");
								e.printStackTrace();
							}
							highScoreConfigNeedsSaving = false;
						}
						else {
							int temp = highScoreSavingTaskId;
							highScoreSavingTaskId = -1;
							PluginManagement.cancelTask(temp);
						}
					}
				});
			}


		}
	}



	public static FileConfiguration getSavedInventoriesConfig() {
		if (savedInventoriesConfig == null) {
			savedInventoriesConfigFile = new File(getConfigFolder(), SAVED_INVENTORIES_CONFIG_NAME);
			savedInventoriesConfig = YamlConfiguration.loadConfiguration(savedInventoriesConfigFile);
		}
		return savedInventoriesConfig;
	}


	public static void saveSavedInventoriesConfig() {
		if (savedInventoriesConfig != null) {
			try {
				savedInventoriesConfig.save(savedInventoriesConfigFile);
			}
			catch (IOException e) {
				MessageSystem.consoleError(SAVED_INVENTORIES_CONFIG_NAME + "cannot be overwritten or created");
				e.printStackTrace();
			}
		}
	}


	static File getConfigFolder() {
		File dataFolder = PluginManagement.getInstance().getDataFolder();
		if (!dataFolder.exists()) {
			dataFolder.mkdirs();
		}
		return dataFolder;
	}



}
