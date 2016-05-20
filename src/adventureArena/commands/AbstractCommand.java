package adventureArena.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import adventureArena.AdventureArena;
import adventureArena.MessageSystem;


public abstract class AbstractCommand implements CommandExecutor {

	protected final static String	NEED_OP_MESSAGE	= "You need to be Op for this command.";

	private final String			commandString;
	private final boolean			isOpOnly;


	public AbstractCommand(String commandString, boolean isOpOnly) {
		this.commandString = commandString;
		this.isOpOnly = isOpOnly;
		AdventureArena.getInstance().getCommand(commandString).setExecutor(this);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (commandString.equals(command.getName())) {
			if (isOpOnly && !sender.isOp()) {
				MessageSystem.error(NEED_OP_MESSAGE, sender);
				return true;
			}
			else return onCommand(sender, args);
		}
		else return false;
	}

	abstract boolean onCommand(CommandSender sender, String[] args);

}
