package adventureArena;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class AA_Commands implements CommandExecutor {


	public static final String saveInventory = "saveInventory";
	public static final String restoreInventory = "restoreInventory";


	public AA_Commands(final JavaPlugin javaPlugin) {
		javaPlugin.getCommand(saveInventory).setExecutor(this);
		javaPlugin.getCommand(restoreInventory).setExecutor(this);
	}




	@Override
	public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
		String commandName = command.getName();
		if (!sender.isOp()) {
			sender.sendMessage("You need to be Op for this.");
			return true;
		}

		if (commandName.equals(saveInventory)) {
			if (args.length != 1) return false;
			Player player = AdventureArena.getOnlinePlayerStartingWith(args[0]);
			if (player == null) return false;
			InventorySaver.saveInventoryAndPlayerMeta(player);
		}

		if (commandName.equals(restoreInventory)) {
			if (args.length != 1) return false;
			Player player = AdventureArena.getOnlinePlayerStartingWith(args[0]);
			if (player == null) return false;
			InventorySaver.restoreInventoryAndPlayerMeta(player);
		}


		return true;
	}




}
