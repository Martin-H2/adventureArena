package adventureArena;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class AA_Events implements Listener {

	@EventHandler
	public void onPlayerInteract(final PlayerInteractEvent event) {
		if(event.getAction().equals(Action.PHYSICAL) && event.getClickedBlock().getType() == Material.STONE_PLATE){
			AdventureArena.broadcast("STONE_PLATE EVENT !");
			//event.setCancelled(true);

		}
	}





}
