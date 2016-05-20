package adventureArena.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import adventureArena.*;
import adventureArena.control.HubControl;
import adventureArena.control.MiniGameManagement;
import adventureArena.control.PlayerControl;


public class MgCommands extends AbstractCommand {


	public MgCommands() {
		super("mg", false);
	}


	@Override
	boolean onCommand(CommandSender sender, String[] args) {
		if (args.length < 1) return false;
		String subCommand = args[0];


		// ############### INGAME ONLY #################
		if (sender instanceof Player) {
			Player commandSender = (Player) sender;

			if (subCommand.equals("info")) {
				MiniGameManagement.surroundingMiniGameInfo(commandSender);
				return true;
			}

			if (PlayerControl.isPlayerInsideHisEditableArea(commandSender)) {
				switch (subCommand) {
				case "resetScore":
					AA_ScoreManager.surroundingMiniGameScoreReset(commandSender);
					return true;
				case "wipeSession":
					MiniGameManagement.surroundingMiniGameSessionWipe(commandSender);
					return true;
				case "aas":
					MiniGameManagement.surroundingMiniGameAllowAllSpectators(commandSender);
					return true;
				case "resetRoom":
					if (commandSender.isOp()) {
						MiniGameManagement.surroundingMiniGameRoomReset(commandSender);
					}
					return true;
				case "fixSigns":
					MiniGameManagement.surroundingMiniGameFixSigns(commandSender);
					return true;
				}
			}
			else {
				MessageSystem.error("You need to be above a miniGame and edit access.", sender);
				return true;
			}
		}


		// ############### CONSOLE / OP #################
		if (args.length == 3 && subCommand.equals("addAllowedEditor")) {
			if (!sender.isOp()) {
				MessageSystem.error(NEED_OP_MESSAGE, sender);
			}
			else {
				Player player = AdventureArena.getOnlinePlayerStartingWith(args[2]);
				if (player == null) return false;
				try {
					int id = Integer.parseInt(args[1]);
					MiniGameManagement.addAllowedEditor(id, player, sender);
				}
				catch (NumberFormatException e) {
					return false;
				}
			}
			return true;
		}
		else if (args.length == 3 && subCommand.equals("removeAllowedEditor")) {
			if (!sender.isOp()) {
				MessageSystem.error(NEED_OP_MESSAGE, sender);
			}
			else {
				try {
					int id = Integer.parseInt(args[1]);
					MiniGameManagement.removeAllowedEditor(id, args[2], sender);
				}
				catch (NumberFormatException e) {
					return false;
				}
			}
			return true;
		}
		else if (args.length == 2 && subCommand.equals("rebuildMiniGameCfg")) {
			if (!sender.isOp()) {
				MessageSystem.error(NEED_OP_MESSAGE, sender);
			}
			else {
				HubControl.rebuildMiniGameCfgFromSigns(args[1]);
			}
			return true;
		}


		return false;
	}

}
