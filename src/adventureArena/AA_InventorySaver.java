package adventureArena;

import java.util.Collection;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;

public class AA_InventorySaver {


	private static final boolean KEEP_ARENA_XP_AS_REWARD = false;


	public static void saveInventoryAndPlayerMeta(final Player player){
		PlayerInventory inventory = player.getInventory();
		FileConfiguration config = AdventureArena.getInstance().getConfig();
		String path = "savedPlayerInventories." + player.getName();
		if(!config.contains(path)){
			config.set(path + ".health", player.getHealth());
			config.set(path + ".xp", player.getExp());
			config.set(path + ".level", player.getLevel());
			config.set(path + ".foodLevel", player.getFoodLevel());
			config.set(path + ".lastGameMode", player.getGameMode().toString());
			config.set(path + ".bedSpawnLocation.x", player.getBedSpawnLocation().getX());
			config.set(path + ".bedSpawnLocation.y", player.getBedSpawnLocation().getY());
			config.set(path + ".bedSpawnLocation.z", player.getBedSpawnLocation().getZ());

			int i = 0;
			for(ItemStack item : inventory.getContents()){
				config.set(path + ".bag." + i, item);
				i++;
			}

			i = 0;
			for(ItemStack item : inventory.getArmorContents()){
				config.set(path + ".armor." + i, item);
				i++;
			}

			config.set(path + ".activePotionEffects", player.getActivePotionEffects());

			player.setHealth(player.getMaxHealth());
			player.setLevel(0);
			player.setExp(0);
			player.setFoodLevel(20);
			inventory.clear();
			inventory.setArmorContents(new ItemStack[inventory.getArmorContents().length]);
			for (PotionEffect pe: player.getActivePotionEffects()) {
				player.removePotionEffect(pe.getType());
			}

			AdventureArena.getInstance().saveConfig();
			player.sendMessage(ChatColor.GREEN + "Your Inventory and XP has been saved...");
		}
	}


	public static void restoreInventoryAndPlayerMeta(final Player player){
		PlayerInventory inventory = player.getInventory();
		FileConfiguration config = AdventureArena.getInstance().getConfig();
		String path = "savedPlayerInventories." + player.getName();

		if(config.contains(path)){
			player.setHealth(config.getDouble(path + ".health", player.getMaxHealth()));
			player.setLevel((KEEP_ARENA_XP_AS_REWARD ? player.getLevel() : 0) + config.getInt(path + ".level"));
			player.setExp((float) config.getDouble(path + ".xp"));
			player.setFoodLevel(config.getInt(path + ".foodLevel", 20));
			player.setGameMode(GameMode.valueOf(config.getString(path + ".lastGameMode", "SURVIVAL")));

			double x = config.getDouble(path + ".bedSpawnLocation.x");
			double y = config.getDouble(path + ".bedSpawnLocation.y");
			double z = config.getDouble(path + ".bedSpawnLocation.z");
			player.setBedSpawnLocation(new Location(player.getWorld(), x, y, z), true);

			ItemStack[] items = inventory.getContents();
			for(int i=0; i<items.length; i++){
				items[i] = config.getItemStack(path + ".bag." + i, null);
			}
			inventory.setContents(items);

			ItemStack[] armor = inventory.getArmorContents();
			for(int i=0; i<armor.length; i++){
				armor[i] = config.getItemStack(path + ".armor." + i, null);
			}
			inventory.setArmorContents(armor);

			Object ape = config.get(path + ".activePotionEffects");
			if (ape instanceof Collection<?>) {
				for (PotionEffect pe: player.getActivePotionEffects()) {
					player.removePotionEffect(pe.getType());
				}
				for (Object pe: (Collection<?>)ape) {
					if (pe instanceof PotionEffect) {
						player.addPotionEffect((PotionEffect) pe);
					}
				}
			}

			config.set(path, null);
			AdventureArena.getInstance().saveConfig();
			player.sendMessage(ChatColor.GREEN + "Your Inventory and XP has been restored...");
		}
	}


}
