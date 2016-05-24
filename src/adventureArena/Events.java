package adventureArena;

import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.*;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.projectiles.ProjectileSource;
import adventureArena.control.*;
import adventureArena.messages.MessageSystem;
import adventureArena.messages.OnScreenMessages;
import adventureArena.miniGameComponents.MiniGame;
import adventureArena.score.ScoreManager;

@SuppressWarnings ("deprecation")
public class Events implements Listener {

	static Map<UUID, Integer>		playerBlockPos	= new HashMap<UUID, Integer>();

	private static HashSet<Byte>	transparent		= new HashSet<Byte>();
	static {
		transparent.add((byte) Material.AIR.getId());
		transparent.add((byte) Material.LONG_GRASS.getId());
		transparent.add((byte) Material.WALL_SIGN.getId());
	}



	@EventHandler
	public void onPlayerMove(final PlayerMoveEvent e) {
		Integer simpleFlooredLocationHash = TerrainHelper.simpleFlooredLocationHash(e.getTo());
		if (!simpleFlooredLocationHash.equals(playerBlockPos.get(e.getPlayer().getUniqueId()))) {
			playerBlockPos.put(e.getPlayer().getUniqueId(), simpleFlooredLocationHash);
			onBlockEnter(e.getPlayer(), e.getTo().getBlock());
		}
	}

	private void onBlockEnter(final Player player, final Block block) {
		if (PlayerControl.isPlayingMiniGame(player)) {
			MiniGame mg = MiniGameLoading.getMiniGameForPlayer(player);
			mg.checkRangedTriggersOn(player);
		}
		else if (PlayerControl.isEditingMiniGame(player)) {
			MiniGame mg = MiniGameLoading.getMiniGameForPlayer(player);
			if (!mg.isInsidePlayableBounds(player.getLocation())) {
				MiniGameSessions.leaveCurrentSession(player, false);
			}
		}
	}

	@EventHandler
	public void onPlayerJoin(final PlayerJoinEvent event) {
		if (HubControl.isInMgHubAABB(event.getPlayer().getLocation())) {
			MiniGameSessions.kickIfInsideMiniGame(event.getPlayer());
			HubControl.leaveMiniGameHub(event.getPlayer(), null);
		}
	}

	@EventHandler
	public void onPlayerQuit(final PlayerQuitEvent e) {
		HubControl.kickFromMiniGameAndHub(e.getPlayer());
	}

	@EventHandler
	public void onPlayerKick(final PlayerKickEvent e) {
		HubControl.kickFromMiniGameAndHub(e.getPlayer());
	}


	@EventHandler
	public void onEntityDamageByEntity(final EntityDamageByEntityEvent event) {
		if (event.getEntity() instanceof Player) {
			Player victim = (Player) event.getEntity();
			if (PlayerControl.isPlayingMiniGame(victim)) {
				Player attacker = getAttackingPlayerOverProjectile(event.getDamager());
				if (attacker != null) {
					MiniGame miniGame = MiniGameLoading.getMiniGameForPlayer(victim);
					if (TeamManager.isAllied(victim, attacker)) {
						event.setCancelled(true);
					}
					else if (miniGame.isPvp()) {
						OnScreenMessages.sendPvpHitMessage(victim, attacker, event);
					}
					else {
						// e.g. Spleef
						if (event.getCause() == DamageCause.PROJECTILE) {
							event.setDamage(0); //allow pushback only for projectiles
						}
						else {
							event.setCancelled(true);
						}
					}
				}
			}
		}
	}

	@EventHandler
	public void onEntityDamage(final EntityDamageEvent e) {
		if (e.getEntity() instanceof Player) {
			Player victim = (Player) e.getEntity();
			if (PlayerControl.isWatchingMiniGames(victim)) {
				e.setCancelled(true);
			}
			else if (PlayerControl.isPlayingMiniGame(victim)) {
				MiniGame miniGame = MiniGameLoading.getMiniGameForPlayer(victim);
				if (miniGame.isOver()) {
					e.setCancelled(true);
				}
				else {
					//AA_OnScreenHitMessages.pvpHitMessage(victim, null, e); 	//can't really detect indirect PVP damage
				}
			}
		}
	}

	//	@EventHandler
	//	public void onPotionSplash(final PotionSplashEvent e) {
	//		if (e.getEntity().getShooter() instanceof Player) {
	//			for (LivingEntity victim: e.getAffectedEntities()) {
	//				if (victim instanceof Player) {
	//					onPlayerDamagesPlayer((Player) e.getEntity().getShooter(), (Player) victim);
	//				}
	//			}
	//		}
	//	}
	//
	//	@EventHandler
	//	public void onAreaEffectCloudApply(final AreaEffectCloudApplyEvent e) {
	//		if (e.getEntity().getSource() instanceof Player) {
	//			for (LivingEntity victim: e.getAffectedEntities()) {
	//				if (victim instanceof Player) {
	//					onPlayerDamagesPlayer((Player) e.getEntity().getSource(), (Player) victim);
	//				}
	//			}
	//		}
	//	}

	//	@EventHandler
	//	public void onEntityCombustByEntity(EntityCombustByEntityEvent e) {
	//		if (e.getEntity() instanceof Player && e.getCombuster() instanceof Player) {
	//			onPlayerDamagesPlayer((Player) e.getCombuster(), (Player) e.getEntity());
	//		}
	//	}
	//
	//	private void onPlayerDamagesPlayer(Player attacker, Player victim) {
	//
	//
	//	}



	@EventHandler
	public void onCreatureSpawn(final CreatureSpawnEvent e) {
		if (HubControl.isInMgHubAABB(e.getLocation())) {
			if (e.getSpawnReason() == SpawnReason.NATURAL) {
				MiniGame mg = MiniGameLoading.getMiniGameContainingLocation(e.getLocation());
				if (mg != null && mg.isAnySessionActive()) { //only cancel mob-spawning for active sessions to prevent memory leaks
					e.setCancelled(true);
				}
			}
		}
	}

	@EventHandler
	public void onPlayerDeath(final PlayerDeathEvent event) {
		Player dyingPlayer = event.getEntity();
		if (PlayerControl.isPlayingMiniGame(dyingPlayer)) {
			event.setKeepLevel(true);
			ScoreManager.onPlayerDeath(dyingPlayer);
			MiniGameSessions.leaveCurrentSession(dyingPlayer, true);
		}
		else if (PlayerControl.isEditingMiniGame(dyingPlayer)) {
			event.setKeepLevel(true);
			MiniGameSessions.leaveCurrentSession(dyingPlayer, true);
		}
		else {
			event.setKeepLevel(false);
		}
	}

	@EventHandler
	public void onEntityDeath(final EntityDeathEvent event) {
		Player killer = event.getEntity().getKiller();
		if (killer != null && PlayerControl.isPlayingMiniGame(killer)) {
			ScoreManager.onEntityDeath(event.getEntity(), killer);
		}
	}

	@EventHandler
	public void onPlayerRespawn(final PlayerRespawnEvent event) {
		Player player = event.getPlayer();
		if (HubControl.isInMiniGameHub(player)) {
			PluginManagement.executeDelayed(0.2, new Runnable() {

				@Override
				public void run() {
					HubControl.becomeSpectator(event.getPlayer(), false, event.getPlayer().getBedSpawnLocation(), false);
				}
			});
		}
	}



	@EventHandler (priority = EventPriority.HIGHEST)
	public void onPlayerBedEnter(final PlayerBedEnterEvent e) {
		if (HubControl.isInMiniGameHub(e.getPlayer())) {
			e.setCancelled(true);
		}
	}

	@EventHandler (priority = EventPriority.HIGHEST)
	public void onPlayerExpChange(final PlayerExpChangeEvent e) {
		if (PlayerControl.isEditingMiniGame(e.getPlayer())) {
			e.setAmount(0);
		}
	}

	@EventHandler (priority = EventPriority.HIGHEST)
	public void onPlayerTeleport(final PlayerTeleportEvent e) {
		if ((PlayerControl.isPlayingMiniGame(e.getPlayer()) || PlayerControl.isEditingMiniGame(e.getPlayer()))
			&& !MiniGameLoading.getMiniGameForPlayer(e.getPlayer()).isInsidePlayableBounds(e.getTo())) {
			e.setCancelled(true);
		}
	}

	//	@EventHandler
	//	public void onBlockPistonExtend(BlockPistonExtendEvent e) {
	//		for (Block b: e.getBlocks()) {
	//			if (TerrainHelper.isUndestroyableArenaBorder(b)) {
	//				e.setCancelled(true);
	//				break;
	//			}
	//		}
	//	}
	//
	//	@EventHandler
	//	public void onBlockPistonRetract(BlockPistonRetractEvent e) {
	//		for (Block b: e.getBlocks()) {
	//			if (TerrainHelper.isUndestroyableArenaBorder(b)) {
	//				e.setCancelled(true);
	//				break;
	//			}
	//		}
	//	}

	@EventHandler (priority = EventPriority.HIGHEST)
	public void onPortalCreate(final PortalCreateEvent e) {
		MiniGame mg = MiniGameLoading.getMiniGameContainingLocation(e.getBlocks().iterator().next().getLocation());
		if (mg != null) {
			for (Block b: e.getBlocks()) {
				b.setType(Material.STONE);
			}
			e.setCancelled(true);
		}
	}

	//	@EventHandler
	//	public void onBlockPlace(final BlockPlaceEvent event) {
	//		onBlockModify(event.getBlock(), event.getPlayer(), event);
	//	}
	//
	public void onBlockModify(final Block block, final Player player, final Cancellable c) {
		antiCheatControl(player, block, c); //TODO rellay needed ?
	}

	//
	//	@EventHandler
	//	public void onInventoryOpen(final InventoryOpenEvent event) {//use block enter event ?
	//		if (event.getPlayer() instanceof Player) {
	//			antiCheatControl((Player) event.getPlayer(), null, event);
	//		}
	//	}
	//
	//	@EventHandler
	//	public void onPlayerDropItem(final PlayerDropItemEvent event) {
	//		antiCheatControl(event.getPlayer(), null, event);
	//	}



	// ############## SIGN EDIT ################


	@EventHandler
	public void onBlockBreak(final BlockBreakEvent event) {
		onBlockModify(event.getBlock(), event.getPlayer(), event);
		if (event.isCancelled()) return;
		AA_SignCommand signCommand = AA_SignCommand.createFrom(event.getBlock());
		if (signCommand != null) {
			signCommand.executeOnBreak(event.getPlayer(), event);
		}
		List<Block> attachedSigns = TerrainHelper.getAttachedSigns(event.getBlock());
		for (Block b: attachedSigns) {
			signCommand = AA_SignCommand.createFrom(b);
			if (signCommand != null) {
				signCommand.executeOnBreak(event.getPlayer(), event);
			}
		}

	}

	@EventHandler
	public void onEntityExplode(final EntityExplodeEvent event) {
		List<Block> blocksToRemove = new ArrayList<Block>();
		for (Block b: event.blockList()) {
			AA_SignCommand signCommand = AA_SignCommand.createFrom(b);
			if (signCommand != null && signCommand.isClickCommand()) { //TODO protect in editSession
				blocksToRemove.add(b);
				blocksToRemove.add(signCommand.getAttachedBlock());
			}
			else if (TerrainHelper.isUndestroyableArenaBorder(b)) {
				blocksToRemove.add(b);
			}
		}
		event.blockList().removeAll(blocksToRemove);
	}


	@EventHandler
	public void onSignChange(final SignChangeEvent event) {
		final AA_SignCommand signCommand = AA_SignCommand.createFrom(event.getBlock(), event.getLines(), event);
		if (signCommand != null) {
			signCommand.executeOnCreation(event.getPlayer(), event.getPlayer().getWorld());
		}
	}



	// ############## SIGN CLICKING ################

	@EventHandler
	public void onPlayerInteract(final PlayerInteractEvent event) {
		if (event.getPlayer().getGameMode().equals(GameMode.CREATIVE) && event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
			onClick(event.getPlayer(), event.getClickedBlock());
		}
		if (HubControl.isInMgHubAABB(event.getPlayer().getLocation())
			&& event.getClickedBlock() != null
			&& event.getClickedBlock().getType() == Material.ENDER_CHEST) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onPlayerAnimation(final PlayerAnimationEvent event) {
		if (event.getPlayer().getGameMode().equals(HubControl.MINIGAME_HUB_GAMEMODE) && event.getAnimationType().equals(PlayerAnimationType.ARM_SWING)) {
			List<Block> targetBlocks = event.getPlayer().getLineOfSight(transparent, 3);
			for (int i = 0; i < targetBlocks.size(); i++) {
				if (targetBlocks.get(i).getType().equals(Material.SIGN_POST)) {
					Events.onClick(event.getPlayer(), targetBlocks.get(i));
					break;
				}
				else if (targetBlocks.get(i).getType().equals(Material.WALL_SIGN)
					&& i + 1 == targetBlocks.size()
					|| i + 1 < targetBlocks.size()
					&& !targetBlocks.get(i + 1).getType().equals(Material.WALL_SIGN)) {
					Events.onClick(event.getPlayer(), targetBlocks.get(i));
					break;
				}
			}
		}
	}

	public static void onClick(final Player player, final Block block) {
		AA_SignCommand signCommand = AA_SignCommand.createFrom(block);
		if (signCommand != null) {
			signCommand.executeOnClick(player, null);
		}
	}



	//	@EventHandler
	//	public void onPlayerQuit(final PlayerQuitEvent event) {
	//		FileConfiguration config = AdventureArena.getInstance().getConfig();
	//		config.set("lastGameMode." + event.getPlayer().getName(), event.getPlayer().getGameMode().toString());
	//		AdventureArena.getInstance().saveConfig();
	//	}
	//


	//	@EventHandler
	//	public void onPlayerInteract(final PlayerInteractEvent event) {
	//		if(event.getAction().equals(Action.PHYSICAL) && event.getClickedBlock().getType() == Material.STONE_PLATE){
	//			AdventureArena.broadcast("STONE_PLATE EVENT !");
	//			//event.setCancelled(true);
	//
	//		}
	//	}



	// ############## UTIL FUNC ################

	/**
	 * Retracing attacking player
	 *
	 * @param event
	 * @return The player who attacked (through a bow eg.) or null
	 */
	private Player getAttackingPlayerOverProjectile(Entity damager) {
		if (damager instanceof Player) return (Player) damager;
		if (damager instanceof Projectile) {
			ProjectileSource shooter = ((Projectile) damager).getShooter();
			if (shooter instanceof Player) return (Player) shooter;
		}
		return null;
	}

	private void antiCheatControl(final Player player, final Block block, final Cancellable c) { //TODO rework this
		if (player != null && player.getGameMode() == GameMode.CREATIVE && !player.isOp()) {
			if (block != null && TerrainHelper.isUndestroyableArenaBorder(block)) {
				c.setCancelled(true);
				MessageSystem.sideNote("You can leave this area by right-clicking a sign labeled " + ChatColor.BLUE + "[exit]", player);
			}
			if (!PlayerControl.isPlayerInsideHisEditableArea(player)) {
				c.setCancelled(true);
				PluginManagement.executeDelayed(0.1, new Runnable() {

					@Override
					public void run() {
						HubControl.kickFromMiniGameAndHub(player);
						MessageSystem.sideNote("You escaped with CREATIVE somehow...", player);
						player.setGameMode(Bukkit.getDefaultGameMode());
					}
				});
			}

		}
	}


}
