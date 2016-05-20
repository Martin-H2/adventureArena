package adventureArena.commands;

import org.bukkit.command.CommandSender;
import adventureArena.MessageSystem;


public class ServerInfoCommand extends AbstractCommand {


	public ServerInfoCommand() {
		super("serverinfo", false);
	}


	@Override
	boolean onCommand(CommandSender sender, String[] args) {
		MessageSystem.sideNote("java.version: " + System.getProperty("java.version"), sender);
		MessageSystem.sideNote("java.vendor: " + System.getProperty("java.vendor"), sender);
		MessageSystem.sideNote("java.library.path: " + System.getProperty("java.library.path"), sender);
		MessageSystem.sideNote("os.arch: " + System.getProperty("os.arch"), sender);
		MessageSystem.sideNote("user.name: " + System.getProperty("user.name"), sender);
		return true;
	}

}
