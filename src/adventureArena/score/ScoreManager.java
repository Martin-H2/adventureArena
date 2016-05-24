package adventureArena.score;

import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import adventureArena.ConfigAccess;
import adventureArena.control.MiniGameLoading;
import adventureArena.control.PlayerControl;
import adventureArena.enums.HighScoreMode;
import adventureArena.enums.ScoreType;
import adventureArena.messages.MessageSystem;
import adventureArena.messages.OnScreenMessages;
import adventureArena.miniGameComponents.MiniGame;

public class ScoreManager {

	private static final double	DEFAULT_RATING		= 500.0;
	private static final double	AVERAGE_SCORESTEAL	= 25.0;


	// ################ SCORE EVENTS ##################

	/**
	 * The dying player should not have been removed from the miniGame yet for score calculation !
	 *
	 * @param dyingPlayer
	 *
	 */
	public static void onPlayerDeath(final Player dyingPlayer) {
		MiniGame mg = MiniGameLoading.getMiniGameForPlayer(dyingPlayer);
		if (mg == null) return;

		// DEATH
		addScore(mg, ScoreType.DEATHS, dyingPlayer);

		if (mg.isPvp()) {
			// PVP
			Player killer = dyingPlayer.getKiller();
			if (killer == null && mg.getNumberOfEnemyPlayersRemaining(dyingPlayer) == 1) {
				for (Player remainingPlayer: mg.getPlayersRemaining()) {
					if (!dyingPlayer.equals(remainingPlayer)) {
						killer = remainingPlayer;
						MessageSystem.warningToGroup("detected suicide in 1vs1, counting as kill for " + killer.getName(), mg.getPlayersRemaining());
						break;
					}
				}
			}
			if (killer != null) {
				addScore(mg, ScoreType.KILLS_PVP, killer);
				double victimRating = getScore(mg, ScoreType.PVP_RATING, dyingPlayer, DEFAULT_RATING);
				double killerRating = getScore(mg, ScoreType.PVP_RATING, killer, DEFAULT_RATING);
				double stolenRating = ((victimRating - killerRating) / DEFAULT_RATING * 2 + 1.0) * AVERAGE_SCORESTEAL;
				stolenRating = Math.max(0.0, Math.min(AVERAGE_SCORESTEAL * 2.0, stolenRating));
				double newKillerRating = killerRating + stolenRating;
				double newVictimRating = victimRating - stolenRating;
				killer.sendMessage(String.format(
						ChatColor.DARK_GRAY + "[Rating] " + ChatColor.GREEN + "You:%.0f->%.0f  " + ChatColor.GRAY + dyingPlayer.getName() + ":%.0f->%.0f",
						killerRating, newKillerRating, victimRating, newVictimRating));
				dyingPlayer.sendMessage(String.format(
						ChatColor.DARK_GRAY + "[Rating] " + ChatColor.RED + "You:%.0f->%.0f  " + ChatColor.GRAY + killer.getName() + ":%.0f->%.0f",
						victimRating, newVictimRating, killerRating, newKillerRating));
				setScore(mg, ScoreType.PVP_RATING, killer, newKillerRating);
				setScore(mg, ScoreType.PVP_RATING, dyingPlayer, newVictimRating);
				OnScreenMessages.sendPvpKillMessages(killer, dyingPlayer, killerRating, newKillerRating, victimRating, newVictimRating, mg);
			}
		}
		else {
			// PVE
			double pveKills = getScore(mg, ScoreType.KILLS_PVE, dyingPlayer);
			double deaths = getScore(mg, ScoreType.DEATHS, dyingPlayer);
			setScore(mg, ScoreType.PVE_RATING, dyingPlayer, pveKills / (deaths + 1));
		}
	}

	public static void onEntityDeath(final LivingEntity entity, final Player killer) { //TODO use rating system like PVP one
		MiniGame mg = MiniGameLoading.getMiniGameForPlayer(killer);
		if (mg == null) return;
		addScore(mg, ScoreType.KILLS_PVE, killer, entity.getMaxHealth() / killer.getMaxHealth());
		double pveKills = getScore(mg, ScoreType.KILLS_PVE, killer);
		double deaths = getScore(mg, ScoreType.DEATHS, killer);
		setScore(mg, ScoreType.PVE_RATING, killer, pveKills / (deaths + 1));
	}

	public static void onPlayerLeft(final MiniGame mg, final Player player) {
		if (mg.getScoreMode() == HighScoreMode.LastManStanding) {
			double i = mg.getInitialNumberOfPlayers();
			double p = mg.getNumberOfPlayersRemaining();
			double score = 1.0 - 2.0 * p / (i - 1.0);
			if (score == Double.NaN) {
				score = 0;
			}
			MessageSystem.consoleDebug("LastManStanding: " + player.getName() + " got " + score * AVERAGE_SCORESTEAL + " points (" + p + "/" + i + " still in game)");
			double ltsRating = getScore(mg, ScoreType.LTS_RATING, player, DEFAULT_RATING);
			setScore(mg, ScoreType.LTS_RATING, player, ltsRating + score * AVERAGE_SCORESTEAL);
		}
	}

	public static void onPlayerWin(final MiniGame mg, final Player player) {
		if (mg.getScoreMode() == HighScoreMode.LastManStanding) {
			MessageSystem.consoleDebug("LastManStanding: " + player.getName() + " got " + AVERAGE_SCORESTEAL + " points for winning");
			double ltsRating = getScore(mg, ScoreType.LTS_RATING, player, DEFAULT_RATING);
			setScore(mg, ScoreType.LTS_RATING, player, ltsRating + AVERAGE_SCORESTEAL);
		}
	}

	public static void onSetScoreCmd(final Player p, final double newScore) {
		MiniGame mg = MiniGameLoading.getMiniGameForPlayer(p);
		if (mg != null && PlayerControl.isPlayingMiniGame(p) && mg.getScoreMode() == HighScoreMode.ScoreByCommand) {
			double oldScore = getScore(mg, ScoreType.CMD_RATING, p, 0.0);
			if (newScore > oldScore) {
				MessageSystem.success("Your '" + mg.getName() + "' score is now: " + ChatColor.GOLD + Math.round(newScore), p);
				setScore(mg, ScoreType.CMD_RATING, p, newScore);
			}
		}
	}



	// ################ CONFIG INTERFACE ##################

	static double getScore(final MiniGame mg, final ScoreType st, final Player p, final double defaultValue) {
		return ConfigAccess.getHighscoreConfig().getDouble(mg.getID() + "." + st.toString() + "." + p.getName(), defaultValue);
	}

	private static double getScore(final MiniGame mg, final ScoreType st, final Player p) {
		return getScore(mg, st, p, 0.0);
	}

	private static void setScore(final MiniGame mg, final ScoreType st, final Player p, final double newScore) {
		ConfigAccess.getHighscoreConfig().set(mg.getID() + "." + st.toString() + "." + p.getName(), newScore);
		ConfigAccess.saveHighscoreConfig();
	}

	private static void addScore(final MiniGame mg, final ScoreType st, final Player p, final double addedScore) {
		setScore(mg, st, p, getScore(mg, st, p) + addedScore);
	}

	private static void addScore(final MiniGame mg, final ScoreType st, final Player p) {
		addScore(mg, st, p, 1.0);
	}



}
