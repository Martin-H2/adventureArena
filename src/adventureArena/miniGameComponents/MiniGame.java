package adventureArena.miniGameComponents;

import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;
import adventureArena.ConfigAccess;
import adventureArena.PluginManagement;
import adventureArena.TerrainHelper;
import adventureArena.control.MiniGameLoading;
import adventureArena.control.PlayerControl;
import adventureArena.control.TeamManager;
import adventureArena.enums.ConfigPaths;
import adventureArena.enums.HighScoreMode;
import adventureArena.enums.ScoreType;
import adventureArena.messages.MessageSystem;
import adventureArena.score.HighScoreManager;

public class MiniGame {


	private final int						id;
	private Vector							southEastMax			= null;
	private Vector							northWestMin			= null;
	private String							name					= "newMiniGame";
	private String							king					= null;
	private boolean							pvpDamage				= false;
	private HighScoreMode					scoreMode				= HighScoreMode.ScoreByCommand;
	private final Map<String, List<Vector>>	spawnPoints				= new HashMap<String, List<Vector>>();
	//private final List<ItemStack> spawnEquip = new ArrayList<ItemStack>();
	private final List<SpawnEquip>			spawnEquipDefinitions	= new ArrayList<SpawnEquip>();
	private final List<String>				allowedEditors			= new ArrayList<String>();
	private final List<Vector>				highScoreSignLocations	= new ArrayList<Vector>();
	private final List<MiniGameTrigger>		rangedTriggers			= new ArrayList<MiniGameTrigger>();
	private final List<MiniGameTrigger>		startTriggers			= new ArrayList<MiniGameTrigger>();

	private boolean							needsPersisting			= true;
	private boolean							needsEnvironmentBackup	= true;

	private boolean							isEditSessionActive		= false;
	private boolean							isPlaySessionActive				= false;
	private World							world					= null;

	//play session.
	//private final HashMap<String, List<Player>> teamPlayerMappings = new HashMap<String, List<Player>>();
	private static Random					rnd						= new Random();
	private final HashMap<String, Integer>	initialJoiners			= new HashMap<String, Integer>();
	private boolean							isOver					= false;
	List<Player>							activePlayers			= new ArrayList<Player>();
	private Location						spectatorRespawnPoint	= null;
	private Vector							playableAreaMidpoint;



	//################## OBJECT LIFECYLCE ######################

	public MiniGame(final int id, final World w) {
		this.id = id;
		world = w;
		if (w == null) {
			MessageSystem.consoleError("world is NULL for " + this);
		}
	}

	public void persist() {
		set(ConfigPaths.needsEnvironmentBackup, needsEnvironmentBackup);
		set(ConfigPaths.lockedByEditSession, isEditSessionActive);
		set(ConfigPaths.inProgress, isPlaySessionActive);
		set(ConfigPaths.worldName, world.getName());
		set(ConfigPaths.southEastMax, getSouthEastMax());
		set(ConfigPaths.northWestMin, getNorthWestMin());
		set(ConfigPaths.name, name);
		set(ConfigPaths.king, king);
		set(ConfigPaths.pvpDamage, pvpDamage);
		set(ConfigPaths.scoreMode, scoreMode.toString());
		//set(AA_ConfigPaths.spawnPoints, spawnPoints);
		for (String teamName: spawnPoints.keySet()) {
			set(ConfigPaths.spawnPoints + "." + teamName, spawnPoints.get(teamName));
		}
		set(ConfigPaths.spawnEquip, spawnEquipDefinitions);
		set(ConfigPaths.rangedMonsterTriggers, rangedTriggers);
		set(ConfigPaths.startMonsterTriggers, startTriggers);
		set(ConfigPaths.allowedEditors, allowedEditors);
		set(ConfigPaths.highScoreSignLocations, highScoreSignLocations);
		ConfigAccess.saveMiniGameConfig();
		needsPersisting = false;
	}

	private void set(final String miniGameSubPath, final Object obj) {
		ConfigAccess.getMiniGameConfig().set(id + "." + miniGameSubPath, obj);
		needsPersisting = true;
	}

	public static MiniGame loadFromConfig(final int id) {
		FileConfiguration cfg = ConfigAccess.getMiniGameConfig();
		String miniGameRootPath = String.valueOf(id);
		if (!cfg.contains(miniGameRootPath)) return null;
		MiniGame mg = new MiniGame(id, Bukkit.getWorld(cfg.getString(miniGameRootPath + "." + ConfigPaths.worldName, "3gd")));
		mg.needsEnvironmentBackup = cfg.getBoolean(miniGameRootPath + "." + ConfigPaths.needsEnvironmentBackup, true);
		mg.isEditSessionActive = cfg.getBoolean(miniGameRootPath + "." + ConfigPaths.lockedByEditSession, false);
		mg.isPlaySessionActive = cfg.getBoolean(miniGameRootPath + "." + ConfigPaths.inProgress, false);
		mg.setSouthEastMax(cfg.getVector(miniGameRootPath + "." + ConfigPaths.southEastMax, null));
		mg.setNorthWestMin(cfg.getVector(miniGameRootPath + "." + ConfigPaths.northWestMin, null));
		mg.name = cfg.getString(miniGameRootPath + "." + ConfigPaths.name, "newMiniGame");
		mg.king = cfg.getString(miniGameRootPath + "." + ConfigPaths.king, null);
		mg.pvpDamage = cfg.getBoolean(miniGameRootPath + "." + ConfigPaths.pvpDamage, false);
		mg.scoreMode = HighScoreMode.valueOf(cfg.getString(miniGameRootPath + "." + ConfigPaths.scoreMode, HighScoreMode.ScoreByCommand.toString()));
		//fillSpawnPointMap(miniGameRootPath + "." + AA_ConfigPaths.spawnPoints, mg);
		ConfigurationSection spawns = cfg.getConfigurationSection(miniGameRootPath + "." + ConfigPaths.spawnPoints);
		if (spawns != null) {
			for (String teamPath: spawns.getKeys(false)) {
				List<Vector> ts = new ArrayList<Vector>();
				fillCollection(miniGameRootPath + "." + ConfigPaths.spawnPoints + "." + teamPath, ts);
				mg.spawnPoints.put(teamPath, ts);
			}
		}
		fillCollection(miniGameRootPath + "." + ConfigPaths.spawnEquip, mg.getSpawnEquipDefinitions());
		fillCollection(miniGameRootPath + "." + ConfigPaths.rangedMonsterTriggers, mg.rangedTriggers);
		fillCollection(miniGameRootPath + "." + ConfigPaths.startMonsterTriggers, mg.startTriggers);
		fillCollection(miniGameRootPath + "." + ConfigPaths.allowedEditors, mg.allowedEditors);
		fillCollection(miniGameRootPath + "." + ConfigPaths.highScoreSignLocations, mg.highScoreSignLocations);
		mg.needsPersisting = false;
		return mg;
	}

	private static <T> void fillCollection(final String path, final Collection<T> collection) {
		List<?> cfgCollection = ConfigAccess.getMiniGameConfig().getList(path);
		if (cfgCollection instanceof Collection<?>) {
			for (Object cfgElement: (Collection<?>) cfgCollection) {
				try {
					@SuppressWarnings ("unchecked") T element = (T) cfgElement;
					collection.add(element);
				}
				catch (ClassCastException e) {
					e.printStackTrace();
				}
			}
		}
	}



	//################## SETTER GETTER ######################

	public void setSouthEastMax(final Vector southEastMax) {
		this.southEastMax = southEastMax;
		calcSpectatorRespawnPoint();
		needsPersisting = true;
	}

	public void setNorthWestMin(final Vector northWestMin) {
		this.northWestMin = northWestMin;
		calcSpectatorRespawnPoint();
		needsPersisting = true;
	}

	private void calcSpectatorRespawnPoint() {
		if (getSouthEastMax() != null && getNorthWestMin() != null) {
			spectatorRespawnPoint = new Location(world,
					(getSouthEastMax().getX() + getNorthWestMin().getX()) / 2,
					getSouthEastMax().getY() - 1,
					(getSouthEastMax().getZ() + getNorthWestMin().getZ()) / 2
				);
		}
	}

	public Location getSpectatorRespawnPoint() {
		if (spectatorRespawnPoint == null) {
			calcSpectatorRespawnPoint();
		}
		if (spectatorRespawnPoint == null) {
			MessageSystem.consoleError("can't calc spectatorRespawnPoint for '" + name + "', id: " + id);
		}
		return spectatorRespawnPoint;
	}

	public Vector getNorthWestMin() {
		return northWestMin;
	}

	public Vector getSouthEastMax() {
		return southEastMax;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
		needsPersisting = true;
	}

	public String getKing() {
		return king;
	}

	public void setKing(final String k) {
		king = k;
		persist();
	}

	public boolean isPvp() {
		return pvpDamage;
	}

	public void setPvpDamage(final boolean pvpDamage) {
		this.pvpDamage = pvpDamage;
		needsPersisting = true;
	}

	public HighScoreMode getScoreMode() {
		return scoreMode;
	}

	public ScoreType getScoreTypeForHighscore() {
		ScoreType st = ScoreType.CMD_RATING;
		if (getScoreMode() == HighScoreMode.KillsPerDeath) {
			if (isPvp()) {
				st = ScoreType.PVP_RATING;
			}
			else {
				st = ScoreType.PVE_RATING;
			}
		}
		else if (getScoreMode() == HighScoreMode.LastManStanding) {
			st = ScoreType.LTS_RATING;
		}
		return st;
	}

	public void setScoreMode(final HighScoreMode scoreMode) {
		this.scoreMode = scoreMode;
		needsPersisting = true;
	}

	public List<Vector> getSpawnPoints(final String team) {
		return spawnPoints.get(team);
	}

	public void addSpawnPoint(final String teamName, final Location loc) {
		addSpawnPoint(teamName, loc.toVector());
	}

	public void addSpawnPoint(final String teamName, final Vector loc) {
		if (!spawnPoints.containsKey(teamName)) {
			spawnPoints.put(teamName, new ArrayList<Vector>());
		}
		spawnPoints.get(teamName).add(loc);
		needsPersisting = true;
	}

	public void removeSpawnPoint(final String teamName, final Location loc) {
		removeSpawnPoint(teamName, loc.toVector());
	}

	public void removeSpawnPoint(final String teamName, final Vector loc) {
		if (!spawnPoints.containsKey(teamName)) {
			MessageSystem.consoleError("failed to remove spawnpoint " + loc.toString() + " from " + this + ", team not found: " + teamName);
			return;
		}
		spawnPoints.get(teamName).remove(loc);
		needsPersisting = true;
	}

	public void addTrigger(final MiniGameTrigger trigger) {
		if (trigger.isStartTrigger()) {
			startTriggers.add(trigger);
		}
		else {
			rangedTriggers.add(trigger);
		}
		needsPersisting = true;
	}

	public void removeTriggerBySignPos(final Vector monsterTriggerSignPos) {
		for (Iterator<MiniGameTrigger> iter = startTriggers.iterator(); iter.hasNext();) {
			MiniGameTrigger mt = iter.next();
			if (monsterTriggerSignPos.equals(mt.getSignPos())) {
				iter.remove();
				needsPersisting = true;
			}
		}
		for (Iterator<MiniGameTrigger> iter = rangedTriggers.iterator(); iter.hasNext();) {
			MiniGameTrigger mt = iter.next();
			if (monsterTriggerSignPos.equals(mt.getSignPos())) {
				iter.remove();
				needsPersisting = true;
			}
		}
	}

	public List<MiniGameTrigger> getRangedTriggers() {
		return rangedTriggers;
	}

	public List<MiniGameTrigger> getStartTriggers() {
		return startTriggers;
	}

	public void runStartTriggers() {
		for (MiniGameTrigger mt: getStartTriggers()) {
			mt.checkAndTrigger(getWorld(), this);
		}
	}

	public List<SpawnEquip> getSpawnEquipDefinitions() {
		return spawnEquipDefinitions;
	}

	public void addSpawnEquipDefinition(final SpawnEquip item) {
		spawnEquipDefinitions.add(item);
		needsPersisting = true;
	}

	public void removeSpawnEquipBySignPos(final Vector spawnEquipSignPos) {
		for (Iterator<SpawnEquip> iter = spawnEquipDefinitions.iterator(); iter.hasNext();) {
			SpawnEquip se = iter.next();
			if (spawnEquipSignPos.equals(se.getSignPos())) {
				iter.remove();
				needsPersisting = true;
			}
		}
	}

	public ItemStack[] getSpawnEquip() {
		ItemStack[] items = new ItemStack[getSpawnEquipDefinitions().size()];
		for (int i = 0; i < items.length; i++) {
			items[i] = getSpawnEquipDefinitions().get(i).toItemStack();
		}
		return items;
	}

	public List<Player> getAllowedEditors() {
		ArrayList<Player> allowedEditors = new ArrayList<Player>();
		for (String name: this.allowedEditors) {
			Player p = Bukkit.getPlayer(name);
			if (p != null) {
				allowedEditors.add(p);
			}
		}
		return allowedEditors;
	}

	public void addAllowedEditor(final String name) {
		if (!allowedEditors.contains(name)) {
			allowedEditors.add(name);
			needsPersisting = true;
		}
	}

	public void removeAllowedEditor(final String name) {
		allowedEditors.remove(name);
		needsPersisting = true;
	}

	public boolean isEditableByPlayer(final Player player) {
		return allowedEditors.contains(player.getName()) || player.isOp();
	}

	public int getID() {
		return id;
	}


	public void registerHighScoreSignLocation(final Location location) {
		if (!highScoreSignLocations.contains(location.toVector())) {
			highScoreSignLocations.add(location.toVector());
			needsPersisting = true;
		}
		final MiniGame mg = this;
		PluginManagement.executeDelayed(0.1, new Runnable() {

			@Override
			public void run() {
				HighScoreManager.updateHighScoreList(mg);
			}
		});
	}

	public void unRegisterHighScoreSignLocation(final Location location) {
		highScoreSignLocations.remove(location.toVector());
		needsPersisting = true;
	}

	public void unRegisterHighScoreSignLocation(final Vector v) {
		highScoreSignLocations.remove(v);
		needsPersisting = true;
	}

	public List<Vector> getHighScoreSignLocations() {
		return highScoreSignLocations;
	}

	public World getWorld() {
		return world;
	}

	//################## LOCKS & DIRTY-FLAGS ######################

	public boolean needsPersisting() {
		return needsPersisting;
	}

	public boolean needsEnvironmentBackup() {
		return needsEnvironmentBackup;
	}

	public boolean isEditSessionActive() {
		return isEditSessionActive;
	}

	public boolean isPlaySessionActive() {
		return isPlaySessionActive;
	}

	public boolean isAnySessionActive() {
		return isPlaySessionActive || isEditSessionActive;
	}



	public void setLockedByEditSession(final boolean locked) {
		isEditSessionActive = locked;
		if (locked) {
			needsEnvironmentBackup = true;
		}
		persist();
	}

	public void setInProgress(final boolean inProgress) {
		this.isPlaySessionActive = inProgress;
		persist();
	}



	//################## Environment ######################

	public boolean doEnvironmentBackup() {
		//AA_MessageSystem.sideNoteForGroup("Saving environment backup for '" + name + "' (id:" + id + ")", getPlayersInArea());
		if (TerrainHelper.saveMiniGameToSchematic(getNorthWestMin(), getSouthEastMax(), id, world)) {
			needsEnvironmentBackup = false;
			persist();
			return true;
		}
		else {
			MessageSystem.errorToGroup("...backup failed!", getAllowedEditors());
			return false;
		}

	}

	public boolean restoreEnvironmentBackup() {
		//AA_MessageSystem.sideNoteForGroup("Loading environment backup for '" + name + "' (id:" + id + ")", getPlayersInArea());
		if (TerrainHelper.loadMinigameFromSchematic(id, world)) {
			needsEnvironmentBackup = false;
			persist();
			return true;
		}
		else {
			MessageSystem.errorToGroup("...restore failed!", getAllowedEditors());
			return false;
		}
	}



	public void wipeEntities() {
		for (Entity e: world.getEntities()) {
			if (isInsideBounds(e.getLocation()) && !isProtectedEntity(e)) {
				e.remove();
			}
		}
	}

	boolean isProtectedEntity(Entity e) {
		return e instanceof Hanging || e instanceof Vehicle || e instanceof ArmorStand || e instanceof Player;
	}


	//################## UTIL ######################

	public boolean isInsideBounds(final Location loc) {
		if (getSouthEastMax() == null || getNorthWestMin() == null) return false;
		return getSouthEastMax().getX() + 1 >= loc.getX() && getNorthWestMin().getX() <= loc.getX() // +1 due to block coords floor function
			&& getSouthEastMax().getY() + 1 >= loc.getY() && getNorthWestMin().getY() <= loc.getY()
			&& getSouthEastMax().getZ() + 1 >= loc.getZ() && getNorthWestMin().getZ() <= loc.getZ();
	}

	public boolean isInsidePlayableBounds(final Location loc) {
		if (getSouthEastMax() == null || getNorthWestMin() == null) return false;
		return getSouthEastMax().getX() + 1 >= loc.getX() && getNorthWestMin().getX() <= loc.getX()
			&& getSouthEastMax().getY() + 1 - 3 >= loc.getY() && getNorthWestMin().getY() <= loc.getY()
			&& getSouthEastMax().getZ() + 1 >= loc.getZ() && getNorthWestMin().getZ() <= loc.getZ();
	}

	public int getNumberOfSpawnPoints() {
		int n = 0;
		for (String team: spawnPoints.keySet()) {
			n += spawnPoints.get(team).size();
		}
		return n;
	}

	@Override
	public String toString() {
		return "[id=" + id + ", southEastMax=" + getSouthEastMax()
			+ ", northWestMin=" + getNorthWestMin() + ", name=" + name
			+ ", spectatorRespawnPoint=" + getSpectatorRespawnPoint().toVector()
			+ ", pvpDamage=" + pvpDamage + ", scoreMode=" + scoreMode
			+ ", #spawnPoints:" + getNumberOfSpawnPoints() + ", #spawnEquip:" + spawnEquipDefinitions.size()
			+ ", allowedEditors=" + allowedEditors.toString()
			+ ", #highScoreSignLocations: " + highScoreSignLocations.size()
			+ ", #startTriggers: " + startTriggers.size()
			+ ", #distanceTriggers: " + rangedTriggers.size()
			+ ", needsEnvironmentBackup: " + needsEnvironmentBackup
			+ ", inProgress: " + isPlaySessionActive
			+ ", lockedByEditSession: " + isEditSessionActive
			+ ", isSoloPlayable: " + isSoloPlayable()
			+ ", numberOfPlayersRemaining: " + getNumberOfPlayersRemaining()
			+ ", initialJoiners: " + initialJoiners.toString()
			+ ", remainingTeams: " + getTeamsAsMap().toString()
			+ "]";
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof MiniGame)) return false;
		return id == ((MiniGame) obj).getID();
	}



	public int getInitialNumberOfPlayers() {
		int initialNumberOfPlayers = 0;
		for (int n: initialJoiners.values()) {
			initialNumberOfPlayers += n;
		}
		return initialNumberOfPlayers;
	}

	public int getInitialNumberOfTeams() {
		return initialJoiners.keySet().size();
	}


	@SuppressWarnings ("deprecation")
	public void addPlayer(final String teamName, final Player p) {
		activePlayers.add(p);
		Team t = TeamManager.getTeam(id + ":" + teamName, this);
		if (t.hasPlayer(p)) return;
		t.addPlayer(p);
		if (initialJoiners.containsKey(teamName)) {
			initialJoiners.put(teamName, initialJoiners.get(teamName) + 1);
		}
		else {
			initialJoiners.put(teamName, 1);
		}
	}

	@SuppressWarnings ("deprecation")
	public void removePlayer(final Player p) {
		activePlayers.remove(p);
		Team t = TeamManager.getTeam(p);
		if (t == null) return;
		t.removePlayer(p);
		if (t.getSize() == 0) {
			t.unregister();
		}
	}

	public void wipeSessionVariables() {
		setInProgress(false);
		setLockedByEditSession(false);
		initialJoiners.clear();
		activePlayers.clear();
		isOver = false;
		for (Team t: getTeams()) {
			MessageSystem.consoleWarn("cleaning up team: " + t.getName());
			t.unregister();
		}
		for (MiniGameTrigger mt: startTriggers) {
			mt.reset();
		}
		for (MiniGameTrigger mt: rangedTriggers) {
			mt.reset();
		}
	}


	public int getNumberOfPlayersRemaining() {
		//		int totalPlayers = 0;
		//		for (Team t: getTeams()) {
		//			totalPlayers += t.getSize();
		//		}
		//		return totalPlayers;
		return activePlayers.size();
	}

	public List<Player> getPlayersRemaining() {
		return activePlayers;
	}

	//	public int numberOfPlayersLeft(final Team t) {
	//		if (t==null) return 0;
	//		return t.getSize();
	//	}
	public int getNumberOfEnemyPlayersRemaining(Player pov) {
		if (!equals(MiniGameLoading.getMiniGameForPlayer(pov))) {
			MessageSystem.consoleError("getNumberOfEnemiesRemaining: " + pov.getName() + " is not inside " + getName());
			return 0;
		}
		if (!isPvp()) return 0;

		final Team poiTeam = TeamManager.getTeam(pov);
		int totalEnemies = 0;
		if (isTeamPlayModeActive()) {
			for (Team t: getTeams()) {
				if (!t.equals(poiTeam)) {
					totalEnemies += t.getSize();
				}
			}
		}
		else {
			totalEnemies = getNumberOfPlayersRemaining() - 1;
		}
		return totalEnemies;
	}


	public boolean isTeamPlayModeActive() {
		return getInitialNumberOfTeams() > 1;
	}

	public boolean isSoloPlayable() {
		return scoreMode == HighScoreMode.ScoreByCommand || scoreMode == HighScoreMode.KillsPerDeath && !pvpDamage;
	}

	public List<Team> getTeams() {
		String teamNameIdPrefix = String.valueOf(id) + ":";
		List<Team> miniGameTeams = new ArrayList<Team>();

		for (Team t: TeamManager.scoreBoard.getTeams()) {
			if (t.getName().startsWith(teamNameIdPrefix)) {
				miniGameTeams.add(t);
			}
		}
		return miniGameTeams;
	}

	public HashMap<String, Integer> getTeamsAsMap() {
		HashMap<String, Integer> teams = new HashMap<String, Integer>();

		for (Team t: getTeams()) {
			teams.put(t.getName(), t.getSize());
		}
		return teams;
	}

	public boolean isVictory() {
		boolean soloPlayable = isSoloPlayable();
		boolean teamPlayModeActive = isTeamPlayModeActive();
		int numberOfPlayersRemaining = getNumberOfPlayersRemaining();
		return numberOfPlayersRemaining == 0 || !soloPlayable && (!teamPlayModeActive && numberOfPlayersRemaining <= 1 || teamPlayModeActive && getTeams().size() <= 1);
	}



	public boolean isOver() {
		return isOver;
	}

	public void setOver() {
		isOver = true;
	}

	public Player getRandomPlayer() {
		return activePlayers.size() == 0 ? null : activePlayers.get(rnd.nextInt(activePlayers.size()));
	}

	public List<Player> getSpectators() {
		List<Player> spectators = new ArrayList<Player>();
		for (Player p: Bukkit.getOnlinePlayers()) {
			if (isInsideBounds(p.getLocation()) && PlayerControl.isWatchingMiniGames(p)) {
				spectators.add(p);
			}
		}
		return spectators;
	}

	/**
	 * @return everybody inside minigame AABB (playing, editing, watching players)
	 */
	public ArrayList<Player> getPlayersInArea() {
		ArrayList<Player> playersInArea = new ArrayList<Player>();
		for (Player p: Bukkit.getOnlinePlayers()) {
			if (isInsideBounds(p.getLocation())) {
				playersInArea.add(p);
			}
		}
		return playersInArea;
	}


	public void resetRoom() {
		name = "newMiniGame";
		king = null;
		pvpDamage = false;
		scoreMode = HighScoreMode.ScoreByCommand;
		spawnPoints.clear();
		spawnEquipDefinitions.clear();
		allowedEditors.clear();
		rangedTriggers.clear();
		startTriggers.clear();
		needsPersisting = true;
		needsEnvironmentBackup = true;
		isEditSessionActive = false;
		isPlaySessionActive = false;
		persist();
		TerrainHelper.resetMiniGameRoom(this);
		doEnvironmentBackup();
	}

	public Vector getPlayableAreaMidpoint() {
		if (playableAreaMidpoint == null) {
			playableAreaMidpoint = getSouthEastMax().getMidpoint(getNorthWestMin());
		}
		return playableAreaMidpoint;
	}

	public void checkRangedTriggersOn(Player player) {
		for (MiniGameTrigger mt: getRangedTriggers()) {
			mt.checkRangeAndTrigger(player, this);
		}
	}



}
