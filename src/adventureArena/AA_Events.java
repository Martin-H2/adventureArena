package adventureArena;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

public class AA_Events implements Listener {



	@EventHandler
	public void onPlayerRespawn(final PlayerRespawnEvent event) {
		final Player player = event.getPlayer();
		FileConfiguration config = AdventureArena.getInstance().getConfig();
		if(config.getBoolean("isInArena." + player.getName(), false)){
			AdventureArena.executeDelayed(0.1, new Runnable() {
				@Override
				public void run() {
					player.addPotionEffect(AA_Commands.MINING_FATIGUE_5);
				}
			});
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
	//	public void onPlayerJoin(final PlayerJoinEvent event) {
	//		FileConfiguration config = AdventureArena.getInstance().getConfig();
	//		event.getPlayer().setGameMode(GameMode.valueOf(config.getString("lastGameMode." + event.getPlayer().getName(), "SURVIVAL")));
	//	}


	//	@EventHandler
	//	public void onPlayerInteract(final PlayerInteractEvent event) {
	//		if(event.getAction().equals(Action.PHYSICAL) && event.getClickedBlock().getType() == Material.STONE_PLATE){
	//			AdventureArena.broadcast("STONE_PLATE EVENT !");
	//			//event.setCancelled(true);
	//
	//		}
	//	}


}
