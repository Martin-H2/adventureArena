package adventureArena;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class AA_Commands implements CommandExecutor {


	public static final String joinMiniGameHub = "joinMiniGameHub";
	public static final String leaveMiniGameHub = "leaveMiniGameHub";
	public static final String serverinfo = "serverinfo";
	public static final String mg = "mg";


	public AA_Commands(final JavaPlugin javaPlugin) {
		javaPlugin.getCommand(joinMiniGameHub).setExecutor(this);
		javaPlugin.getCommand(leaveMiniGameHub).setExecutor(this);
		javaPlugin.getCommand(serverinfo).setExecutor(this);
		javaPlugin.getCommand(mg).setExecutor(this);
	}



	@Override
	public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
		String commandName = command.getName();

		if (commandName.equals(mg) && args.length == 1 && sender instanceof Player) {
			Player player = (Player) sender;
			if(args[0].equals("info")) {
				AA_MiniGameControl.surroundingMiniGameInfo(player);
				return true;
			}
			if(args[0].equals("resetScore") && AA_MiniGameControl.isPlayerInsideHisEditableArea(player)) {
				AA_ScoreManager.surroundingMiniGameScoreReset(player);
				return true;
			}
		}

		// ############### OPERATOR ONLY #################
		if (!sender.isOp()) {
			AA_MessageSystem.error("You need to be Op for this.", sender);
			return true;
		}


		if (commandName.equals(mg))  {
			if (args.length == 3 && args[0].equals("addAllowedEditor")) {
				Player player = AdventureArena.getOnlinePlayerStartingWith(args[2]);
				if (player == null) return false;
				try {
					int id = Integer.parseInt(args[1]);
					AA_MiniGameControl.addAllowedEditor(id, player, sender);
					return true;
				} catch (NumberFormatException e) {
					return false;
				}

			}
			if (args.length == 3 && args[0].equals("removeAllowedEditor")) {
				Player player = AdventureArena.getOnlinePlayerStartingWith(args[2]);
				if (player == null) return false;
				try {
					int id = Integer.parseInt(args[1]);
					AA_MiniGameControl.removeAllowedEditor(id, player, sender);
					return true;
				} catch (NumberFormatException e) {
					return false;
				}

			}
		}

		if (commandName.equals(serverinfo))  {
			serverInfo(sender);
			return true;
		}


		//joinMiniGameHub Rei 124 40 -60
		else if (commandName.equals(joinMiniGameHub)) {
			if (args.length != 1 && args.length != 4) return false;
			Player player = AdventureArena.getOnlinePlayerStartingWith(args[0]);
			if (player == null) return false;
			Location target = null;
			if (args.length == 4) {
				try {
					int x = Integer.parseInt(args[1]);
					int y = Integer.parseInt(args[2]);
					int z = Integer.parseInt(args[3]);
					target = new Location(player.getWorld(), x, y, z);
				} catch (NumberFormatException e) {
					return false;
				}
			}
			AA_MiniGameControl.joinMiniGameHub(player, target);
			return true;
		}

		//leaveMiniGameHub Rei 124 65 160
		else if (commandName.equals(leaveMiniGameHub)) {
			if (args.length != 1 && args.length != 4) return false;
			Player player = AdventureArena.getOnlinePlayerStartingWith(args[0]);
			if (player == null) return false;
			Location target = null;
			if (args.length == 4) {
				try {
					int x = Integer.parseInt(args[1]);
					int y = Integer.parseInt(args[2]);
					int z = Integer.parseInt(args[3]);
					target = new Location(player.getWorld(), x, y, z);
				} catch (NumberFormatException e) {
					return false;
				}
			}
			AA_MiniGameControl.leaveMiniGameHub(player, target);
			return true;
		}


		return false;
	}



	private void serverInfo(final CommandSender sender) {
		AA_MessageSystem.sideNote("java.version: " + System.getProperty("java.version"), sender);
		AA_MessageSystem.sideNote("os.arch: " + System.getProperty("os.arch"), sender);

	}


}
