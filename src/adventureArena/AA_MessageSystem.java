package adventureArena;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public class AA_MessageSystem {
	public final static ConsoleCommandSender CONSOLE_SENDER = Bukkit.getConsoleSender();

	public static void broadcast(final Object o) {
		for (Player p: Bukkit.getOnlinePlayers()) {
			p.sendMessage(o!=null?o.toString():"null");
		}
	}

	private static void send(final String message, final CommandSender p) {
		if (p==null) {
			broadcast(message);
		} else {
			p.sendMessage(message);
		}
	}





	public static void gameplayWarning(final Object o, final CommandSender p) {
		send(ChatColor.DARK_PURPLE + "[Arena] " + ChatColor.ITALIC + o.toString(), p);
	}
	public static void error(final Object o, final CommandSender sender) {
		send(ChatColor.RED + "[Error] " + ChatColor.ITALIC + o.toString(), sender);
	}
	public static void success(final Object o, final CommandSender p) {
		send(ChatColor.DARK_GREEN + "[Success] " + ChatColor.ITALIC + o.toString(), p);
	}
	public static void sideNote(final Object o, final CommandSender p) {
		send(ChatColor.GRAY + "[Note] " + ChatColor.ITALIC + o.toString(), p);
	}

	public static void gameplayWarning(final Object o) {
		gameplayWarning(o, null);
	}
	public static void sideNote(final Object o) {
		sideNote(o, null);
	}



	public static void playSoundToEveryone(final Sound sound) {
		for (Player player: Bukkit.getOnlinePlayers()) {
			player.playSound(player.getLocation(), sound, 1, 1);
		}

	}



	public static void consoleInfo(final Object o) {
		CONSOLE_SENDER.sendMessage(ChatColor.GRAY + "[AA] " + ChatColor.DARK_GRAY + o);
	}

	public static void consoleWarn(final Object o) {
		CONSOLE_SENDER.sendMessage(ChatColor.GRAY + "[AA] " + ChatColor.YELLOW + "WARNING - " +  o);
	}

	public static void consoleError(final Object o) {
		CONSOLE_SENDER.sendMessage(ChatColor.GRAY + "[AA] " + ChatColor.RED + "ERROR - " +  o);
	}




}
