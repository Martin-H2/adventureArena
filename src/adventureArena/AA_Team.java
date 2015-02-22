package adventureArena;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.Player;

public class AA_Team {

	private static Map<AA_MiniGame, Map<String,AA_Team>> teamChallenges = new HashMap<AA_MiniGame, Map<String,AA_Team>>();


	private final String teamName;
	private final ArrayList<Player> players;

	public AA_Team(final String teamName, final ArrayList<Player> players) {
		this.teamName = teamName;
		this.players = players;
	}


	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof AA_Team)) return false;
		return teamName.equals(((AA_Team)obj).teamName);
	}





	public static boolean registerTeamChallenge(final AA_MiniGame miniGame, final String teamName, final ArrayList<Player> players) {
		boolean newChallengeForThisMiniGame;

		Map<String, AA_Team> challengingTeamsForThisMiniGame = teamChallenges.get(miniGame);
		if (challengingTeamsForThisMiniGame==null) {
			challengingTeamsForThisMiniGame = new HashMap<String, AA_Team>();
			newChallengeForThisMiniGame = true;
		} else {
			newChallengeForThisMiniGame = false;
		}

		AA_Team team = challengingTeamsForThisMiniGame.get(teamName);
		if (team == null ) {
			team = new AA_Team(teamName, players);
		}

		for (Player p: players) {
			if (!team.players.contains(p)) {
				team.players.add(p);
			}
		}
		teamChallenges.put(miniGame, challengingTeamsForThisMiniGame);
		return newChallengeForThisMiniGame;
	}

	public static Map<String, AA_Team> getChallengingTeamsForThisMiniGame(final AA_MiniGame miniGame) {
		return teamChallenges.get(miniGame);
	}


	public String getTeamName() {
		return teamName;
	}


	public ArrayList<Player> getPlayers() {
		return players;
	}


	public static boolean teamChallengeActive(final AA_MiniGame miniGame) {
		return teamChallenges.get(miniGame)!=null;
	}


	public static ArrayList<Player> getAllChallengersFor(final AA_MiniGame miniGame) {
		Map<String, AA_Team> challengingTeamsForThisMiniGame = teamChallenges.get(miniGame);
		ArrayList<Player> allChallengers = new ArrayList<Player>();

		if (challengingTeamsForThisMiniGame==null) return allChallengers;
		for (AA_Team team: challengingTeamsForThisMiniGame.values()) {
			allChallengers.addAll(team.players);
		}
		return allChallengers;
	}


	public static void removeChallengeFor(final AA_MiniGame miniGame) {
		teamChallenges.remove(miniGame);
	}


}
