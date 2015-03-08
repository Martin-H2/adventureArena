package adventureArena;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;


public class AA_MonsterTrigger implements ConfigurationSerializable {

	private final Vector signPosition;
	private final Vector attachedBlockPosition;
	private final boolean isSpawnTrigger;
	private final double radius;

	//#### OPTIONAL ####
	private EntityType entityType = EntityType.UNKNOWN;
	private int newScore = -1;
	private double delay = -1;
	private double delayRndRange = 0;
	private double hp = -1;
	private double lifeTime = -1;
	private boolean explodeOnDeath = false;

	//#### NON PERSIST BUFFERS ####
	private static Random rnd = new Random();
	private boolean hasGlobalCd = false;
	private final List<Integer> runningTasks = new ArrayList<Integer>();




	//#### OBJ LC ####
	public AA_MonsterTrigger(final Vector signPosition, final Vector attachedBlockPosition, final boolean isSpawnTrigger, final double radius, final EntityType entityType) {
		super();
		this.signPosition = signPosition;
		this.attachedBlockPosition = attachedBlockPosition;
		this.isSpawnTrigger = isSpawnTrigger;
		this.radius = radius;
		this.entityType = entityType;
	}
	public AA_MonsterTrigger(final Vector signPosition, final Vector attachedBlockPosition, final boolean isSpawnTrigger, final double radius, final int newScore) {
		super();
		this.signPosition = signPosition;
		this.attachedBlockPosition = attachedBlockPosition;
		this.isSpawnTrigger = isSpawnTrigger;
		this.radius = radius;
		this.newScore = newScore;
	}
	public AA_MonsterTrigger(final Map<String, Object> serializedForm) {
		signPosition = (Vector) serializedForm.get("signPosition");
		attachedBlockPosition = (Vector) serializedForm.get("attachedBlockPosition");
		isSpawnTrigger = (boolean) serializedForm.get("isSpawnTrigger");
		radius = (double) serializedForm.get("radius");
		entityType = EntityType.valueOf((String) serializedForm.get("entityType"));
		delay = (double) serializedForm.get("delay");
		delayRndRange = (double) serializedForm.get("delayRndRange");
		hp = (double) serializedForm.get("hp");
		lifeTime = (double) serializedForm.get("lifeTime");
		explodeOnDeath = (boolean) serializedForm.get("explodeOnDeath");
		newScore = (int) serializedForm.get("newScore");
	}
	@Override
	public Map<String, Object> serialize() {
		Map<String, Object> serializedForm = new HashMap<String, Object>();
		serializedForm.put("signPosition", signPosition);
		serializedForm.put("attachedBlockPosition", attachedBlockPosition);
		serializedForm.put("isSpawnTrigger", isSpawnTrigger);
		serializedForm.put("radius", radius);
		serializedForm.put("entityType", entityType.toString());
		serializedForm.put("delay", delay);
		serializedForm.put("delayRndRange", delayRndRange);
		serializedForm.put("hp", hp);
		serializedForm.put("lifeTime", lifeTime);
		serializedForm.put("explodeOnDeath", explodeOnDeath);
		serializedForm.put("newScore", newScore);
		return serializedForm;
	}






	//#### API ####
	public void checkRangeAndTrigger(final Player p) {
		if (isInside(p.getLocation().toVector())) {
			if (newScore>=0) {
				triggerScore(p);
			}
			else if (!hasGlobalCd) {
				triggerSpawn(p.getWorld());
			}

		}
	}
	public void checkAndTrigger(final World w) {
		if (!hasGlobalCd) {
			triggerSpawn(w);
		}
	}
	public void reset() {
		hasGlobalCd = false;
		for(int id: runningTasks) {
			AdventureArena.cancelTask(id);
		}
		runningTasks.clear();
	}






	//#### PRIVATE API ####
	private void triggerScore(final Player p) {
		if (p!=null) {
			AA_ScoreManager.onSetScoreCmd(p, newScore);
		}
	}
	private void triggerSpawn(final World w) {
		hasGlobalCd = true;
		Runnable spawnProcess = new Runnable() {
			@Override
			public void run() {
				Location airBlockAboveAttachedBlockTelePos = AA_TerrainHelper.getAirBlockAboveGroundTelePos(attachedBlockPosition.toLocation(w), true);
				final Entity entity = w.spawnEntity(airBlockAboveAttachedBlockTelePos, entityType);
				if (entity instanceof LivingEntity && hp>0) {
					LivingEntity monster = (LivingEntity) entity;
					monster.setMaxHealth(hp);
					monster.setHealth(hp);
				}
				if (lifeTime>0) {
					runningTasks.add(AdventureArena.executeDelayed(lifeTime, new Runnable() {
						@Override
						public void run() {
							if(entity.isValid()) {
								if (explodeOnDeath) {
									float power = 4;
									if (entity instanceof LivingEntity) {
										LivingEntity monster = (LivingEntity) entity;
										power *= (float) (monster.getHealth() / monster.getMaxHealth());
									}
									entity.getWorld().createExplosion(entity.getLocation(), power);
								}
								entity.remove();
							}
						}
					}));
				}
			}
		};
		if (delay>0) {
			double rndDelay = delay + rnd.nextDouble()*delayRndRange;
			runningTasks.add(AdventureArena.executeDelayed(rndDelay, spawnProcess));
		} else {
			spawnProcess.run();
		}
	}
	private boolean isInside(final Vector v) {
		return attachedBlockPosition.distanceSquared(v) <= radius*radius;
	}







	//#### SET & GET ####
	public boolean isOnGlobalCd() {
		return hasGlobalCd;
	}
	public void setDelay(final double delay) {
		this.delay = delay;
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
	public boolean isSpawnTrigger() {
		return isSpawnTrigger;
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



	//#### OVERRIDE ####
	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof AA_MonsterTrigger)) return false;
		return signPosition.equals(((AA_MonsterTrigger)obj).signPosition);
	}
	@Override
	public String toString() {
		return "AA_MonsterTrigger [isSpawnTrigger=" + isSpawnTrigger
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
