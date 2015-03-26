package adventureArena;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;

public class AA_InventorySaver {

	private static final boolean KEEP_MINIGAME_LEVEL_AS_REWARD = true;
	private static final double MINIGAME_LEVEL_REWARD_FACT = 0.1;
	private static final String CONFIG_NAME = "savedPlayerInventories.yml";
	private static File configFile = null;
	private static YamlConfiguration config = null;

	public static boolean saveInventoryAndPlayerMeta(final Player player, final String library){
		FileConfiguration config = getConfig();
		String playerDataRootPath = library + "." + player.getName();
		if(!config.contains(playerDataRootPath)){
			PlayerInventory inventory = player.getInventory();

			config.set(playerDataRootPath + "." + AA_ConfigPaths.health, player.getHealth());
			config.set(playerDataRootPath + "." + AA_ConfigPaths.xp, player.getExp());
			config.set(playerDataRootPath + "." + AA_ConfigPaths.level, player.getLevel());
			config.set(playerDataRootPath + "." + AA_ConfigPaths.foodLevel, player.getFoodLevel());
			if (player.getBedSpawnLocation()!=null) {
				config.set(playerDataRootPath + "." + AA_ConfigPaths.bedSpawnLocationX, player.getBedSpawnLocation().getX());
				config.set(playerDataRootPath + "." + AA_ConfigPaths.bedSpawnLocationY, player.getBedSpawnLocation().getY());
				config.set(playerDataRootPath + "." + AA_ConfigPaths.bedSpawnLocationZ, player.getBedSpawnLocation().getZ());
			}

			int i = 0;
			for(ItemStack item : inventory.getContents()){
				config.set(playerDataRootPath + "." + AA_ConfigPaths.bag + "." + i, item);
				i++;
			}

			i = 0;
			for(ItemStack item : inventory.getArmorContents()){
				config.set(playerDataRootPath + "." + AA_ConfigPaths.armor + "." + i, item);
				i++;
			}

			config.set(playerDataRootPath + "." + AA_ConfigPaths.activePotionEffects, player.getActivePotionEffects());

			player.setLevel(0);
			player.setExp(0);

			saveConfig();
			if (library.equals(AA_ConfigPaths.savedPlayerData)) {
				AA_MessageSystem.success("Inventory, xp & buffs have been saved...", player);
			}
			return true;
		} else {
			if (library.equals(AA_ConfigPaths.savedPlayerData)) {
				AA_MessageSystem.error("Saved inventory already in config. Please contact admin!", player);
			}
			return false;
		}
	}


	public static boolean restoreInventoryAndPlayerMeta(final Player player, final String library){
		FileConfiguration config = getConfig();
		String playerDataRootPath = library + "." + player.getName();

		if(config.contains(playerDataRootPath)){
			PlayerInventory inventory = player.getInventory();

			player.setHealth(config.getDouble(playerDataRootPath + "." + AA_ConfigPaths.health, player.getMaxHealth()));
			int arenaLevel = (int) Math.round(player.getLevel()*MINIGAME_LEVEL_REWARD_FACT);
			player.setLevel((KEEP_MINIGAME_LEVEL_AS_REWARD ? arenaLevel : 0) + config.getInt(playerDataRootPath + "." + AA_ConfigPaths.level));
			player.setExp((float) config.getDouble(playerDataRootPath + "." + AA_ConfigPaths.xp));
			player.setFoodLevel(config.getInt(playerDataRootPath + "." + AA_ConfigPaths.foodLevel, 20));

			if(config.contains(playerDataRootPath + "." + AA_ConfigPaths.bedSpawnLocationX)){
				double x = config.getDouble(playerDataRootPath + "." + AA_ConfigPaths.bedSpawnLocationX);
				double y = config.getDouble(playerDataRootPath + "." + AA_ConfigPaths.bedSpawnLocationY);
				double z = config.getDouble(playerDataRootPath + "." + AA_ConfigPaths.bedSpawnLocationZ);
				player.setBedSpawnLocation(new Location(player.getWorld(), x, y, z), true);
			}

			ItemStack[] items = inventory.getContents();
			for(int i=0; i<items.length; i++){
				items[i] = config.getItemStack(playerDataRootPath + "." + AA_ConfigPaths.bag + "." + i, null);
			}
			inventory.setContents(items);

			ItemStack[] armor = inventory.getArmorContents();
			for(int i=0; i<armor.length; i++){
				armor[i] = config.getItemStack(playerDataRootPath + "." + AA_ConfigPaths.armor + "." + i, null);
			}
			inventory.setArmorContents(armor);

			for (PotionEffect pe: player.getActivePotionEffects()) {
				player.removePotionEffect(pe.getType());
			}
			Object ape = config.get(playerDataRootPath + "." + AA_ConfigPaths.activePotionEffects);
			if (ape instanceof Collection<?>) {
				for (Object pe: (Collection<?>)ape) {
					if (pe instanceof PotionEffect) {
						player.addPotionEffect((PotionEffect) pe);
					}
				}
			}

			config.set(playerDataRootPath, null);
			saveConfig();
			if (library.equals(AA_ConfigPaths.savedPlayerData)) {
				AA_MessageSystem.success("Inventory, xp & buffs restored..." + (KEEP_MINIGAME_LEVEL_AS_REWARD && arenaLevel>0 ? " (+" + arenaLevel + " level reward)" : ""), player);
			}
			return true;
		} else {
			if (library.equals(AA_ConfigPaths.savedPlayerData)) {
				AA_MessageSystem.error("Inventory was not saved before.", player);
			}
			return false;
		}
	}


	private static FileConfiguration getConfig() {
		if (config==null) {
			File dataFolder = AdventureArena.getInstance().getDataFolder();
			if (!dataFolder.exists()) {
				dataFolder.mkdirs();
			}
			configFile = new File(dataFolder, CONFIG_NAME);
			config = YamlConfiguration.loadConfiguration(configFile);
		}
		return config;
	}


	private static void saveConfig() {
		if (config!=null) {
			try {
				config.save(configFile);
			} catch (IOException e) {
				AA_MessageSystem.consoleError(CONFIG_NAME + "cannot be overwritten or created");
				e.printStackTrace();
			}
		}
	}


}
