package adventureArena;

import java.util.Collection;

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


	public static String getErrorColor() {
		return ChatColor.RED.toString();
	}

	public static String getSuccessColor() {
		return ChatColor.GREEN.toString();
	}


	public static void gameplayWarning(final Object o, final Player p) {
		send(ChatColor.DARK_PURPLE + "[MiniGames] " + ChatColor.ITALIC + o.toString(), p);
	}
	public static void gameplayWarningAll(final Object o) {
		gameplayWarning(o, null);
	}
	public static void gameplayWarningForGroup(final Object o, final Collection<Player> cp) {
		for (Player p: cp) {
			gameplayWarning(o, p);
		}
	}



	public static void errorForGroup(final Object o, final Collection<Player> cp) {
		for (Player p: cp) {
			error(o, p);
		}
	}
	public static void error(final Object o, final CommandSender sender) {
		send(ChatColor.DARK_RED + "[Error] " + getErrorColor() + o.toString(), sender);
	}
	public static void errorAll(final Object o) {
		error(o, null);
	}
	public static void success(final Object o, final CommandSender p) {
		send(ChatColor.DARK_GREEN + "[Success] " + getSuccessColor() + o.toString(), p);
	}
	public static void sideNoteForGroup(final Object o, final Collection<Player> cp) {
		for (Player p: cp) {
			sideNote(o, p);
		}
	}
	public static void sideNote(final Object o, final CommandSender p) {
		send(ChatColor.DARK_GRAY + "[Note] " + ChatColor.GRAY + o.toString(), p);
	}
	public static void example(final Object o, final CommandSender p) {
		send(ChatColor.DARK_GRAY + "[Example] " + ChatColor.GOLD + o.toString(), p);
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
