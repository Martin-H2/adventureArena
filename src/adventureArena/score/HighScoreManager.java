package adventureArena.score;

import java.util.*;
import java.util.Map.Entry;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import adventureArena.ConfigAccess;
import adventureArena.Util;
import adventureArena.control.MiniGameLoading;
import adventureArena.enums.ScoreType;
import adventureArena.messages.MessageSystem;
import adventureArena.miniGameComponents.MiniGame;


public class HighScoreManager {


	public static double getHighScoreRating(final MiniGame mg, final Player p) {
		return ScoreManager.getScore(mg, mg.getScoreTypeForHighscore(), p, 0.0);
	}


	public static void updateHighScoreLists() {
		for (MiniGame mg: MiniGameLoading.getMiniGames()) {
			updateHighScoreList(mg);
		}
	}

	public static void updateHighScoreList(final MiniGame mg) {
		List<Vector> signLocsToRemove = new ArrayList<Vector>();
		for (Vector v: mg.getHighScoreSignLocations()) {
			Block signBlock = v.toLocation(mg.getWorld()).getBlock();
			if (signBlock.getState() instanceof Sign) {
				Sign signState = (Sign) signBlock.getState();
				String[] lines = signState.getLines();
				if (!lines[0].startsWith("[") && !lines[0].startsWith(ChatColor.RED + "==")) {
					MessageSystem.consoleInfo("mg." + mg.getID() + ": removing HighScoreSignLocation from " + v.toString() + " (wrong sign text)");
					signLocsToRemove.add(v);
					break;
				}
				signState.setLine(0, getHighScoreHeading(mg));
				signState.update();
				int globalLineIndex = 1;
				List<Entry<String, Double>> sortedHighscoreList = getSortedHighscoreList(mg);
				while (signState != null) {
					int index = globalLineIndex - 1;
					if (index < sortedHighscoreList.size()) {
						Entry<String, Double> entry = sortedHighscoreList.get(globalLineIndex - 1);
						ChatColor color = ChatColor.BLACK;
						String playerName = entry.getKey();
						long score = Math.round(entry.getValue());
						if (globalLineIndex == 1) {
							color = ChatColor.BLUE;
							String oldKing = mg.getKing();
							if (!playerName.equals(oldKing)) {
								MessageSystem.broadcast(ChatColor.GOLD + "[" + mg.getName() + "] NEW HIGHSCORE LEADER: " + ChatColor.RED + playerName + " with " + score);
								mg.setKing(playerName);
							}
						}
						else if (globalLineIndex == 2) {
							color = ChatColor.DARK_BLUE;
						}
						signState.setLine(globalLineIndex % 4, ChatColor.GRAY.toString() + globalLineIndex + "." + color.toString() + playerName.substring(0, Math.min(10, playerName.length())) + ": " + score);
					}
					else {
						signState.setLine(globalLineIndex % 4, " ");
					}

					signState.update();
					globalLineIndex++;
					if (globalLineIndex % 4 == 0) {
						signState = getSignStateBelow(signBlock, globalLineIndex / 4);
					}
				}
			}
			else {
				MessageSystem.consoleInfo("mg." + mg.getID() + ": removing HighScoreSignLocation from " + v.toString() + " (no sign here)");
				signLocsToRemove.add(v);
				break;
			}
		}
		mg.getHighScoreSignLocations().removeAll(signLocsToRemove);
		if (mg.needsPersisting()) {
			mg.persist();
		}
	}



	static List<Entry<String, Double>> getSortedHighscoreList(final MiniGame mg) {
		Map<String, Double> sortedHighscoreList = new HashMap<String, Double>();
		ScoreType st = mg.getScoreTypeForHighscore();
		ConfigurationSection scores = ConfigAccess.getHighscoreConfig().getConfigurationSection(mg.getID() + "." + st.toString());
		if (scores != null) {
			for (String playerName: scores.getKeys(false)) {
				sortedHighscoreList.put(playerName, scores.getDouble(playerName));
			}
		}
		return Util.sortByValue(sortedHighscoreList, false);
	}


	static Sign getSignStateBelow(final Block signBlock, final int i) {
		Block next = signBlock.getRelative(BlockFace.DOWN, i);
		if (next.getState() instanceof Sign) return (Sign) next.getState();
		else return null;
	}

	public static String getHighScoreHeading(final MiniGame mg) {
		return ChatColor.RED + "==" + mg.getName() + "==";
	}

}
