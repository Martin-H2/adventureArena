package adventureArena.commands;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import adventureArena.PluginManagement;
import adventureArena.control.HubControl;


public class JoinMghCommand extends AbstractCommand {


	public JoinMghCommand() {
		super("joinMiniGameHub", true);
	}


	@Override
	boolean onCommand(CommandSender sender, String[] args) {
		if (args.length != 1 && args.length != 4) return false;
		Player player = PluginManagement.getOnlinePlayerStartingWith(args[0]);
		if (player == null) return false;
		Location target = null;
		if (args.length == 4) {
			try {
				int x = Integer.parseInt(args[1]);
				int y = Integer.parseInt(args[2]);
				int z = Integer.parseInt(args[3]);
				target = new Location(HubControl.getMiniGameHubWorld(), x, y, z);
			}
			catch (NumberFormatException e) {
				return false;
			}
		}
		HubControl.joinMiniGameHub(player, target);
		return true;
	}

}
