package adventureArena;

import java.util.HashSet;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerJoinEvent;

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



	@EventHandler
	public void onBlockBreak(final BlockBreakEvent event) {
		onBlockModify(event.getBlock(), event.getPlayer(), event);
	}

	@EventHandler
	public void onBlockPlace(final BlockPlaceEvent event) {
		onBlockModify(event.getBlock(), event.getPlayer(), event);
	}

	@EventHandler
	public void onSignChange(final SignChangeEvent event) {
		AA_SignCommand signCommand = AA_SignCommand.createFrom(event.getLines(), event);
		if (signCommand != null) {
			signCommand.execute();
		}
		//TODO what now ?
	}

	public void onBlockModify(final Block block, final Player player, final Cancellable c) {
		if (player!=null && !player.isOp() && player.getGameMode()==GameMode.CREATIVE && isForbiddenForArenaBuilders(block)) {
			c.setCancelled(true);
		}
	}



	private boolean isForbiddenForArenaBuilders(final Block block) {
		return block.getType().equals(Material.BEDROCK) || block.getType().equals(Material.STAINED_GLASS);
	}



	//	@EventHandler
	//	public void onPlayerInteract(final PlayerInteractEvent event) {
	//		if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
	//			Block targetBlock = event.getClickedBlock();
	//			if (targetBlock !=null && targetBlock.getState() instanceof Sign) {
	//				AA_MiniGameControl.signClick(event.getPlayer(), (Sign) targetBlock.getState());
	//			}
	//		}
	//	}

	@EventHandler
	public void onPlayerAnimation(final PlayerAnimationEvent event) {
		if (event.getAnimationType().equals(PlayerAnimationType.ARM_SWING)) {
			List<Block> targetBlocks = event.getPlayer().getLineOfSight(transparent, 3);
			for (int i=0; i<targetBlocks.size(); i++) {
				if (targetBlocks.get(i).getType().equals(Material.SIGN_POST)) {
					AA_SignCommandHandler.signClick(event.getPlayer(), targetBlocks.get(i));
					break;
				} else if (targetBlocks.get(i).getType().equals(Material.WALL_SIGN)
						&& i+1==targetBlocks.size()
						|| i+1<targetBlocks.size()
						&& !targetBlocks.get(i+1).getType().equals(Material.WALL_SIGN)) {
					AA_SignCommandHandler.signClick(event.getPlayer(), targetBlocks.get(i));
					break;
				}
			}
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
