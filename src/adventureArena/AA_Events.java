package adventureArena;

import java.util.HashSet;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@SuppressWarnings("deprecation")
public class AA_Events implements Listener {


	private static HashSet<Byte> transparent = new HashSet<Byte>();
	static {
		transparent.add((byte) Material.AIR.getId());
		transparent.add((byte) Material.LONG_GRASS.getId());
		transparent.add((byte) Material.WALL_SIGN.getId());
	}


	@EventHandler
	public void onPlayerJoin(final PlayerJoinEvent event) {
		if(!AA_MiniGameControl.isInMiniGameHub(event.getPlayer())) {
			event.getPlayer().setGameMode(Bukkit.getDefaultGameMode());
		}
	}


	// ############## MINIGAME GAMEPLAY ################

	@EventHandler
	public void onPlayerDeath(final PlayerDeathEvent event) {
		if (AA_MiniGameControl.isPlayingMiniGame(event.getEntity())) {
			event.setKeepLevel(true);
			AdventureArena.executeDelayed(0.1, new Runnable() {
				@Override
				public void run() {
					AA_MiniGameControl.leaveMiniGame(event.getEntity());

				}
			});
		} else {
			event.setKeepLevel(false);
		}
	}
	@EventHandler
	public void onPlayerQuit(final PlayerQuitEvent e) {
		if (AA_MiniGameControl.isPlayingMiniGame(e.getPlayer())) {
			AA_MiniGameControl.leaveMiniGame(e.getPlayer());
		}
	}
	@EventHandler
	public void onPlayerKick(final PlayerKickEvent e) {
		if (AA_MiniGameControl.isPlayingMiniGame(e.getPlayer())) {
			AA_MiniGameControl.leaveMiniGame(e.getPlayer());
		}
	}



	// ############## NO CHEATING WITH CREATIVE ################

	@EventHandler
	public void onPlayerExpChange(final PlayerExpChangeEvent e) {
		if (e.getPlayer().getGameMode()==GameMode.CREATIVE) {
			e.setAmount(0);
		}
	}

	public void onBlockModify(final Block block, final Player player, final Cancellable c) {
		antiCheatControl(player, block, c);
	}

	@EventHandler
	public void onInventoryOpen(final InventoryOpenEvent event) {//TODO use block enter event ?
		if (event.getPlayer() instanceof Player) {
			antiCheatControl((Player) event.getPlayer(), null, event);
		}
	}

	@EventHandler
	public void onPlayerDropItem(final PlayerDropItemEvent event) {
		antiCheatControl(event.getPlayer(), null, event);
	}

	private void antiCheatControl(final Player player, final Block block, final Cancellable c) {
		if (player!=null && player.getGameMode()==GameMode.CREATIVE && !player.isOp()) {
			if (block!=null && AA_TerrainHelper.isUndestroyableArenaBorder(block)) {
				c.setCancelled(true);
				AA_MessageSystem.sideNote("You can leave this area by right-clicking a sign labeled " + ChatColor.BLUE + "[exit]", player);
			}
			if (!AA_MiniGameControl.isPlayerInsideHisEditableArea(player)) {
				c.setCancelled(true);
				AA_MiniGameControl.leaveMiniGame(player);
				AA_MessageSystem.sideNote("You escaped with CREATIVE somehow...", player);
			}

		}
	}




	// ############## SIGN EDIT ################

	@EventHandler
	public void onBlockBreak(final BlockBreakEvent event) {
		onBlockModify(event.getBlock(), event.getPlayer(), event);
		AA_SignCommand signCommand = AA_SignCommand.createFrom(event.getBlock());
		if (signCommand != null) {
			signCommand.executeOnBreak(event.getPlayer(), event);
		}
	}
	@EventHandler
	public void onBlockPlace(final BlockPlaceEvent event) {
		onBlockModify(event.getBlock(), event.getPlayer(), event);
	}
	@EventHandler
	public void onSignChange(final SignChangeEvent event) {
		final AA_SignCommand signCommand = AA_SignCommand.createFrom(event.getBlock(), event.getLines() , event);
		if (signCommand != null) {
			signCommand.executeOnCreation(event.getPlayer());
		}
	}




	// ############## SIGN CLICKING ################

	@EventHandler
	public void onPlayerInteract(final PlayerInteractEvent event) {
		if (event.getPlayer().getGameMode().equals(GameMode.CREATIVE) && event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
			onSignClick(event.getPlayer(), event.getClickedBlock());
		}
	}
	@EventHandler
	public void onPlayerAnimation(final PlayerAnimationEvent event) {
		if (event.getPlayer().getGameMode().equals(AA_MiniGameControl.MINIGAME_HUB_GAMEMODE) && event.getAnimationType().equals(PlayerAnimationType.ARM_SWING)) {
			List<Block> targetBlocks = event.getPlayer().getLineOfSight(transparent, 3);
			for (int i=0; i<targetBlocks.size(); i++) {
				if (targetBlocks.get(i).getType().equals(Material.SIGN_POST)) {
					AA_Events.onSignClick(event.getPlayer(), targetBlocks.get(i));
					break;
				} else if (targetBlocks.get(i).getType().equals(Material.WALL_SIGN)
						&& i+1==targetBlocks.size()
						|| i+1<targetBlocks.size()
						&& !targetBlocks.get(i+1).getType().equals(Material.WALL_SIGN)) {
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
	//	public void onPlayerRespawn(final PlayerRespawnEvent event) {
	//	}






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
