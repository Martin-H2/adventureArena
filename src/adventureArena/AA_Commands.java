package adventureArena;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class AA_Commands implements CommandExecutor {


	public static final String saveInventory = "saveInventory";
	public static final String restoreInventory = "restoreInventory";
	public static final String disableDigging = "disableDigging";
	public static final String enableDigging = "enableDigging";
	public static final PotionEffect MINING_FATIGUE_5 = new PotionEffect(PotionEffectType.SLOW_DIGGING, Integer.MAX_VALUE, 5, true);


	public AA_Commands(final JavaPlugin javaPlugin) {
		javaPlugin.getCommand(saveInventory).setExecutor(this);
		javaPlugin.getCommand(restoreInventory).setExecutor(this);
		javaPlugin.getCommand(disableDigging).setExecutor(this);
		javaPlugin.getCommand(enableDigging).setExecutor(this);
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
			AA_InventorySaver.saveInventoryAndPlayerMeta(player);
			AdventureArena.getInstance().getConfig().set("isInArena." + player.getName(), true);
			AdventureArena.getInstance().saveConfig();
		}

		if (commandName.equals(restoreInventory)) {
			if (args.length != 1) return false;
			Player player = AdventureArena.getOnlinePlayerStartingWith(args[0]);
			if (player == null) return false;
			AA_InventorySaver.restoreInventoryAndPlayerMeta(player);
			AdventureArena.getInstance().getConfig().set("isInArena." + player.getName(), false);
			AdventureArena.getInstance().saveConfig();
		}

		if (commandName.equals(disableDigging)) {
			if (args.length != 1) return false;
			Player player = AdventureArena.getOnlinePlayerStartingWith(args[0]);
			if (player == null) return false;
			player.addPotionEffect(MINING_FATIGUE_5);
		}

		if (commandName.equals(enableDigging)) {
			if (args.length != 1) return false;
			Player player = AdventureArena.getOnlinePlayerStartingWith(args[0]);
			if (player == null) return false;
			player.removePotionEffect(MINING_FATIGUE_5.getType());
		}


		return true;
	}




}
