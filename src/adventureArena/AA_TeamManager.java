package adventureArena;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

public class AA_TeamManager {


	static Map<AA_MiniGame, Map<String,AA_TeamChallenge>> teamChallenges = new HashMap<AA_MiniGame, Map<String,AA_TeamChallenge>>();
	static Set<Player> lockedPlayers = new HashSet<Player>();
	public static final Scoreboard scoreBoard = Bukkit.getScoreboardManager().getMainScoreboard();
	static final String FFA_TEAM = "ffa";
	static final String EDITORS_TEAM = "edit";


	// ################ SCOREBOAD TEAMS #########################


	public static Team getTeam(final OfflinePlayer p) {
		if (p==null) return null;
		return scoreBoard.getPlayerTeam(p);
	}

	public static Team getTeam(final String idAndTeamName, final AA_MiniGame mg) {
		Team team = scoreBoard.getTeam(idAndTeamName);
		if (team==null) {
			team = registerTeam(idAndTeamName, mg);
		}
		return team;
	}

	private static Team registerTeam(final String idAndTeamName, final AA_MiniGame mg) {
		Team team = scoreBoard.registerNewTeam(idAndTeamName);
		team.setAllowFriendlyFire(idAndTeamName.endsWith(FFA_TEAM) && mg.isPvpDamage());
		team.setCanSeeFriendlyInvisibles(true);
		return team;
	}



	// ################ MINIGAME STARTING #########################

	public static void doChecksAndRegisterTeam(final AA_MiniGame miniGame, final String teamName, final Location origin, final double radius) {
		// COLLECT PLAYERS NEARBY
		final ArrayList<Player> playersAroundSign = new ArrayList<Player>();
		for (Player p: Bukkit.getOnlinePlayers()) {
			if (origin.distance(p.getLocation())<=radius && AA_MiniGameControl.isWatchingMiniGames(p)) {
				playersAroundSign.add(p);
			}
		}
		if (playersAroundSign.size()==0) return;
		if (playersAroundSign.size()<=1 && !miniGame.isSoloPlayable()) {
			AA_MessageSystem.errorForGroup("You need more people for this arena.", playersAroundSign);
			return;
		}


		if (AA_MiniGameControl.canJoinMiniGame(miniGame, playersAroundSign) && AA_TeamManager.canTeamJoinMiniGame(miniGame, teamName, playersAroundSign)) {
			if (teamName.equals(FFA_TEAM)) {
				if (teamChallengeActive(miniGame)) {
					AA_MessageSystem.errorForGroup("Team-challenge pending, use team entrance.", playersAroundSign);
				} else {
					AA_TeamManager.startMiniGameForTeam(miniGame, teamName, playersAroundSign);
				}
				return;
			}

			boolean newChallengeForThisMiniGame = registerTeamChallenge(miniGame, teamName, playersAroundSign);
			if (newChallengeForThisMiniGame) {
				AA_MessageSystem.gameplayWarningForGroup("Team " + teamName + " wants to start '" + miniGame.getName() + "'", AA_MiniGameControl.getAllGameHubSpectators());
				AdventureArena.executeDelayed(10, new Runnable() {
					@Override
					public void run() {
						Map<String, AA_TeamChallenge> challengingTeamsForThisMiniGame = getChallengingTeamsForMiniGame(miniGame);
						removeChallengeFor(miniGame);
						if (challengingTeamsForThisMiniGame.size()>1) {
							// START GAME NOW !
							if (AA_MiniGameControl.canJoinMiniGame(miniGame, getAllChallengersFor(miniGame))) {
								miniGame.wipeEntities();
								for (String teamName: challengingTeamsForThisMiniGame.keySet()) {
									AA_TeamChallenge team = challengingTeamsForThisMiniGame.get(teamName);
									AA_TeamManager.startMiniGameForTeam(miniGame, team.getTeamName(), team.getPlayers());
								}
							}
						} else {
							AA_MessageSystem.gameplayWarningForGroup("Nobody answered the challenge for '" + miniGame.getName() + "'", AA_MiniGameControl.getAllGameHubSpectators());
						}
					}
				});
			} else {
				AA_MessageSystem.gameplayWarningForGroup("Team " + teamName + " accepts the challenge for '" + miniGame.getName() + "'", AA_MiniGameControl.getAllGameHubSpectators());
			}
		}
	}
	static boolean canTeamJoinMiniGame(final AA_MiniGame miniGame, final String teamName, final ArrayList<Player> players) {
		List<Vector> teamSpawns = miniGame.getSpawnPoints(teamName);
		// SPAWNS CHECK
		if (teamSpawns==null || teamSpawns.isEmpty()) {
			AA_MessageSystem.errorForGroup("No spawnpoints found for team " + teamName, players);
			AA_MessageSystem.sideNoteForGroup(players.size() + " players failed joining your " + miniGame.getName() + ". (no spawPoints found)", miniGame.getAllowedEditors());
			return false;
		}
		return true;
	}
	static void startMiniGameForTeam(final AA_MiniGame miniGame, final String teamName, final ArrayList<Player> team) {
		if (!canTeamJoinMiniGame(miniGame, teamName, team)) return;

		List<Vector> teamSpawns = miniGame.getSpawnPoints(teamName);

		for (int i=0; i<team.size(); i++) {
			Player p = team.get(i);
			if (i<teamSpawns.size()) {
				AA_MiniGameControl.joinMiniGame(miniGame, teamName,  p, teamSpawns.get(i));
			} else {
				AA_MessageSystem.error("No more spawnpoints in " + miniGame.getName() + " for team " + teamName + ". You can't participate.", p);
			}
		}

		for (AA_MonsterTrigger mt: miniGame.getStartMonsterTriggers()) {
			mt.checkAndTrigger(miniGame.getWorld());
		}

	}




	// ################ CHALLENGES #########################

	public static boolean registerTeamChallenge(final AA_MiniGame miniGame, final String teamName, final ArrayList<Player> players) {
		boolean newChallengeForThisMiniGame;

		Map<String, AA_TeamChallenge> challengingTeamsForThisMiniGame = AA_TeamManager.teamChallenges.get(miniGame);
		if (challengingTeamsForThisMiniGame==null) {
			challengingTeamsForThisMiniGame = new HashMap<String, AA_TeamChallenge>();
			newChallengeForThisMiniGame = true;
		} else {
			newChallengeForThisMiniGame = false;
		}

		AA_TeamChallenge team = challengingTeamsForThisMiniGame.get(teamName);
		if (team == null ) {
			team = new AA_TeamChallenge(teamName);
		}

		for (Player p: players) {
			if (!AA_TeamManager.lockedPlayers.contains(p)) { //every player only once
				team.players.add(p);
				AA_TeamManager.lockedPlayers.add(p);
			}
		}
		challengingTeamsForThisMiniGame.put(teamName, team);
		AA_TeamManager.teamChallenges.put(miniGame, challengingTeamsForThisMiniGame);
		return newChallengeForThisMiniGame;
	}

	public static Map<String, AA_TeamChallenge> getChallengingTeamsForMiniGame(final AA_MiniGame miniGame) {
		return teamChallenges.get(miniGame);
	}
	public static boolean teamChallengeActive(final AA_MiniGame miniGame) {
		return teamChallenges.get(miniGame)!=null;
	}
	public static ArrayList<Player> getAllChallengersFor(final AA_MiniGame miniGame) {
		Map<String, AA_TeamChallenge> challengingTeamsForThisMiniGame = teamChallenges.get(miniGame);
		ArrayList<Player> allChallengers = new ArrayList<Player>();

		if (challengingTeamsForThisMiniGame==null) return allChallengers;
		for (AA_TeamChallenge team: challengingTeamsForThisMiniGame.values()) {
			allChallengers.addAll(team.players);
		}
		return allChallengers;
	}
	public static void removeChallengeFor(final AA_MiniGame miniGame) {
		for (Player p: getAllChallengersFor(miniGame)) {
			lockedPlayers.remove(p);
		}
		teamChallenges.remove(miniGame);
	}






}
