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
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.util.Vector;

public class AA_SignCommand {

	private static final String ENCHANT_IDS_HELP = ChatColor.BLUE.toString() + ChatColor.UNDERLINE + "hub.spigotmc.org/javadocs/bukkit/org/bukkit/enchantments/Enchantment.html";
	private static final String MATERIAL_IDS_HELP = ChatColor.BLUE.toString() + ChatColor.UNDERLINE + "hub.spigotmc.org/javadocs/bukkit/org/bukkit/Material.html";
	private static final String ENTITY_IDS_HELP = ChatColor.BLUE.toString() + ChatColor.UNDERLINE + "hub.spigotmc.org/javadocs/bukkit/org/bukkit/entity/EntityType.html";
	private static final double PLAYER_COMMAND_DELAY_SEC = 1.0;
	private static final String DELIM_MINUS = "(-)|(- )|( - )|( -)";
	private static final String DELIM_KOMMA = "(,)|(, )|( , )|( ,)";
	private static final String DELIM_ASSIGN = "(:)|(: )|( : )|(=)|(= )|( = )";

	private static List<String> commands;
	static {
		commands = new ArrayList<String>();
		commands.add("border");
		commands.add("settings");
		commands.add("spawnEquip");
		commands.add("spawn");
		commands.add("start");
		commands.add("edit");
		commands.add("exit");
		commands.add("highScore");
		commands.add("@start");
		commands.add("@distance");
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
			String teamName = AA_TeamManager.FFA_TEAM;
			if (parameterMap.containsKey("team")) {
				teamName = parameterMap.get("team");
			}
			AA_TeamManager.doChecksAndRegisterTeam(miniGame, teamName, attachedBlock.getLocation().add(0.5, 0, 0.5), 3.8);
			return true;
		}

		if (command.equals("edit")) {
			AA_MiniGameControl.startEditing(miniGame, player);
			return true;
		}

		if (command.equals("exit")) {
			AA_MiniGameControl.leaveCurrentMiniGame(player, false);
			return true;
		}


		return false;
	}



	public boolean executeOnBreak(final Player breaker, final Cancellable c, final boolean isEditmode) {
		if (!isEditmode) {
			if (isClickCommand()) {
				c.setCancelled(true);
			}
			return false;
		}
		if (!commands.contains(command))
			return false;

		AA_MiniGame miniGame = AA_MiniGameControl.getMiniGameContainingLocation(signBlock.getLocation());

		if (!breaker.isOp() && isOpOnlyCommand()) {
			failAndCancel(breaker, "Only Op can remove " + highlightColor() + command, c);
			return false;
		}

		if (miniGame==null && isSurroundingMiniGameRequired()) {
			failAndCancel(breaker, "Command-sign is not inside miniGame borders", c);
			return false;
		}

		if (miniGame!=null && !miniGame.isEditableByPlayer(breaker)) {
			failAndCancel(breaker, "You are not allowed to modify this miniGame", c);
			return false;
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
			miniGame.removeSpawnEquipBySignPos(signBlock.getLocation().toVector());
			AA_MessageSystem.success("(" + miniGame.getSpawnEquipDefinitions().size() + "x SpawnEquip left)", breaker);
		}
		else if (command.equals("@distance")) {
			miniGame.removeMonsterTriggerBySignPos(signBlock.getLocation().toVector());
		}
		else if (command.equals("spawn")) {
			String teamName;
			if (parameterMap.containsKey("team")) {
				teamName = parameterMap.get("team");
			} else {
				teamName = AA_TeamManager.FFA_TEAM;
			}
			miniGame.removeSpawnPoint(teamName, attachedBlock.getLocation());
			AA_MessageSystem.success("(" + miniGame.getNumberOfSpawnPoints() + " SpawnPoints left)", breaker);
		}
		else if (command.equals("highScore")) {
			if (miniGame==null) {
				int id = Integer.parseInt(parameterMap.get("id"));
				miniGame = AA_MiniGameControl.getMiniGame(id);
				if (miniGame==null)
					return false;
			}
			miniGame.unRegisterHighScoreSignLocation(signBlock.getLocation());
			miniGame.persist();
		}


		if (miniGame!=null && miniGame.needsPersisting()) {
			miniGame.persist();
			AA_MessageSystem.success("Removed " + command + " from config for miniGame #" + miniGame.getID(), breaker);
			return true;
		}
		return false;
	}


	public boolean executeOnCreation(final Player optionalCreator, final World world) {
		if (!commands.contains(command)) {
			failAndBreak(optionalCreator, "Unknown command: " + highlightColor() + command + errorColor() + ", possible is: " + ultraHighlightColor() + commands.toString());
			return false;
		}

		AA_MiniGame miniGame = AA_MiniGameControl.getMiniGameContainingLocation(signBlock.getLocation());

		if (optionalCreator!=null && !optionalCreator.isOp() && isOpOnlyCommand()) {
			failAndBreak(optionalCreator, "Only Op can set " + highlightColor() + command);
			return false;
		}

		if (miniGame==null && isSurroundingMiniGameRequired()) {
			failAndBreak(optionalCreator, "Command-sign is not inside miniGame borders" + (optionalCreator==null?" @"+signBlock.getLocation().toVector():""));
			return false;
		}

		if (optionalCreator!=null && miniGame!=null && !miniGame.isEditableByPlayer(optionalCreator)) {
			failAndBreak(optionalCreator, "You are not allowed to modify this miniGame");
			return false;
		}

		if (miniGame!=null && command.equals("border")) {
			failAndBreak(optionalCreator, "MG overlap or 3rd border definition");
			return false;
		}



		if (command.equals("border") && validateStringParamater(optionalCreator, "corner", "nwl", "seu") & validateIntParamater(optionalCreator, "id", 0, Integer.MAX_VALUE)) {
			int id = Integer.parseInt(parameterMap.get("id"));
			if (miniGame==null) {
				miniGame = AA_MiniGameControl.getMiniGame(id);
			}
			if (miniGame==null) {
				miniGame = new AA_MiniGame(id, world);
				AA_MiniGameControl.addMiniGame(miniGame);
			}
			Vector signBlockVectorLoc = signBlock.getLocation().toVector();
			if (parameterMap.get("corner").equals("nwl")) {
				if(miniGame.getNorthWestMin()!=null) {
					failAndBreak(optionalCreator, "nwl border already exists.");
				} else {
					miniGame.setNorthWestMin(signBlockVectorLoc);
				}
			}
			if (parameterMap.get("corner").equals("seu")) {
				if(miniGame.getSouthEastMax()!=null) {
					failAndBreak(optionalCreator, "seu border already exists.");
				} else {
					miniGame.setSouthEastMax(signBlockVectorLoc);
				}
			}
		}
		else if (command.equals("highScore")) {
			if (miniGame==null) {
				if (validateIntParamater(optionalCreator, "id", 0, Integer.MAX_VALUE)) {
					int id = Integer.parseInt(parameterMap.get("id"));
					miniGame = AA_MiniGameControl.getMiniGame(id);
					if (miniGame==null) {
						failAndBreak(optionalCreator, "No miniGame found with id: " + id);
						return false;
					}
				} else {
					failAndBreak(optionalCreator, "You need an id, placing this outside a miniGame");
					return false;
				}
			}
			miniGame.registerHighScoreSignLocation(signBlock.getLocation());
			miniGame.persist();
		}
		else if (command.equals("settings") && validateStringParamater(optionalCreator, "name")
				& validateIntParamater(optionalCreator, "pvpDamage", 0, 1)
				& validateStringParamater(optionalCreator, "scoreMode", "kd", "lms", "cmd") ) {
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
			if (parameterMap.size()==0) {
				failAndBreak(optionalCreator, "No item specified");
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
						if (validateIntParamater(optionalCreator, itemName, 1, 64)) {
							amount = Integer.parseInt(entry.getValue());
						}
					} catch (IllegalArgumentException e) {
						failAndBreak(optionalCreator, "1st line needs to be an " + ultraHighlightColor() + "ItemId:amount" + errorColor() + "pair");
						failAndBreak(optionalCreator, "Unknown item ID: " + highlightColor() + itemName + errorColor() + ", see all IDs here: " +MATERIAL_IDS_HELP);
					}
				}
				if (entry.getKey().equals("for")) {
					String itemName = entry.getValue();
					try {
						targetMaterial = Material.valueOf(itemName.toUpperCase());
					} catch (IllegalArgumentException e) {
						failAndBreak(optionalCreator, "Unknown item ID: " + highlightColor() + itemName + errorColor() + ", see all IDs here: " +MATERIAL_IDS_HELP);
					}
				}
				if (entry.getKey().equals("ench")) {
					String[] enchVal = entry.getValue().split(" ");
					if (enchVal.length!=2) {
						failAndBreak(optionalCreator, "Enchant line must be: " + ultraHighlightColor() + "ench:EnchId lvl" + errorColor() + ", see all IDs here: " +ENCHANT_IDS_HELP);
					} else {
						String enchName = enchVal[0];
						ench = Enchantment.getByName(enchName.toUpperCase());
						if (ench==null) {
							failAndBreak(optionalCreator, "Unknown enchant ID: " + highlightColor() + enchName + errorColor() + ", see all IDs here: " +ENCHANT_IDS_HELP);
						}
						try {
							enchLevel = Integer.parseInt(enchVal[1]);
						} catch (NumberFormatException e1) {
							failAndBreak(optionalCreator, "Enchant lvl must be a number: " + ultraHighlightColor() + "ench:EnchId lvl");
						}
					}
				}
				lineIndex++;
			}

			if (failed) {
				if(optionalCreator!=null) {
					AA_MessageSystem.example("  [spawnEquip]", optionalCreator);
					AA_MessageSystem.example(" dia_pickaxe:1", optionalCreator);
					AA_MessageSystem.example(" for:redstone_b", optionalCreator);
					AA_MessageSystem.example(" ench:dig_speed 5", optionalCreator);
					AA_MessageSystem.sideNote("(dia_ is short for diamond_)", optionalCreator);
					AA_MessageSystem.sideNote("(_b is short for _block)", optionalCreator);
				}
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
				teamName = AA_TeamManager.FFA_TEAM;
			}
			miniGame.addSpawnPoint(teamName, attachedBlock.getLocation());
		}
		else if (command.equals("highScore")) {
			miniGame.registerHighScoreSignLocation(signBlock.getLocation());
		}
		else if (command.equals("@distance") || command.equals("@start")) {
			if (command.equals("@distance") && !parameterMap.containsKey("radius")) {
				failAndBreak(optionalCreator, "Missing radius. Example: " + ultraHighlightColor() + "[@distance:5]");
				return false;
			}
			try {
				double radius = 1000;
				if (command.equals("@distance")) {
					radius = Double.parseDouble(parameterMap.get("radius"));
					parameterMap.remove("radius");
				}
				double delay = 0;
				double delayRngRange = 0;
				boolean explode = false;
				int count = 1;
				int newScore = -1;
				if (parameterMap.containsKey("setScore")) {
					if (optionalCreator!=null && miniGame.getScoreMode()!=ScoreMode.ScoreByCommand) {
						optionalCreator.sendMessage(ChatColor.RED + "[WARNING] score set by sign is only displayed on highScore lists when scoreMode:cmd under [settings]");
					}
					if (!validateIntParamater(optionalCreator, "setScore", 0, 999)) return false;
					newScore = Integer.parseInt(parameterMap.get("setScore"));
					AA_MonsterTrigger mt = new AA_MonsterTrigger(signBlock.getLocation().toVector(), attachedBlock.getLocation().toVector(), command.equals("@start"), radius, newScore);
					miniGame.addMonsterTrigger(mt);
				} else {
					if (parameterMap.containsKey("delay")) {
						String delayAndRange = parameterMap.get("delay");
						String[] fromTo = delayAndRange.split(DELIM_MINUS);
						delay = Double.parseDouble(fromTo[0]);
						if(fromTo.length==2) {
							delayRngRange = Double.parseDouble(fromTo[1]) - delay;
						}
						parameterMap.remove("delay");
					}
					if (parameterMap.containsKey("explode")) {
						explode = !parameterMap.get("explode").equals("0");
						parameterMap.remove("explode");
					}
					if (parameterMap.containsKey("count")) {
						count = Integer.parseInt(parameterMap.get("count"));
						parameterMap.remove("count");
					}
					for (String monsterName: parameterMap.keySet()) {
						EntityType entityType = EntityType.valueOf(monsterName.toUpperCase());
						String[] hpLifeTime = parameterMap.get(monsterName).split(DELIM_KOMMA);
						double hp = Double.parseDouble(hpLifeTime[0]);
						double lifeTime = -1;
						if (hpLifeTime.length==2) {
							lifeTime = Double.parseDouble(hpLifeTime[1]);
						}
						AA_MonsterTrigger mt = new AA_MonsterTrigger(signBlock.getLocation().toVector(), attachedBlock.getLocation().toVector(), command.equals("@start"), radius, entityType);
						mt.setDelay(delay);
						mt.setDelayRndRange(delayRngRange);
						mt.setHp(hp);
						mt.setCount(count);
						mt.setLifeTime(lifeTime);
						mt.setExplodeOnDeath(explode);
						miniGame.addMonsterTrigger(mt);
					}
				}
			} catch (Exception e) {
				failAndBreak(optionalCreator, "Wrong format.");
				if(optionalCreator!=null) {
					AA_MessageSystem.example("  [" + (command.equals("@distance")?"@distance:5":"@start") + "]", optionalCreator);
					AA_MessageSystem.example(" delay:3-7   " + ChatColor.DARK_GRAY + "(optional)", optionalCreator);
					AA_MessageSystem.example(" zombie:100,10   (or setScore:75)", optionalCreator);
					AA_MessageSystem.example(" explode:1", optionalCreator);
					AA_MessageSystem.sideNote("where 100=hp and 10=lifeTime", optionalCreator);
					AA_MessageSystem.sideNote("Entity IDs here: " + ENTITY_IDS_HELP, optionalCreator);
				}
				e.printStackTrace();
				return false;
			}
		}


		if (miniGame!=null && miniGame.needsPersisting()) {
			miniGame.persist();
			if(optionalCreator!=null) {
				AA_MessageSystem.success("Added " + command + " to config for miniGame #" + miniGame.getID(), optionalCreator);
			} else {
				AA_MessageSystem.consoleDebug("Added " + command + " to config for miniGame #" + miniGame.getID() + " @" + signBlock.getLocation().toVector());
			}
			return true;
		}
		return false;
	}



	public boolean isSurroundingMiniGameRequired() {
		return !command.equals("border") && !command.equals("highScore");
	}
	public boolean isOpOnlyCommand() {
		return command.equals("start") || command.equals("edit") || command.equals("border");
	}
	public boolean isClickCommand() {
		return command.equals("start") || command.equals("edit") || command.equals("exit");
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
		if (executor!=null) {
			AA_MessageSystem.error(message, executor);
			signBlock.breakNaturally();
		} else {
			AA_MessageSystem.consoleError(message);
		}
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

			if(lines[0].startsWith(ChatColor.RED + "==")) {
				command = "highScore";
				parameterMap = new LinkedHashMap<String, String>();
			} else {
				for (int i = 0; i<lines.length; i++) {
					String line = ChatColor.stripColor(lines[i]);
					if (command != null) {
						String[] keyVal = line.split(DELIM_ASSIGN, 2);
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
							optionalStylingLoopback.setLine(i, getFormatedCommand(command));
						}
						parameterMap = new LinkedHashMap<String, String>();
						if (command.startsWith("@distance")) {
							String[] cmdRad = command.split(DELIM_ASSIGN, 2);
							if(cmdRad.length==2) {
								command = "@distance";
								parameterMap.put("radius", cmdRad[1]);
							}
						}
					}
				}
			}
			return command == null ? null : new AA_SignCommand(signBlock, attachedBlock, command, parameterMap);

		} else return null;
	}


	public static String getFormatedCommand(final String command) {
		return "[" + ChatColor.BLUE + command + ChatColor.RESET + "]";
	}

	public Block getAttachedBlock() {
		return attachedBlock;
	}

	public String getCommand() {
		return command;
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
