package adventureArena.miniGameComponents;

import java.util.*;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import adventureArena.AA_ScoreManager;
import adventureArena.AdventureArena;
import adventureArena.tasks.AbstractPeriodicTask;
import adventureArena.tasks.PeriodicEntityTask;
import adventureArena.tasks.TimedBlockTask;


// WARNING SERIALIZED OBJECT ! Don't rename or refactor path
public class MiniGameTrigger implements ConfigurationSerializable {

	private final Vector		signPosition;
	private final Vector		attachedBlockPosition;
	private final boolean		isStartTrigger;
	private final double		radius;

	//#### OPTIONAL ####
	private EntityType			entityType			= EntityType.UNKNOWN;
	private Material			blockType			= Material.AIR;
	private int					newScore			= -1;
	private double				delay				= 0;
	private double				delayRndRange		= 0;
	private double				hp					= -1;
	private double				lifeTime			= -1;
	private boolean				explodeOnDeath		= false;
	private int					count				= 1;
	private boolean				isPerPlayerCount	= false;

	//#### NON PERSIST BUFFERS ####
	private static Random		rnd					= new Random();
	private boolean				hasGlobalCd			= false;
	private final List<Integer>	runningTasks		= new ArrayList<Integer>();



	//#### OBJ LC ####
	public MiniGameTrigger(final Vector signPosition, final Vector attachedBlockPosition, final boolean isStartTrigger, final double radius, final EntityType entityType) {
		super();
		this.signPosition = signPosition;
		this.attachedBlockPosition = attachedBlockPosition;
		this.isStartTrigger = isStartTrigger;
		this.radius = radius;
		this.entityType = entityType;
	}

	public MiniGameTrigger(final Vector signPosition, final Vector attachedBlockPosition, final boolean isStartTrigger, final double radius, final Material blockType) {
		super();
		this.signPosition = signPosition;
		this.attachedBlockPosition = attachedBlockPosition;
		this.isStartTrigger = isStartTrigger;
		this.radius = radius;
		this.blockType = blockType;
	}

	public MiniGameTrigger(final Vector signPosition, final Vector attachedBlockPosition, final boolean isStartTrigger, final double radius, final int newScore) {
		super();
		this.signPosition = signPosition;
		this.attachedBlockPosition = attachedBlockPosition;
		this.isStartTrigger = isStartTrigger;
		this.radius = radius;
		this.newScore = newScore;
	}

	public MiniGameTrigger(final Map<String, Object> serializedForm) {
		signPosition = (Vector) serializedForm.get("signPosition");
		attachedBlockPosition = (Vector) serializedForm.get("attachedBlockPosition");
		isStartTrigger = (boolean) serializedForm.get("isSpawnTrigger");
		radius = (double) serializedForm.get("radius");
		entityType = EntityType.valueOf((String) serializedForm.get("entityType"));
		if (serializedForm.containsKey("blockType")) {
			blockType = Material.valueOf((String) serializedForm.get("blockType"));
		}
		delay = (double) serializedForm.get("delay");
		delayRndRange = (double) serializedForm.get("delayRndRange");
		hp = (double) serializedForm.get("hp");
		lifeTime = (double) serializedForm.get("lifeTime");
		explodeOnDeath = (boolean) serializedForm.get("explodeOnDeath");
		newScore = (int) serializedForm.get("newScore");
		if (serializedForm.containsKey("count")) {
			count = (int) serializedForm.get("count");
		}
		if (serializedForm.containsKey("isPerPlayerCount")) {
			isPerPlayerCount = (boolean) serializedForm.get("isPerPlayerCount");
		}
	}

	@Override
	public Map<String, Object> serialize() {
		Map<String, Object> serializedForm = new HashMap<String, Object>();
		serializedForm.put("signPosition", signPosition);
		serializedForm.put("attachedBlockPosition", attachedBlockPosition);
		serializedForm.put("isSpawnTrigger", isStartTrigger);
		serializedForm.put("radius", radius);
		serializedForm.put("entityType", entityType.toString());
		serializedForm.put("blockType", blockType.toString());
		serializedForm.put("delay", delay);
		serializedForm.put("delayRndRange", delayRndRange);
		serializedForm.put("hp", hp);
		serializedForm.put("lifeTime", lifeTime);
		serializedForm.put("explodeOnDeath", explodeOnDeath);
		serializedForm.put("newScore", newScore);
		serializedForm.put("count", count);
		serializedForm.put("isPerPlayerCount", isPerPlayerCount);
		return serializedForm;
	}



	//#### API ####
	public void checkRangeAndTrigger(final Player p, final MiniGame mg) {
		if (isInside(p.getLocation().toVector())) {
			if (newScore >= 0) {
				triggerScore(p);
			}
			else if (!hasGlobalCd) {
				triggerSpawn(p.getWorld(), mg);
			}

		}
	}

	public void checkAndTrigger(final World w, final MiniGame mg) {
		if (!hasGlobalCd) {
			triggerSpawn(w, mg);
		}
	}

	public void reset() {
		hasGlobalCd = false;
		for (int id: runningTasks) {
			AdventureArena.cancelTask(id);
		}
		runningTasks.clear();
	}



	//#### PRIVATE API ####
	private void triggerScore(final Player p) {
		if (p != null) {
			AA_ScoreManager.onSetScoreCmd(p, newScore);
		}
	}

	private void triggerSpawn(final World w, final MiniGame mg) {
		hasGlobalCd = true;

		if (blockType != Material.AIR) {
			// SPAWN BLOCKS
			Runnable spawnTask = new TimedBlockTask(w, attachedBlockPosition, blockType, hp, explodeOnDeath, lifeTime, mg, runningTasks, count);
			if (delay <= 0) {
				spawnTask.run();
			}
			else {
				runningTasks.add(AdventureArena.executeDelayed(delay, spawnTask));
			}
		}
		else {
			// SPAWN ENTITIES
			AbstractPeriodicTask spawnTask = new PeriodicEntityTask(w, attachedBlockPosition, entityType, hp, explodeOnDeath, lifeTime, mg, runningTasks);
			if (delay <= 0 && count == 1) {
				spawnTask.lastTick();
			}
			else {
				int finalCount = count * (isPerPlayerCount ? mg.getNumberOfPlayersRemaining() : 1);
				spawnTask.schedule(delay + rnd.nextDouble() * delayRndRange, 1.0, finalCount);
				runningTasks.add(spawnTask.getTaskId());
			}
		}



	}

	private boolean isInside(final Vector v) {
		return attachedBlockPosition.distanceSquared(v) <= (radius + 1) * (radius + 1);
	}



	//#### SET & GET ####
	public boolean isOnGlobalCd() {
		return hasGlobalCd;
	}

	public void setDelay(final double delay) {
		this.delay = Math.max(0, delay);
	}

	public void setDelayRndRange(final double delayRndRange) {
		this.delayRndRange = Math.abs(delayRndRange);
	}

	public void setHp(final double hp) {
		this.hp = hp;
	}

	public void setLifeTime(final double lifeTime) {
		this.lifeTime = lifeTime;
	}

	public boolean isStartTrigger() {
		return isStartTrigger;
	}

	public Vector getSignPos() {
		return signPosition;
	}

	public boolean isExplodeOnDeath() {
		return explodeOnDeath;
	}

	public void setExplodeOnDeath(final boolean explodeOnDeath) {
		this.explodeOnDeath = explodeOnDeath;
	}

	public void setCount(final int count) {
		this.count = count;
	}

	public boolean isPerPlayerCount() {
		return isPerPlayerCount;
	}

	public void setPerPlayerCount(final boolean isPerPlayerCount) {
		this.isPerPlayerCount = isPerPlayerCount;
	}


	//#### OVERRIDE ####
	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof MiniGameTrigger)) return false;
		return signPosition.equals(((MiniGameTrigger) obj).signPosition);
	}

	@Override
	public String toString() {
		return "MiniGameTrigger [isSpawnTrigger=" + isStartTrigger
			+ ", radius=" + radius + ", entityType=" + entityType
			+ ", delay=" + delay + ", delayRndRange=" + delayRndRange
			+ ", hp=" + hp + ", lifeTime=" + lifeTime + ", hasGlobalCd="
			+ hasGlobalCd + ", runningTasks=" + runningTasks + "]";
	}

	public int getNewScore() {
		return newScore;
	}

	public void setNewScore(final int newScore) {
		this.newScore = newScore;
	}


}
