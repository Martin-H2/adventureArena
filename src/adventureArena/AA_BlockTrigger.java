package adventureArena;

import java.util.*;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;


public class AA_BlockTrigger implements ConfigurationSerializable {

	private final Vector		signPosition;
	private final Vector		attachedBlockPosition;
	private final boolean		isSpawnTrigger;
	private final double		radius;

	//#### OPTIONAL ####
	private EntityType			entityType			= EntityType.UNKNOWN;
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
	public AA_BlockTrigger(final Vector signPosition, final Vector attachedBlockPosition, final boolean isSpawnTrigger, final double radius, final EntityType entityType) {
		super();
		this.signPosition = signPosition;
		this.attachedBlockPosition = attachedBlockPosition;
		this.isSpawnTrigger = isSpawnTrigger;
		this.radius = radius;
		this.entityType = entityType;
	}

	public AA_BlockTrigger(final Vector signPosition, final Vector attachedBlockPosition, final boolean isSpawnTrigger, final double radius, final int newScore) {
		super();
		this.signPosition = signPosition;
		this.attachedBlockPosition = attachedBlockPosition;
		this.isSpawnTrigger = isSpawnTrigger;
		this.radius = radius;
		this.newScore = newScore;
	}

	public AA_BlockTrigger(final Map<String, Object> serializedForm) {
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
		serializedForm.put("isSpawnTrigger", isSpawnTrigger);
		serializedForm.put("radius", radius);
		serializedForm.put("entityType", entityType.toString());
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
	public void checkRangeAndTrigger(final Player p, final AA_MiniGame mg) {
		if (isInside(p.getLocation().toVector())) {
			if (newScore >= 0) {
				triggerScore(p);
			}
			else if (!hasGlobalCd) {
				triggerSpawn(p.getWorld(), mg);
			}

		}
	}

	public void checkAndTrigger(final World w, final AA_MiniGame mg) {
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

	private void triggerSpawn(final World w, final AA_MiniGame mg) {
		hasGlobalCd = true;
		final Location airBlockAboveAttachedBlockTelePos = AA_TerrainHelper.getAirBlockAboveGroundTelePos(attachedBlockPosition.toLocation(w), true);

		AA_SelfCancelingTask spawnTask = new AA_SelfCancelingTask() {

			@Override
			public void tick() {
				final Entity entity = w.spawnEntity(airBlockAboveAttachedBlockTelePos, entityType);
				if (entity instanceof Creature && hp > 0) {
					Creature creature = (Creature) entity;
					creature.setMaxHealth(hp);
					creature.setHealth(hp);
					creature.setTarget(mg.getRandomPlayer());
					if (explodeOnDeath) {
						EntityEquipment ee = creature.getEquipment();
						ee.setHelmet(new ItemStack(Material.PUMPKIN));
					}
				}
				if (lifeTime > 0) {
					entity.setCustomNameVisible(true);
					AA_SelfCancelingTask explosionTimerTask = new AA_SelfCancelingTask() {

						@Override
						public void tick() {
							if (explodeOnDeath) {
								ChatColor cc = getNumberOfExecutionsLeft() < 3 ? ChatColor.RED : ChatColor.BLUE;
								entity.setCustomName(cc.toString() + getNumberOfExecutionsLeft());
								//AA_MessageSystem.consoleDebug("EXPLODE: " + cc.toString() + getNumberOfExecutionsLeft());
							}
						}

						@Override
						public void lastTick() {
							if (entity.isValid()) {
								if (explodeOnDeath) {
									float power = 4;
									if (entity instanceof LivingEntity) {
										LivingEntity monster = (LivingEntity) entity;
										power *= (float) (monster.getHealth() / monster.getMaxHealth());
									}
									entity.setCustomName("Exploding " + entity.getName());
									entity.getWorld().createExplosion(entity.getLocation(), power);
								}
								entity.remove();
							}
						}
					};

					explosionTimerTask.schedule(0, 1, (int) Math.round(lifeTime) + 1);
					runningTasks.add(explosionTimerTask.getTaskId());
				}
			}

			@Override
			public void lastTick() {
				tick();
			}
		};


		if (delay == 0 && count == 1) {
			spawnTask.tick();
		}
		else {
			int finalCount = count * (isPerPlayerCount ? mg.getNumberOfPlayersRemaining() : 1);
			spawnTask.schedule(delay + rnd.nextDouble() * delayRndRange, 1, finalCount);
			runningTasks.add(spawnTask.getTaskId());
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
		if (!(obj instanceof AA_BlockTrigger)) return false;
		return signPosition.equals(((AA_BlockTrigger) obj).signPosition);
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
