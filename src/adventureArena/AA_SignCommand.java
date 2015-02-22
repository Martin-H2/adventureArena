package adventureArena;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.block.SignChangeEvent;

public class AA_SignCommand {

	private static final String ENCHANT_IDS_HELP = ChatColor.BLUE.toString() + ChatColor.UNDERLINE + "hub.spigotmc.org/javadocs/bukkit/org/bukkit/enchantments/Enchantment.html";
	private static final String MATERIAL_IDS_HELP = ChatColor.BLUE.toString() + ChatColor.UNDERLINE + "hub.spigotmc.org/javadocs/bukkit/org/bukkit/Material.html";
	private static final double PLAYER_COMMAND_DELAY_SEC = 1.0;

	private static List<String> commands;
	static {
		commands = new ArrayList<String>();
		commands.add("border");
		commands.add("settings");
		commands.add("spawnEquip");
		commands.add("spawn");
		commands.add("score");
		commands.add("start");
		commands.add("edit");
		commands.add("exit");
	}
	static HashSet<UUID> playersWithCommandTimeout = new HashSet<UUID>();

	private final Block signBlock;
	private final Block attachedBlock;
	private final String command;
	private final LinkedHashMap<String, String> parameterMap;
	private boolean failed = false;



	private static void setCommandTimeoutForPlayer(final Player player) {
		playersWithCommandTimeout.add(player.getUniqueId());
		AdventureArena.executeDelayed(PLAYER_COMMAND_DELAY_SEC, new Runnable() {
			@Override
			public void run() {
				playersWithCommandTimeout.remove(player.getUniqueId());
			}
		});
	}
	private static boolean isCommandTimeoutForPlayer(final Player player) {
		return playersWithCommandTimeout.contains(player.getUniqueId());
	}




	private AA_SignCommand(final Block signBlock, final Block attachedBlock, final String command, final LinkedHashMap<String, String> parameterMap) {
		this.signBlock = signBlock;
		this.attachedBlock = attachedBlock;
		this.command = command;
		this.parameterMap = parameterMap;
	}


	public boolean executeOnClick(final Player player, final Cancellable c) {
		if (!command.equals("start") && !command.equals("edit") && !command.equals("exit"))
			return false;
		if (isCommandTimeoutForPlayer(player)) return false;
		setCommandTimeoutForPlayer(player);
		AA_MiniGame miniGame = AA_MiniGameControl.getMiniGameContainingLocation(signBlock.getLocation());

		if (miniGame==null) {
			AA_MessageSystem.error("Can't find corresponding miniGame, inform admin!", player);
			AA_MessageSystem.consoleError("Can't find corresponding miniGame: " + command + " at " + signBlock.getLocation().toVector().toString());
			return false;
		}

		if (command.equals("start")) {
			String teamName = "default";
			if (parameterMap.containsKey("team")) {
				teamName = parameterMap.get("team");
			}
			AA_MiniGameControl.doChecksAndRegisterTeam(miniGame, teamName, attachedBlock.getLocation().add(0.5, 0, 0.5), 3.8);
			return true;
		}

		if (command.equals("edit")) {
			AA_MiniGameControl.startEditing(miniGame, player);
			return true;
		}

		if (command.equals("exit")) {
			AA_MiniGameControl.leaveMiniGame(player);
			return true;
		}


		return false;
	}



	public boolean executeOnBreak(final Player breaker, final Cancellable c) {
		if (!commands.contains(command))
			return false;

		AA_MiniGame miniGame = AA_MiniGameControl.getMiniGameContainingLocation(signBlock.getLocation());

		if (command.equals("start") || command.equals("edit")) {
			if (!breaker.isOp()) {
				failAndCancel(breaker, "Only Op can move the entrances", c);
				return false;
			}
		}
		if (command.equals("border")) {
			if (!breaker.isOp()) {
				failAndCancel(breaker, "Only Op can move the border", c);
				return false;
			}
		} else {
			if (miniGame==null) {
				int id = Integer.parseInt(parameterMap.get("id"));
				failAndCancel(breaker, "Can't find corresponding miniGame #" + id, c);
				return false;
			} else if (!miniGame.isEditableByPlayer(breaker)) {
				failAndCancel(breaker, "You are not allowed to modify this miniGame", c);
				return false;
			}
		}



		if (command.equals("border") && validateStringParamater(breaker, "corner", "nwl", "seu") && validateIntParamater(breaker, "id", 0, Integer.MAX_VALUE)) {
			int id = Integer.parseInt(parameterMap.get("id"));
			if (miniGame==null) {
				miniGame = AA_MiniGame.loadFromConfig(id);
			}
			if (miniGame==null) {
				failAndCancel(breaker, "Can't find corresponding miniGame #" + id, c);
				return false;
			}
			if (parameterMap.get("corner").equals("nwl")) {
				miniGame.setNorthWestMin(null);
			}
			if (parameterMap.get("corner").equals("seu")) {
				miniGame.setSouthEastMax(null);
			}
		}
		else if (command.equals("spawnEquip")) {
			//			List<ItemStack> spawnEquipToRemove = new ArrayList<ItemStack>();
			//			for (String itemName: parameterMap.keySet()) {
			//				try {
			//					Material mat = Material.valueOf(itemName.toUpperCase());
			//					if (validateIntParamater(breaker, itemName, 1, 64)) {
			//						spawnEquipToRemove.add(new ItemStack(mat, Integer.parseInt(parameterMap.get(itemName))));
			//					}
			//				} catch (IllegalArgumentException e) {}
			//			}
			miniGame.removeSpawnEquipBySignPos(signBlock.getLocation().toVector());
			AA_MessageSystem.success("(" + miniGame.getSpawnEquipDefinitions().size() + "x SpawnEquip left)", breaker);
		}
		else if (command.equals("spawn")) {
			String teamName;
			if (parameterMap.containsKey("team")) {
				teamName = parameterMap.get("team");
			} else {
				teamName = "default";
			}
			miniGame.removeSpawnPoint(teamName, attachedBlock.getLocation());
			AA_MessageSystem.success("(" + miniGame.getNumberOfSpawnPoints() + " SpawnPoints left)", breaker);
		}
		else if (command.equals("score")) {
			//TODO add score
		}


		if (miniGame!=null && miniGame.needsPersisting()) {
			miniGame.persist();
			AA_MessageSystem.success("Removed " + command + " from config for miniGame #" + miniGame.getID(), breaker);
			return true;
		}
		return false;
	}


	public boolean executeOnCreation(final Player creator) {
		if (!commands.contains(command)) {
			failAndBreak(creator, "Unknown command: " + highlightColor() + command + errorColor() + ", possible is: " + ultraHighlightColor() + commands.toString());
			return false;
		}

		AA_MiniGame miniGame = AA_MiniGameControl.getMiniGameContainingLocation(signBlock.getLocation());

		if (command.equals("start") || command.equals("edit")) {
			if (!creator.isOp()) {
				failAndBreak(creator, "Only Op can set the entrances");
				return false;
			}
		}
		if (command.equals("border")) {
			if (!creator.isOp()) {
				failAndBreak(creator, "Only Op can set the border");
				return false;
			}
		} else {
			if (miniGame==null) {
				failAndBreak(creator, "Command-sign is not inside valid borders");
				return false;
			} else if (!miniGame.isEditableByPlayer(creator)) {
				failAndBreak(creator, "You are not allowed to modify this miniGame");
				return false;
			}
		}



		if (command.equals("border") && validateStringParamater(creator, "corner", "nwl", "seu") & validateIntParamater(creator, "id", 0, Integer.MAX_VALUE)) {
			int id = Integer.parseInt(parameterMap.get("id"));
			if (miniGame==null) {
				miniGame = AA_MiniGameControl.getMiniGame(id);
			}
			if (miniGame==null) {
				miniGame = new AA_MiniGame(id, creator.getWorld());
				AA_MiniGameControl.addMiniGame(miniGame);
			}
			if (parameterMap.get("corner").equals("nwl")) {
				miniGame.setNorthWestMin(signBlock.getLocation());
			}
			if (parameterMap.get("corner").equals("seu")) {
				miniGame.setSouthEastMax(signBlock.getLocation());
			}
		}
		else if (command.equals("settings") && validateStringParamater(creator, "name")
				& validateIntParamater(creator, "pvpDamage", 0, 1)
				& validateStringParamater(creator, "scoreMode", "kd", "lms", "cmd") ) {
			miniGame.setName(parameterMap.get("name"));
			miniGame.setPvpDamage(!parameterMap.get("pvpDamage").equals("0"));
			if (parameterMap.get("scoreMode").equals("kd")) {
				miniGame.setScoreMode(ScoreMode.KillsPerDeath);
			} else if (parameterMap.get("scoreMode").equals("lms")) {
				miniGame.setScoreMode(ScoreMode.LastManStanding);
			} else if (parameterMap.get("scoreMode").equals("cmd")) {
				miniGame.setScoreMode(ScoreMode.ScoreByCommand);
			}
		}
		else if (command.equals("spawnEquip")) {
			//			List<ItemStack> newSpawnEquip = new ArrayList<ItemStack>();
			//			for (String itemName: parameterMap.keySet()) {
			//				try {
			//					Material mat = Material.valueOf(itemName.toUpperCase());
			//					if (validateIntParamater(creator, itemName, 1, 64)) {
			//						newSpawnEquip.add(new ItemStack(mat, Integer.parseInt(parameterMap.get(itemName))));
			//					}
			//				} catch (IllegalArgumentException e) {
			//					failAndBreak(creator, "Unknown item ID: " + highlightColor() + itemName + errorColor() + ", see all IDs here: "+ MATERIAL_IDS_HELP);
			//					return false;
			//				}
			//			}
			//			miniGame.addSpawnEquip(newSpawnEquip);

			if (parameterMap.size()==0) {
				failAndBreak(creator, "No item specified");
			}

			int lineIndex = 0;

			Material itemMaterial = null;
			Material targetMaterial = null;
			int amount = 0;
			Enchantment ench = null;
			int enchLevel = 0;

			for (Entry<String, String> entry: parameterMap.entrySet()) {
				if (lineIndex==0) {
					String itemName = entry.getKey();
					try {
						itemMaterial = Material.valueOf(itemName.toUpperCase());
						if (validateIntParamater(creator, itemName, 1, 64)) {
							amount = Integer.parseInt(entry.getValue());
						}
					} catch (IllegalArgumentException e) {
						failAndBreak(creator, "1st line needs to be an " + ultraHighlightColor() + "ItemId:amount" + errorColor() + "pair");
						failAndBreak(creator, "Unknown item ID: " + highlightColor() + itemName + errorColor() + ", see all IDs here: " +MATERIAL_IDS_HELP);
					}
				}
				if (entry.getKey().equals("for")) {
					String itemName = entry.getValue();
					try {
						targetMaterial = Material.valueOf(itemName.toUpperCase());
					} catch (IllegalArgumentException e) {
						failAndBreak(creator, "Unknown item ID: " + highlightColor() + itemName + errorColor() + ", see all IDs here: " +MATERIAL_IDS_HELP);
					}
				}
				if (entry.getKey().equals("ench")) {
					String[] enchVal = entry.getValue().split(" ");
					if (enchVal.length!=2) {
						failAndBreak(creator, "Enchant line must be: " + ultraHighlightColor() + "ench:EnchId lvl" + errorColor() + ", see all IDs here: " +ENCHANT_IDS_HELP);
					} else {
						String enchName = enchVal[0];
						ench = Enchantment.getByName(enchName.toUpperCase());
						if (ench==null) {
							failAndBreak(creator, "Unknown enchant ID: " + highlightColor() + enchName + errorColor() + ", see all IDs here: " +ENCHANT_IDS_HELP);
						}
						try {
							enchLevel = Integer.parseInt(enchVal[1]);
						} catch (NumberFormatException e1) {
							failAndBreak(creator, "Enchant lvl must be a number: " + ultraHighlightColor() + "ench:EnchId lvl");
						}
					}
				}
				lineIndex++;
			}

			if (failed) {
				AA_MessageSystem.example("  [spawnEquip]", creator);
				AA_MessageSystem.example(" dia_pickaxe:1", creator);
				AA_MessageSystem.example(" for:redstone_b", creator);
				AA_MessageSystem.example(" ench:dig_speed 5", creator);
				AA_MessageSystem.sideNote("(dia_ is short for diamond_)", creator);
				AA_MessageSystem.sideNote("(_b is short for _block)", creator);
				return false;
			} else {
				miniGame.addSpawnEquipDefinition(new AA_SpawnEquip(signBlock.getLocation().toVector(), itemMaterial, targetMaterial, amount, ench, enchLevel));
			}

		}
		else if (command.equals("spawn")) {
			String teamName;
			if (parameterMap.containsKey("team")) {
				teamName = parameterMap.get("team");
			} else {
				teamName = "default";
			}
			miniGame.addSpawnPoint(teamName, attachedBlock.getLocation());
		}
		else if (command.equals("score")) {
			//TODO add score
		}


		if (miniGame!=null && miniGame.needsPersisting()) {
			miniGame.persist();
			AA_MessageSystem.success("Added " + command + " to config for miniGame #" + miniGame.getID(), creator);
			return true;
		}
		return false;
	}



	private boolean validateStringParamater(final Player executor, final String param, final String... possibleValues) {
		if (!parameterMap.containsKey(param)) {
			failAndBreak(executor, "Missing parameter " + highlightColor() + param + errorColor() + " with value: " + ultraHighlightColor() + Arrays.toString(possibleValues));
			return false;
		}
		if (possibleValues.length>0) {
			if (!Arrays.asList(possibleValues).contains(parameterMap.get(param))) {
				failAndBreak(executor, "Wrong value for " + highlightColor() + param + errorColor() + ", possible is: " + ultraHighlightColor() + Arrays.toString(possibleValues));
				return false;
			}
		} else {
			if (parameterMap.get(param).equals("")) {
				failAndBreak(executor, "Missing value for " + highlightColor() + param + errorColor() + ", possible is: " + ultraHighlightColor() + Arrays.toString(possibleValues));
				return false;
			}
		}
		return true;
	}
	private boolean validateIntParamater(final Player executor, final String param, final int min, final int max) {
		String rangeHint = " a number from " + ultraHighlightColor() + min + errorColor() + " to " + ultraHighlightColor() + max;
		if (!parameterMap.containsKey(param)) {
			failAndBreak(executor, "Missing parameter " + highlightColor() + param + errorColor() + " with" + rangeHint);
			return false;
		}
		try {
			int value = Integer.parseInt(parameterMap.get(param));
			if (value<min || value>max) {
				failAndBreak(executor, "Value behind " + highlightColor() + param + errorColor() + " must be" + rangeHint);
				return false;
			}
		} catch (NumberFormatException e) {
			failAndBreak(executor, "Value behind " + highlightColor() + param + errorColor() + " must be" + rangeHint);
			return false;
		}
		return true;
	}




	private void failAndBreak(final Player executor, final String message) {
		failed = true;
		AA_MessageSystem.error(message, executor);
		signBlock.breakNaturally();
	}
	private void failAndCancel(final Player executor, final String message, final Cancellable c) {
		failed = true;
		AA_MessageSystem.error(message, executor);
		c.setCancelled(true);
	}
	private static String errorColor() {
		return AA_MessageSystem.getErrorColor();
	}

	private static String highlightColor() {
		return ChatColor.ITALIC.toString();
	}

	private static String ultraHighlightColor() {
		return ChatColor.ITALIC.toString() + ChatColor.BLUE;
	}




	@Override
	public String toString() {
		return command + ", params: " + parameterMap + " attachedTo: " + attachedBlock.getType();
	}

	public static AA_SignCommand createFrom(final Block signBlock) {
		return createFrom(signBlock, null, null);
	}

	public static AA_SignCommand createFrom(final Block signBlock, String[] lines, final SignChangeEvent optionalStylingLoopback) {
		if (signBlock !=null && signBlock.getState() instanceof Sign) {
			Sign signState =  (Sign) signBlock.getState();
			if (lines==null) {
				lines = signState.getLines();
			}
			org.bukkit.material.Sign signData = (org.bukkit.material.Sign) signState.getData();
			Block attachedBlock = signBlock.getRelative(signData.getAttachedFace());

			String command = null;
			LinkedHashMap<String, String> parameterMap = null;

			for (int i = 0; i<lines.length; i++) {
				String line = ChatColor.stripColor(lines[i]);
				if (command != null) {
					String[] keyVal = line.split("(:)|(: )|( : )|(=)|(= )|( = )", 2);
					if (keyVal.length == 2) {
						parameterMap.put(resolveAbbreviations(keyVal[0]), resolveAbbreviations(keyVal[1]));
					} else {
						if (line.length()>0 && optionalStylingLoopback != null) {
							AA_MessageSystem.error("Invalid parameter line (" +line+ "), format is" + ultraHighlightColor() + " key:value" , optionalStylingLoopback.getPlayer());
						}
					}
				}
				if (line.startsWith("[") && line.endsWith("]")) {
					command = line.substring(1, line.length()-1);
					if (optionalStylingLoopback != null) {
						optionalStylingLoopback.setLine(i, "[" + ChatColor.BLUE + command + ChatColor.BLACK + "]");
					}
					parameterMap = new LinkedHashMap<String, String>();
				}
			}
			return command == null ? null : new AA_SignCommand(signBlock, attachedBlock, command, parameterMap);

		} else return null;
	}


	private static String resolveAbbreviations(String string) {
		if (string.startsWith("dia_")) {
			//string.replace("dia_", "diamond_");
			string = "diamond_" + string.substring(4);
		}
		if (string.endsWith("_b")) {
			//string.replace("_b", "_block");
			string = string.substring(0,string.length()-2) + "_block";
		}
		return string;
	}




}
