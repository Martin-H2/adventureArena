package adventureArena;

import java.util.*;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.util.BlockVector;

@SuppressWarnings ("deprecation")
public class AA_Events implements Listener {

	static Map<Player, BlockVector>	currentPlayerBlockPos	= new HashMap<Player, BlockVector>();


	private static HashSet<Byte>	transparent				= new HashSet<Byte>();
	static {
		transparent.add((byte) Material.AIR.getId());
		transparent.add((byte) Material.LONG_GRASS.getId());
		transparent.add((byte) Material.WALL_SIGN.getId());
	}

	@EventHandler
	public void onPlayerMove(final PlayerMoveEvent e) {
		BlockVector newPosRounded = e.getTo().toVector().toBlockVector();
		if (newPosRounded.equals(currentPlayerBlockPos.get(e.getPlayer()))) return;
		else {
			currentPlayerBlockPos.put(e.getPlayer(), newPosRounded);
			onBlockEnter(e.getPlayer(), newPosRounded, e.getTo().getBlock());
		}

		//		Location target = e.getTo();
		//
		//		int simpleHash = target.getBlockX()+target.getBlockY()*100+target.getBlockX()*10000;
	}

	private void onBlockEnter(final Player player, final BlockVector newPosRounded, final Block block) {
		//player.sendMessage("BLOCK-ENTER: " + newPosRounded.toString());
		if (AA_MiniGameControl.isPlayingMiniGame(player)) {
			AA_MiniGame mg = AA_MiniGameControl.getMiniGameForPlayer(player);
			if (mg != null) {
				for (AA_BlockTrigger mt: mg.getRangedMonsterTriggers()) {
					//AA_MessageSystem.consoleDebug("TRIGGER: " + mt);
					mt.checkRangeAndTrigger(player, mg);
				}
			}
		}
	}



	@EventHandler
	public void onPlayerJoin(final PlayerJoinEvent event) {
		if (AA_MiniGameControl.isInMgHubAABB(event.getPlayer().getLocation())) {
			AA_MiniGameControl.kickIfPlayingMiniGame(event.getPlayer());
			AA_MiniGameControl.leaveMiniGameHub(event.getPlayer(), null);
		}
	}



	// ############## MINIGAME GAMEPLAY ################

	//	@EventHandler
	//	public void onBlockRedstone(final BlockRedstoneEvent e) {
	//		AA_MessageSystem.consoleWarn("BlockRedstoneEvent[" + e.getBlock().getType() + "]: " + e.getOldCurrent() + "->" + e.getNewCurrent());
	//	}

	//	@EventHandler
	//	public void onEntityRegainHealth(final EntityRegainHealthEvent e) {
	//		if (e.getEntity() instanceof Player && AA_MiniGameControl.isPlayingMiniGame((Player) e.getEntity())) {
	//			e.setCancelled(true);
	//		}
	//	}

	@EventHandler
	public void onEntityDamageByEntity(final EntityDamageByEntityEvent e) {
		if (e.getEntity() instanceof Player) {
			Player attackedPlayer = (Player) e.getEntity();
			if (AA_MiniGameControl.isWatchingMiniGames(attackedPlayer)) {
				e.setCancelled(true);
			}
			else if (AA_MiniGameControl.isPlayingMiniGame(attackedPlayer) && e.getDamager() instanceof Player) {
				Player attackingPlayer = (Player) e.getDamager();
				AA_MiniGame mg = AA_MiniGameControl.getMiniGameForPlayer(attackedPlayer); //TODO !TEST
				if (AA_TeamManager.isAllied(attackedPlayer, attackingPlayer)) {
					e.setCancelled(true);
				}
				else if (!mg.isPvpDamage()) {
					if (e.getCause() == DamageCause.PROJECTILE) {
						e.setDamage(0); //allow pushback only for projectiles
					}
					else {
						e.setCancelled(true);
					}
				}
			}
		}
	}

	@EventHandler
	public void onEntityDamage(final EntityDamageEvent e) {
		if (e.getEntity() instanceof Player) {
			Player p = (Player) e.getEntity();
			AA_MiniGame mg = AA_MiniGameControl.getMiniGameForPlayer(p);
			if (AA_MiniGameControl.isPlayingMiniGame(p) && mg != null && mg.isOver()) {
				e.setCancelled(true);
			}
		}
	}

	//	@EventHandler
	//	public void onItemSpawn(final ItemSpawnEvent e) {
	//		if(AA_MiniGameControl.getMiniGameContainingLocation(e.getLocation())!=null){
	//			//test
	//			//e.setCancelled(true);
	//		}
	//	}

	@EventHandler
	public void onCreatureSpawn(final CreatureSpawnEvent e) {
		if (AA_MiniGameControl.isInMgHubAABB(e.getLocation())) {
			//AA_MessageSystem.consoleDebug("CreatureSpawnEvent: " + e.getSpawnReason() + ", " + e.getEntityType());
			if (e.getSpawnReason() == SpawnReason.NATURAL) {
				AA_MiniGame mg = AA_MiniGameControl.getMiniGameContainingLocation(e.getLocation());
				if (mg != null && (mg.isInProgress() || mg.isLockedByEditSession())) {
					e.setCancelled(true);
				}
			}
		}
	}

	@EventHandler
	public void onPlayerDeath(final PlayerDeathEvent event) {
		Player player = event.getEntity();
		//player.sendMessage("[onPlayerDeath] player.isDead() = " + player.isDead());
		if (AA_MiniGameControl.isPlayingMiniGame(player)) {
			event.setKeepLevel(true);
			AA_ScoreManager.onPlayerDeath(event.getEntity());
			AA_MiniGameControl.leaveCurrentMiniGame(player, true);
		}
		else {
			event.setKeepLevel(false);
		}
	}

	@EventHandler
	public void onEntityDeath(final EntityDeathEvent event) {
		Player killer = event.getEntity().getKiller();
		if (killer != null && AA_MiniGameControl.isPlayingMiniGame(killer)) {
			AA_ScoreManager.onEntityDeath(event.getEntity(), killer);
		}
	}

	@EventHandler
	public void onPlayerRespawn(final PlayerRespawnEvent event) {
		if (AA_MiniGameControl.isInMiniGameHub(event.getPlayer())) {
			AdventureArena.executeDelayed(0.2, new Runnable() {

				@Override
				public void run() {
					AA_MiniGameControl.setMiniGameSpectator(event.getPlayer(), false, event.getPlayer().getBedSpawnLocation());
				}
			});
		}
	}

	@EventHandler
	public void onPlayerQuit(final PlayerQuitEvent e) {
		AA_MiniGameControl.kickFromMiniGameAndHub(e.getPlayer());
	}

	@EventHandler
	public void onPlayerKick(final PlayerKickEvent e) {
		AA_MiniGameControl.kickFromMiniGameAndHub(e.getPlayer());
	}



	// ############## NO CHEATING WITH CREATIVE ################

	@EventHandler (priority = EventPriority.HIGHEST)
	public void onPlayerBedEnter(final PlayerBedEnterEvent e) {
		if (AA_MiniGameControl.isInMiniGameHub(e.getPlayer())) {
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onPlayerExpChange(final PlayerExpChangeEvent e) {
		if (e.getPlayer().getGameMode() == GameMode.CREATIVE) {
			e.setAmount(0);
		}
	}

	@EventHandler
	public void onPlayerTeleport(final PlayerTeleportEvent e) {
		if ((AA_MiniGameControl.isPlayingMiniGame(e.getPlayer()) || AA_MiniGameControl.isEditingMiniGame(e.getPlayer())) //TODO !test
			&& !AA_MiniGameControl.getMiniGameForPlayer(e.getPlayer()).isInsideBounds(e.getTo())) {
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onPortalCreate(final PortalCreateEvent e) {
		AA_MiniGame mg = AA_MiniGameControl.getMiniGameContainingLocation(e.getBlocks().iterator().next().getLocation());
		if (mg != null) {
			for (Block b: e.getBlocks()) {
				b.setType(Material.STONE);
			}
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onBlockPlace(final BlockPlaceEvent event) {
		onBlockModify(event.getBlock(), event.getPlayer(), event);
	}

	public void onBlockModify(final Block block, final Player player, final Cancellable c) {
		antiCheatControl(player, block, c);
	}

	@EventHandler
	public void onInventoryOpen(final InventoryOpenEvent event) {//use block enter event ?
		if (event.getPlayer() instanceof Player) {
			antiCheatControl((Player) event.getPlayer(), null, event);
		}
	}

	@EventHandler
	public void onPlayerDropItem(final PlayerDropItemEvent event) {
		antiCheatControl(event.getPlayer(), null, event);
	}

	private void antiCheatControl(final Player player, final Block block, final Cancellable c) {
		if (player != null && player.getGameMode() == GameMode.CREATIVE && !player.isOp()) {
			if (block != null && AA_TerrainHelper.isUndestroyableArenaBorder(block)) {
				c.setCancelled(true);
				AA_MessageSystem.sideNote("You can leave this area by right-clicking a sign labeled " + ChatColor.BLUE + "[exit]", player);
			}
			if (!AA_MiniGameControl.isPlayerInsideHisEditableArea(player)) {
				c.setCancelled(true);
				AA_MiniGameControl.leaveCurrentMiniGame(player, false);
				AA_MessageSystem.sideNote("You escaped with CREATIVE somehow...", player);
			}

		}
	}



	// ############## SIGN EDIT ################

	@EventHandler
	public void onEntityExplode(final EntityExplodeEvent event) {
		List<Block> blocksToRemove = new ArrayList<Block>();
		for (Block b: event.blockList()) {
			AA_SignCommand signCommand = AA_SignCommand.createFrom(b);
			if (signCommand != null && signCommand.isClickCommand()) {
				blocksToRemove.add(b);
				blocksToRemove.add(signCommand.getAttachedBlock());
			}
			else if (AA_TerrainHelper.isUndestroyableArenaBorder(b)) {
				blocksToRemove.add(b);
			}
		}
		event.blockList().removeAll(blocksToRemove);
	}


	@EventHandler
	public void onBlockBreak(final BlockBreakEvent event) {
		onBlockModify(event.getBlock(), event.getPlayer(), event);
		if (event.isCancelled()) return;
		boolean isEditMode = AA_MiniGameControl.isEditingMiniGame(event.getPlayer());
		AA_SignCommand signCommand = AA_SignCommand.createFrom(event.getBlock());
		if (signCommand != null) {
			signCommand.executeOnBreak(event.getPlayer(), event, isEditMode);
		}
		List<Block> attachedSigns = AA_TerrainHelper.getAttachedSigns(event.getBlock());
		for (Block b: attachedSigns) {
			signCommand = AA_SignCommand.createFrom(b);
			if (signCommand != null) {
				signCommand.executeOnBreak(event.getPlayer(), event, isEditMode);
			}
		}

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
			onSignClick(event.getPlayer(), event.getClickedBlock());
			if (event.getClickedBlock().getType() == Material.ENDER_CHEST) {
				event.setCancelled(true);
				//Bukkit.createInventory(p, 27, ChatColor.GRAY + "EnderChest" + "(" + p.getName() + ")");
			}
		}
	}

	@EventHandler
	public void onPlayerAnimation(final PlayerAnimationEvent event) {
		if (event.getPlayer().getGameMode().equals(AA_MiniGameControl.MINIGAME_HUB_GAMEMODE) && event.getAnimationType().equals(PlayerAnimationType.ARM_SWING)) {
			List<Block> targetBlocks = event.getPlayer().getLineOfSight(transparent, 3);
			for (int i = 0; i < targetBlocks.size(); i++) {
				if (targetBlocks.get(i).getType().equals(Material.SIGN_POST)) {
					AA_Events.onSignClick(event.getPlayer(), targetBlocks.get(i));
					break;
				}
				else if (targetBlocks.get(i).getType().equals(Material.WALL_SIGN)
					&& i + 1 == targetBlocks.size()
					|| i + 1 < targetBlocks.size()
					&& !targetBlocks.get(i + 1).getType().equals(Material.WALL_SIGN)) {
					AA_Events.onSignClick(event.getPlayer(), targetBlocks.get(i));
					break;
				}
			}
		}
	}

	public static void onSignClick(final Player player, final Block block) {
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


}
