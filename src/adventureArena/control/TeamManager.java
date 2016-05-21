package adventureArena.control;

import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;
import adventureArena.PluginManagement;
import adventureArena.MessageSystem;
import adventureArena.miniGameComponents.MiniGame;

public class TeamManager {


	static Map<MiniGame, Map<String, TeamChallenge>>	teamChallenges	= new HashMap<MiniGame, Map<String, TeamChallenge>>();
	static Set<Player>									lockedPlayers	= new HashSet<Player>();
	public static final Scoreboard						scoreBoard		= Bukkit.getScoreboardManager().getMainScoreboard();
	public static final String							FFA_TEAM		= "ffa";
	static final String									EDITORS_TEAM	= "edit";


	// ################ SCOREBOAD TEAMS #########################


	@SuppressWarnings ("deprecation")
	public static Team getTeam(final OfflinePlayer p) {
		if (p == null) return null;
		return scoreBoard.getPlayerTeam(p);
	}

	public static Team getTeam(final String idAndTeamName, final MiniGame mg) {
		Team team = scoreBoard.getTeam(idAndTeamName);
		if (team == null) {
			team = registerTeam(idAndTeamName, mg);
		}
		return team;
	}

	private static Team registerTeam(final String idAndTeamName, final MiniGame mg) {
		Team team = scoreBoard.registerNewTeam(idAndTeamName);
		team.setAllowFriendlyFire(true);
		team.setCanSeeFriendlyInvisibles(false);
		return team;
	}



	// ################ MINIGAME STARTING #########################

	public static void doChecksAndRegisterTeam(final MiniGame miniGame, final String teamName, final Location origin, final double radius) {
		// COLLECT PLAYERS NEARBY
		final ArrayList<Player> playersAroundSign = new ArrayList<Player>();
		for (Player p: Bukkit.getOnlinePlayers()) {
			if (PlayerControl.isWatchingMiniGames(p) && origin.distance(p.getLocation()) <= radius) {
				playersAroundSign.add(p);
			}
		}
		if (playersAroundSign.size() == 0) return;
		if (playersAroundSign.size() <= 1 && !miniGame.isSoloPlayable()) {
			MessageSystem.errorToGroup("You need more people for this arena.", playersAroundSign);
			return;
		}
		if (!miniGame.isSoloPlayable() && miniGame.getNumberOfSpawnPoints() <= 1) {
			MessageSystem.errorToGroup("Not enough spawnpoints installed for teamplay / ffa.", playersAroundSign);
			return;
		}


		if (MiniGameSessions.canJoinMiniGame(miniGame, playersAroundSign) && canTeamJoinMiniGame(miniGame, teamName, playersAroundSign)) {
			if (teamName.equals(FFA_TEAM)) {
				if (teamChallengeActive(miniGame)) {
					MessageSystem.errorToGroup("Team-challenge pending, use team entrance.", playersAroundSign);
				}
				else {
					MiniGameSessions.prepareSession(miniGame);
					spawnTeam(miniGame, teamName, playersAroundSign);
					miniGame.runStartTriggers();
				}
				return;
			}

			boolean newChallengeForThisMiniGame = registerTeamChallenge(miniGame, teamName, playersAroundSign);
			if (newChallengeForThisMiniGame) {
				MessageSystem.warningToGroup("Team " + teamName + " wants to start '" + miniGame.getName() + "'", HubControl.getAllGameHubSpectators());
				PluginManagement.executeDelayed(10, new Runnable() {

					@Override
					public void run() {
						Map<String, TeamChallenge> challengingTeamsForThisMiniGame = getChallengingTeamsForMiniGame(miniGame);
						removeChallengeFor(miniGame);
						if (challengingTeamsForThisMiniGame.size() > 1) {
							// START GAME NOW !
							if (MiniGameSessions.canJoinMiniGame(miniGame, getAllChallengersFor(miniGame))) {
								MiniGameSessions.prepareSession(miniGame);
								for (String teamName: challengingTeamsForThisMiniGame.keySet()) {
									TeamChallenge team = challengingTeamsForThisMiniGame.get(teamName);
									TeamManager.spawnTeam(miniGame, team.getTeamName(), team.getPlayers());
								}
								miniGame.runStartTriggers();
							}
						}
						else {
							MessageSystem.warningToGroup("Nobody answered the challenge for '" + miniGame.getName() + "'", HubControl.getAllGameHubSpectators());
						}
					}
				});
			}
			else {
				MessageSystem.warningToGroup("Team " + teamName + " accepts the challenge for '" + miniGame.getName() + "'", HubControl.getAllGameHubSpectators());
			}
		}
	}

	static boolean canTeamJoinMiniGame(final MiniGame miniGame, final String teamName, final ArrayList<Player> players) {
		List<Vector> teamSpawns = miniGame.getSpawnPoints(teamName);
		// SPAWNS CHECK
		if (teamSpawns == null || teamSpawns.isEmpty()) {
			MessageSystem.errorToGroup("No spawnpoints found for team " + teamName, players);
			MessageSystem.sideNoteToGroup(players.size() + " players failed joining your " + miniGame.getName() + ". (no spawPoints found)", miniGame.getAllowedEditors());
			return false;
		}
		return true;
	}

	static void spawnTeam(final MiniGame miniGame, final String teamName, final ArrayList<Player> team) {
		if (!canTeamJoinMiniGame(miniGame, teamName, team)) return;

		List<Vector> teamSpawns = miniGame.getSpawnPoints(teamName);

		for (int i = 0; i < team.size(); i++) {
			Player p = team.get(i);
			if (i < teamSpawns.size()) {
				MiniGameSessions.joinPlaySession(miniGame, teamName, p, teamSpawns.get(i));
			}
			else {
				MessageSystem.error("No more spawnpoints in " + miniGame.getName() + " for team " + teamName + ". You can't participate.", p);
			}
		}
	}



	// ################ CHALLENGES #########################

	public static boolean registerTeamChallenge(final MiniGame miniGame, final String teamName, final ArrayList<Player> players) {
		boolean newChallengeForThisMiniGame;

		Map<String, TeamChallenge> challengingTeamsForThisMiniGame = TeamManager.teamChallenges.get(miniGame);
		if (challengingTeamsForThisMiniGame == null) {
			challengingTeamsForThisMiniGame = new HashMap<String, TeamChallenge>();
			newChallengeForThisMiniGame = true;
		}
		else {
			newChallengeForThisMiniGame = false;
		}

		TeamChallenge team = challengingTeamsForThisMiniGame.get(teamName);
		if (team == null) {
			team = new TeamChallenge(teamName);
		}

		for (Player p: players) {
			if (!TeamManager.lockedPlayers.contains(p)) { //every player only once
				team.players.add(p);
				TeamManager.lockedPlayers.add(p);
			}
		}
		challengingTeamsForThisMiniGame.put(teamName, team);
		TeamManager.teamChallenges.put(miniGame, challengingTeamsForThisMiniGame);
		return newChallengeForThisMiniGame;
	}

	public static Map<String, TeamChallenge> getChallengingTeamsForMiniGame(final MiniGame miniGame) {
		return teamChallenges.get(miniGame);
	}

	public static boolean teamChallengeActive(final MiniGame miniGame) {
		return teamChallenges.get(miniGame) != null;
	}

	public static ArrayList<Player> getAllChallengersFor(final MiniGame miniGame) {
		Map<String, TeamChallenge> challengingTeamsForThisMiniGame = teamChallenges.get(miniGame);
		ArrayList<Player> allChallengers = new ArrayList<Player>();

		if (challengingTeamsForThisMiniGame == null) return allChallengers;
		for (TeamChallenge team: challengingTeamsForThisMiniGame.values()) {
			allChallengers.addAll(team.players);
		}
		return allChallengers;
	}

	public static void removeChallengeFor(final MiniGame miniGame) {
		for (Player p: getAllChallengersFor(miniGame)) {
			lockedPlayers.remove(p);
		}
		teamChallenges.remove(miniGame);
	}

	public static boolean isSameTeam(final Player p1, final Player p2) {
		Team t1 = getTeam(p1);
		if (t1 == null) return false;
		return t1.equals(getTeam(p2));
	}

	public static boolean isFFaTeam(final Player p) {
		Team t = getTeam(p);
		return t != null && t.getName().endsWith(FFA_TEAM);
	}

	public static boolean isAllied(final Player p1, final Player p2) {
		return isSameTeam(p1, p2) && !TeamManager.isFFaTeam(p1);
	}



}
