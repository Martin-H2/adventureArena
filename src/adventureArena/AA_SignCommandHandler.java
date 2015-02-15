package adventureArena;

import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

public class AA_SignCommandHandler {



	public static void signClick(final Player player, final Block block) {
		if (block.getState() instanceof Sign) {
			Sign sign = (Sign) block.getState();
			AA_SignCommand.createFrom(sign.getLines(), null);
			//TODO what now ?
		}
	}


}
