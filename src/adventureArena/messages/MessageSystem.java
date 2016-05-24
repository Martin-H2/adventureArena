package adventureArena.messages;

import java.util.Collection;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public class MessageSystem {

	public final static ConsoleCommandSender	CONSOLE_SENDER	= Bukkit.getConsoleSender();

	// UNSTYLED

	public static void broadcast(final Object o) {
		for (Player p: Bukkit.getOnlinePlayers()) {
			p.sendMessage(o != null ? o.toString() : "null");
		}
	}

	private static void send(final String message, final CommandSender p) {
		if (p == null) {
			broadcast(message);
		}
		else {
			p.sendMessage(message);
		}
	}



	// STYLED

	public static void error(final Object o, final CommandSender sender) {
		send(ChatColor.DARK_RED + "[Error] " + getErrorColor() + o.toString(), sender);
	}

	public static void errorToAll(final Object o) {
		error(o, null);
	}

	public static void errorToGroup(final Object o, final Collection<Player> cp) {
		for (Player p: cp) {
			error(o, p);
		}
	}

	public static void warning(final Object o, final Player p) {
		send(ChatColor.GOLD + "[Warning] " + ChatColor.ITALIC + o.toString(), p);
	}

	public static void warningToAll(final Object o) {
		warning(o, null);
	}

	public static void warningToGroup(final Object o, final Collection<Player> cp) {
		for (Player p: cp) {
			warning(o, p);
		}
	}

	public static void success(final Object o, final CommandSender p) {
		send(ChatColor.DARK_GREEN + "[Success] " + getSuccessColor() + o.toString(), p);
	}

	public static void sideNote(final Object o, final CommandSender p) {
		send(ChatColor.DARK_GRAY + "[Note] " + ChatColor.GRAY + o.toString(), p);
	}

	public static void sideNoteToGroup(final Object o, final Collection<Player> cp) {
		for (Player p: cp) {
			sideNote(o, p);
		}
	}

	public static void example(final Object o, final CommandSender p) {
		send(ChatColor.DARK_GRAY + "[Example] " + ChatColor.GRAY + o.toString(), p);
	}



	// CONSOLE

	public static void consoleDebug(final Object o) {
		CONSOLE_SENDER.sendMessage(ChatColor.GRAY + "[AA] " + ChatColor.AQUA + "DEBUG - " + o);
	}

	public static void consoleInfo(final Object o) {
		CONSOLE_SENDER.sendMessage(ChatColor.GRAY + "[AA] " + ChatColor.DARK_GRAY + o);
	}

	public static void consoleWarn(final Object o) {
		CONSOLE_SENDER.sendMessage(ChatColor.GRAY + "[AA] " + ChatColor.YELLOW + "WARNING - " + o);
	}

	public static void consoleError(final Object o) {
		CONSOLE_SENDER.sendMessage(ChatColor.GRAY + "[AA] " + ChatColor.RED + "ERROR - " + o);
	}



	// UTIL

	public static String getErrorColor() {
		return ChatColor.RED.toString();
	}

	public static String getSuccessColor() {
		return ChatColor.GREEN.toString();
	}


	public static void playSoundToEveryone(final Sound sound) {
		for (Player player: Bukkit.getOnlinePlayers()) {
			player.playSound(player.getLocation(), sound, 1, 1);
		}

	}



}
