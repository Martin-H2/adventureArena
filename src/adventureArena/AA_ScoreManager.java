package adventureArena;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class AA_ScoreManager {

	enum ScoreType {
		DEATHS,
		KILLS_PVP,
		KILLS_PVE,
		PVP_RATING,
		PVE_RATING,
		CMD_RATING,
		LTS_RATING
	}
	private static final double DEFAULT_RATING = 100.0;
	private static final double AVERAGE_SCORESTEAL = 5.0;


	public static void updateHighScoreLists() {
		for (AA_MiniGame mg: AA_MiniGameControl.getMiniGames()){
			updateHighScoreList(mg);
		}
	}
	public static void updateHighScoreList(final AA_MiniGame mg) {
		List<Vector> signLocsToRemove = new ArrayList<Vector>();
		for (Vector v: mg.getHighScoreSignLocations()) {
			Block signBlock = v.toLocation(mg.getWorld()).getBlock();
			if (signBlock.getState() instanceof Sign) {
				Sign signState =  (Sign) signBlock.getState();
				String[] lines = signState.getLines();
				if (!lines[0].startsWith("[") && !lines[0].startsWith(ChatColor.RED + "==")) {
					AA_MessageSystem.consoleInfo("mg."+mg.getID()+": removing HighScoreSignLocation from " + v.toString() + " (wrong sign text)");
					signLocsToRemove.add(v);
					break;
				}
				signState.setLine(0, getHighScoreHeading(mg));
				signState.update();
				int globalLineIndex = 1;
				for (Entry<String, Double> entry: getSortedHighscoreList(mg)) {
					ChatColor color = ChatColor.BLACK;
					String playerName = entry.getKey();
					long score = Math.round(entry.getValue());
					if (globalLineIndex==1) {
						color = ChatColor.GREEN;
						String king = mg.getKing();
						if (!playerName.equals(king)) {
							AA_MessageSystem.broadcast(ChatColor.GOLD + "[" + mg.getName() + "] NEW HIGHSCORE - " + ChatColor.UNDERLINE + playerName
									+ ChatColor.GOLD + ": " + score);
						}
					}
					else if (globalLineIndex==2) {
						color = ChatColor.DARK_GREEN;
					}
					signState.setLine(globalLineIndex % 4, ChatColor.GRAY.toString() + globalLineIndex + "." + color.toString() + playerName + ": " + score);
					signState.update();
					globalLineIndex++;
					if (globalLineIndex % 4 == 0) {
						signState = getNextSignState(signBlock);
						if (signState==null) {
							break;
						}
					}
				}
			} else {
				AA_MessageSystem.consoleInfo("mg."+mg.getID()+": removing HighScoreSignLocation from " + v.toString() + " (no sign here)");
				signLocsToRemove.add(v);
				break;
			}
		}
		mg.getHighScoreSignLocations().removeAll(signLocsToRemove);
		if (mg.needsPersisting()) {
			mg.persist();
		}
	}

	private static Sign getNextSignState(final Block signBlock) {
		Block next = signBlock.getRelative(BlockFace.DOWN);
		if (next.getState() instanceof Sign)
			return (Sign) next.getState();
		else
			return null;
	}
	public static String getHighScoreHeading(final AA_MiniGame mg) {
		return ChatColor.RED + "==" + mg.getName() + "==";
	}
	private static List<Entry<String, Double>> getSortedHighscoreList(final AA_MiniGame mg) {
		Map<String, Double> sortedHighscoreList = new HashMap<String, Double>();
		ScoreType st = ScoreType.CMD_RATING;
		if (mg.getScoreMode() == ScoreMode.KillsPerDeath) {
			if (mg.isPvpDamage()) {
				st = ScoreType.PVP_RATING;
			} else {
				st = ScoreType.PVE_RATING;
			}
		}
		else if (mg.getScoreMode() == ScoreMode.LastManStanding) {
			st = ScoreType.LTS_RATING;
		}
		ConfigurationSection scores = getHighscoreConfig().getConfigurationSection( mg.getID() + "." + st.toString());
		for (String playerName: scores.getKeys(false)) {
			sortedHighscoreList.put(playerName, scores.getDouble(playerName));
		}
		return Util.sortByValue(sortedHighscoreList, false);
	}





	public static void onEntityDeath(final LivingEntity entity, final Player killer) {
		AA_MiniGame mg = AA_MiniGameControl.getMiniGameForPlayer(killer);
		if (mg==null) return;
		addScore(mg, ScoreType.KILLS_PVE, killer, entity.getMaxHealth()/killer.getMaxHealth());
	}

	public static void onPlayerDeath(final Player dyingPlayer) {
		AA_MiniGame mg = AA_MiniGameControl.getMiniGameForPlayer(dyingPlayer);
		if (mg==null) return;

		addScore(mg, ScoreType.DEATHS, dyingPlayer);

		Player killer = dyingPlayer.getKiller();
		if (killer!=null) {
			addScore(mg, ScoreType.KILLS_PVP, killer);
			double victimRating = getScore(mg, ScoreType.PVP_RATING, dyingPlayer, DEFAULT_RATING);
			double killerRating = getScore(mg, ScoreType.PVP_RATING, killer, DEFAULT_RATING);
			double stolenRating = ( (victimRating-killerRating)/DEFAULT_RATING + 1.0) * AVERAGE_SCORESTEAL;
			stolenRating = Math.max(0.0, Math.min(AVERAGE_SCORESTEAL*2.0, stolenRating));
			setScore(mg, ScoreType.PVP_RATING, dyingPlayer, victimRating - stolenRating);
			setScore(mg, ScoreType.PVP_RATING, killer, killerRating + stolenRating);
		}
	}

	public static void onPlayerLeft(final AA_MiniGame mg, final Player player) {
		if(mg.getScoreMode() == ScoreMode.LastManStanding) {
			int i = mg.getInitialNumberOfPlayers();
			int p = mg.getNumberOfPlayersRemaining();
			double score = 1-2*p/(i-1);
			AA_MessageSystem.consoleWarn("LastManStanding: "+player.getName()+" got "+score*AVERAGE_SCORESTEAL+" points ("+p+"/"+i+" still in game)");
			double ltsRating = getScore(mg, ScoreType.LTS_RATING, player, DEFAULT_RATING);
			setScore(mg, ScoreType.LTS_RATING, player, ltsRating+score*AVERAGE_SCORESTEAL);
		}
	}

	public static void onPlayerWin(final AA_MiniGame mg, final Player player) {
		if(mg.getScoreMode() == ScoreMode.LastManStanding) {
			AA_MessageSystem.consoleWarn("LastManStanding: "+player.getName()+" got "+AVERAGE_SCORESTEAL+" points for winning");
			double ltsRating = getScore(mg, ScoreType.LTS_RATING, player, DEFAULT_RATING);
			setScore(mg, ScoreType.LTS_RATING, player, ltsRating+AVERAGE_SCORESTEAL);
		}
	}




	// ################ CONFIG TO SCORE ##################


	private static double getScore(final AA_MiniGame mg, final ScoreType st, final Player p, final double defaultValue) {
		return getHighscoreConfig().getDouble(mg.getID() + "." + st.toString() + "." + p.getName() , defaultValue);
	}
	private static double getScore(final AA_MiniGame mg, final ScoreType st, final Player p) {
		return getScore(mg, st, p, 0.0);
	}
	private static void setScore(final AA_MiniGame mg, final ScoreType st, final Player p, final double newScore) {
		getHighscoreConfig().set(mg.getID() + "." + st.toString() + "." + p.getName(), newScore);
		saveHighscoreConfig();
	}
	private static void addScore(final AA_MiniGame mg, final ScoreType st, final Player p, final double addedScore) {
		setScore(mg, st, p, getScore(mg, st, p) + addedScore);
	}
	private static void addScore(final AA_MiniGame mg, final ScoreType st, final Player p) {
		addScore(mg, st, p, 1.0);
	}





	// ################ CONFIG UTIL ##################

	private static final String HIGHSCORES_CONFIGNAME = "highScores.yml";
	private static File configFile = null;
	private static YamlConfiguration config = null;

	public static FileConfiguration getPluginConfig() {
		return AdventureArena.getInstance().getConfig();
	}
	public static void savePluginConfig() {
		AdventureArena.getInstance().saveConfig();
	}
	public static FileConfiguration getHighscoreConfig() {
		if (config==null) {
			File dataFolder = AdventureArena.getInstance().getDataFolder();
			if (!dataFolder.exists()) {
				dataFolder.mkdirs();
			}
			configFile = new File(dataFolder, HIGHSCORES_CONFIGNAME);
			config = YamlConfiguration.loadConfiguration(configFile);
		}
		return config;
	}
	public static void saveHighscoreConfig() {
		if (config!=null) {
			try {
				config.save(configFile);
			} catch (IOException e) {
				AA_MessageSystem.consoleError(HIGHSCORES_CONFIGNAME + "cannot be overwritten or created");
				e.printStackTrace();
			}
		}
	}







}
