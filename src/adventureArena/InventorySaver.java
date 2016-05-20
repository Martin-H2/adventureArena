package adventureArena;

import java.util.Collection;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import adventureArena.enums.ConfigPaths;

public class InventorySaver {

	private static final boolean	KEEP_MINIGAME_LEVEL_AS_REWARD	= true;
	private static final double		MINIGAME_LEVEL_REWARD_FACT		= 0.1;

	public static boolean saveInventoryAndPlayerMeta(final Player player, final String library) {
		FileConfiguration config = ConfigAccess.getSavedInventoriesConfig();
		String playerDataRootPath = library + "." + player.getName();
		if (!config.contains(playerDataRootPath)) {
			PlayerInventory inventory = player.getInventory();

			config.set(playerDataRootPath + "." + ConfigPaths.health, player.getHealth());
			config.set(playerDataRootPath + "." + ConfigPaths.xp, player.getExp());
			config.set(playerDataRootPath + "." + ConfigPaths.level, player.getLevel());
			config.set(playerDataRootPath + "." + ConfigPaths.foodLevel, player.getFoodLevel());
			if (player.getBedSpawnLocation() != null) {
				config.set(playerDataRootPath + "." + ConfigPaths.bedSpawnLocationX, player.getBedSpawnLocation().getX());
				config.set(playerDataRootPath + "." + ConfigPaths.bedSpawnLocationY, player.getBedSpawnLocation().getY());
				config.set(playerDataRootPath + "." + ConfigPaths.bedSpawnLocationZ, player.getBedSpawnLocation().getZ());
			}

			int i = 0;
			for (ItemStack item: inventory.getContents()) {
				config.set(playerDataRootPath + "." + ConfigPaths.bag + "." + i, item);
				i++;
			}

			i = 0;
			for (ItemStack item: inventory.getArmorContents()) {
				config.set(playerDataRootPath + "." + ConfigPaths.armor + "." + i, item);
				i++;
			}

			config.set(playerDataRootPath + "." + ConfigPaths.activePotionEffects, player.getActivePotionEffects());

			player.setLevel(0);
			player.setExp(0);

			ConfigAccess.saveSavedInventoriesConfig();
			if (library.equals(ConfigPaths.savedPlayerData)) {
				MessageSystem.sideNote("Inventory, xp & buffs have been saved...", player);
			}
			return true;
		}
		else {
			if (library.equals(ConfigPaths.savedPlayerData)) {
				MessageSystem.error("Saved inventory already in config. Please contact admin!", player);
			}
			return false;
		}
	}


	public static boolean restoreInventoryAndPlayerMeta(final Player player, final String library) {
		FileConfiguration config = ConfigAccess.getSavedInventoriesConfig();
		String playerDataRootPath = library + "." + player.getName();

		if (config.contains(playerDataRootPath)) {
			PlayerInventory inventory = player.getInventory();

			player.setHealth(config.getDouble(playerDataRootPath + "." + ConfigPaths.health, player.getMaxHealth()));
			int arenaLevel = (int) Math.round(player.getLevel() * MINIGAME_LEVEL_REWARD_FACT);
			player.setLevel((KEEP_MINIGAME_LEVEL_AS_REWARD ? arenaLevel : 0) + config.getInt(playerDataRootPath + "." + ConfigPaths.level));
			player.setExp((float) config.getDouble(playerDataRootPath + "." + ConfigPaths.xp));
			player.setFoodLevel(config.getInt(playerDataRootPath + "." + ConfigPaths.foodLevel, 20));

			if (config.contains(playerDataRootPath + "." + ConfigPaths.bedSpawnLocationX)) {
				double x = config.getDouble(playerDataRootPath + "." + ConfigPaths.bedSpawnLocationX);
				double y = config.getDouble(playerDataRootPath + "." + ConfigPaths.bedSpawnLocationY);
				double z = config.getDouble(playerDataRootPath + "." + ConfigPaths.bedSpawnLocationZ);
				player.setBedSpawnLocation(new Location(player.getWorld(), x, y, z), true);
			}

			ItemStack[] items = inventory.getContents();
			for (int i = 0; i < items.length; i++) {
				items[i] = config.getItemStack(playerDataRootPath + "." + ConfigPaths.bag + "." + i, null);
			}
			inventory.setContents(items);

			ItemStack[] armor = inventory.getArmorContents();
			for (int i = 0; i < armor.length; i++) {
				armor[i] = config.getItemStack(playerDataRootPath + "." + ConfigPaths.armor + "." + i, null);
			}
			inventory.setArmorContents(armor);

			for (PotionEffect pe: player.getActivePotionEffects()) {
				player.removePotionEffect(pe.getType());
			}
			Object ape = config.get(playerDataRootPath + "." + ConfigPaths.activePotionEffects);
			if (ape instanceof Collection<?>) {
				for (Object pe: (Collection<?>) ape) {
					if (pe instanceof PotionEffect) {
						player.addPotionEffect((PotionEffect) pe);
					}
				}
			}

			config.set(playerDataRootPath, null);
			ConfigAccess.saveSavedInventoriesConfig();
			if (library.equals(ConfigPaths.savedPlayerData)) {
				MessageSystem.sideNote("Inventory, xp & buffs restored..." + (KEEP_MINIGAME_LEVEL_AS_REWARD && arenaLevel > 0 ? " (+" + arenaLevel + " level reward)" : ""), player);
			}
			return true;
		}
		else {
			if (library.equals(ConfigPaths.savedPlayerData)) {
				MessageSystem.error("Inventory was not saved before.", player);
			}
			return false;
		}
	}



}
