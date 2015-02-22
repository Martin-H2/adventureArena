package adventureArena;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class AA_MiniGame {


	private final int id;
	private Vector southEastMax = null;
	private Vector northWestMin = null;
	private String name = "newMiniGame";
	private boolean pvpDamage = false;
	private ScoreMode scoreMode = ScoreMode.ScoreByCommand;
	private final Map<String, List<Vector>> spawnPoints = new HashMap<String, List<Vector>>();
	//private final List<ItemStack> spawnEquip = new ArrayList<ItemStack>();
	private final List<AA_SpawnEquip> spawnEquipDefinitions = new ArrayList<AA_SpawnEquip>();
	private final List<String> allowedEditors = new ArrayList<String>();
	//TODO highscore

	private boolean needsPersisting = true;
	private boolean needsEnvironmentBackup = true;

	private boolean lockedByEditSession = false;
	private boolean inProgress = false;
	private World world = null;



	//################## OBJECT LIFECYLCE ######################

	public AA_MiniGame(final int id, final World w) {
		this.id = id;
		world = w;
		if (w==null) {
			AA_MessageSystem.consoleError("world is NULL for " + this);
		}
	}

	public void persist() {
		set(AA_ConfigPaths.needsEnvironmentBackup, needsEnvironmentBackup);
		set(AA_ConfigPaths.lockedByEditSession, lockedByEditSession);
		set(AA_ConfigPaths.inProgress, inProgress);
		set(AA_ConfigPaths.worldName, world.getName());
		set(AA_ConfigPaths.southEastMax, southEastMax);
		set(AA_ConfigPaths.northWestMin, northWestMin);
		set(AA_ConfigPaths.name, name);
		set(AA_ConfigPaths.pvpDamage, pvpDamage);
		set(AA_ConfigPaths.scoreMode, scoreMode.toString());
		//set(AA_ConfigPaths.spawnPoints, spawnPoints);
		for (String teamName: spawnPoints.keySet()) {
			set(AA_ConfigPaths.spawnPoints + "." + teamName, spawnPoints.get(teamName));
		}
		set(AA_ConfigPaths.spawnEquip, spawnEquipDefinitions);
		set(AA_ConfigPaths.allowedEditors, allowedEditors);
		AA_MiniGameControl.saveMiniGameConfig();
		needsPersisting = false;
	}
	private void set(final String miniGameSubPath, final Object obj) {
		AA_MiniGameControl.getMiniGameConfig().set(id + "." + miniGameSubPath, obj);
		needsPersisting = true;
	}

	public static AA_MiniGame loadFromConfig(final int id) {
		FileConfiguration cfg = AA_MiniGameControl.getMiniGameConfig();
		String miniGameRootPath = String.valueOf(id);
		if (!cfg.contains(miniGameRootPath)) return null;
		AA_MiniGame mg = new AA_MiniGame(id, Bukkit.getWorld(cfg.getString(miniGameRootPath + "." + AA_ConfigPaths.worldName , "3gd")));
		mg.needsEnvironmentBackup = cfg.getBoolean(miniGameRootPath + "." + AA_ConfigPaths.needsEnvironmentBackup, true);
		mg.lockedByEditSession = cfg.getBoolean(miniGameRootPath + "." + AA_ConfigPaths.lockedByEditSession, false);
		mg.inProgress = cfg.getBoolean(miniGameRootPath + "." + AA_ConfigPaths.inProgress, false);
		mg.southEastMax = cfg.getVector(miniGameRootPath + "." + AA_ConfigPaths.southEastMax , null);
		mg.northWestMin = cfg.getVector(miniGameRootPath + "." + AA_ConfigPaths.northWestMin , null);
		mg.name = cfg.getString(miniGameRootPath + "." + AA_ConfigPaths.name , "newMiniGame");
		mg.pvpDamage = cfg.getBoolean(miniGameRootPath + "." + AA_ConfigPaths.pvpDamage , false);
		mg.scoreMode = ScoreMode.valueOf(cfg.getString(miniGameRootPath + "." + AA_ConfigPaths.scoreMode , ScoreMode.ScoreByCommand.toString()));
		//fillSpawnPointMap(miniGameRootPath + "." + AA_ConfigPaths.spawnPoints, mg);
		ConfigurationSection spawns = cfg.getConfigurationSection(miniGameRootPath + "." + AA_ConfigPaths.spawnPoints);
		if (spawns!=null) {
			for (String teamPath: spawns.getKeys(false)) {
				List<Vector> ts = new ArrayList<Vector>();
				fillCollection(miniGameRootPath + "." + AA_ConfigPaths.spawnPoints + "." + teamPath, ts);
				mg.spawnPoints.put(teamPath, ts);
			}
		}
		fillCollection(miniGameRootPath + "." + AA_ConfigPaths.spawnEquip, mg.getSpawnEquipDefinitions());
		fillCollection(miniGameRootPath + "." + AA_ConfigPaths.allowedEditors, mg.allowedEditors);
		mg.needsPersisting = false;
		return mg;
	}

	private static <T> void fillCollection(final String path, final Collection<T> collection) {
		List<?> cfgCollection = AA_MiniGameControl.getMiniGameConfig().getList(path);
		if (cfgCollection instanceof Collection<?>) {
			for (Object cfgElement: (Collection<?>)cfgCollection) {
				try {
					@SuppressWarnings("unchecked")
					T element = (T) cfgElement;
					collection.add(element);
				} catch (ClassCastException e) {
					e.printStackTrace();
				}
			}
		}
	}




	//################## SETTER GETTER ######################

	public void setSouthEastMax(final Location southEastMax) {
		this.southEastMax = southEastMax!=null ? southEastMax.toVector() : null;
		needsPersisting = true;
	}
	public void setNorthWestMin(final Location northWestMin) {
		this.northWestMin = northWestMin!=null ? northWestMin.toVector() : null;
		needsPersisting = true;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
		needsPersisting = true;
	}

	public boolean isPvpDamage() {
		return pvpDamage;
	}

	public void setPvpDamage(final boolean pvpDamage) {
		this.pvpDamage = pvpDamage;
		needsPersisting = true;
	}

	public ScoreMode getScoreMode() {
		return scoreMode;
	}

	public void setScoreMode(final ScoreMode scoreMode) {
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
			AA_MessageSystem.consoleError("failed to remove spawnpoint " + loc.toString() + " from " + this + ", team not found: " + teamName);
			return;
		}
		spawnPoints.get(teamName).remove(loc);
		needsPersisting = true;
	}

	public List<AA_SpawnEquip> getSpawnEquipDefinitions() {
		return spawnEquipDefinitions;
	}
	public void addSpawnEquipDefinition(final AA_SpawnEquip item) {
		spawnEquipDefinitions.add(item);
		needsPersisting = true;
	}
	public void removeSpawnEquipBySignPos(final Vector spawnEquipSignPos) {
		for (Iterator<AA_SpawnEquip> iter = spawnEquipDefinitions.iterator(); iter.hasNext();) {
			AA_SpawnEquip se = iter.next();
			if (spawnEquipSignPos.equals(se.getSignPos())) {
				iter.remove();
				needsPersisting = true;
			}
		}
	}
	public ItemStack[] getSpawnEquip() {
		ItemStack[] items = new ItemStack[getSpawnEquipDefinitions().size()];
		for (int i = 0; i<items.length; i++) {
			items[i] = getSpawnEquipDefinitions().get(i).toItemStack();
		}
		return items;
	}

	@SuppressWarnings("deprecation")
	public List<Player> getAllowedEditors() {
		ArrayList<Player> allowedEditors = new ArrayList<Player>();
		for (String name: this.allowedEditors) {
			Player p = Bukkit.getPlayer(name);
			if (p!=null) {
				allowedEditors.add(p);
			}
		}
		return allowedEditors;
	}
	public void addAllowedEditor(final String name) {
		allowedEditors.add(name);
		needsPersisting = true;
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




	//################## DIRTY & LOCK FLAGS ######################

	public boolean needsPersisting() {
		return needsPersisting;
	}
	public boolean needsEnvironmentBackup() {
		return needsEnvironmentBackup;
	}
	public boolean isLockedByEditSession() {
		return lockedByEditSession;
	}
	public boolean isInProgress() {
		return inProgress;
	}



	public void setLockedByEditSession(final boolean locked) {
		lockedByEditSession = locked;
		if (locked) {
			needsEnvironmentBackup = true;
		}
		AA_MessageSystem.consoleWarn("lockedByEditSession: " + lockedByEditSession);

		persist();
	}
	public void setInProgress(final boolean inProgress) {
		this.inProgress = inProgress;
		persist();
	}



	//################## Environment ######################

	public boolean doEnvironmentBackup() {
		if (AA_TerrainHelper.saveMiniGameToSchematic(northWestMin, southEastMax, id, world)) {
			needsEnvironmentBackup = false;
			persist();
			return true;
		}
		return false;
	}

	public boolean restoreEnvironmentBackup() {
		if (AA_TerrainHelper.loadMinigameFromSchematic(id, world)) {
			needsEnvironmentBackup = false;
			persist();
			return true;
		}
		return false;
	}

	public void wipeEntities() {
		for(Entity e : world.getEntities()){
			if (isInsideBounds(e.getLocation()) && e.getType()!=EntityType.PLAYER){
				//AA_MessageSystem.consoleWarn("WIPE: " + e.getName());
				e.remove();
			}
		}
	}



	//################## UTIL ######################

	boolean isInsideBounds(final Location loc) {
		if(southEastMax==null || northWestMin==null) return false;
		return southEastMax.getX() >= loc.getX() && northWestMin.getX() <= loc.getX()
				&& southEastMax.getY() >= loc.getY() && northWestMin.getY() <= loc.getY()
				&& southEastMax.getZ() >= loc.getZ() && northWestMin.getZ() <= loc.getZ();
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
		return "[id=" + id + ", southEastMax=" + southEastMax
				+ ", northWestMin=" + northWestMin + ", name=" + name
				+ ", pvpDamage=" + pvpDamage + ", scoreMode=" + scoreMode
				+ ", #spawnPoints:" + getNumberOfSpawnPoints() + ", #spawnEquip:" + spawnEquipDefinitions.size()
				+ ", allowedEditors=" + allowedEditors.toString() + "]";
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof AA_MiniGame)) return false;
		return id == ((AA_MiniGame) obj).getID();
	}





}
