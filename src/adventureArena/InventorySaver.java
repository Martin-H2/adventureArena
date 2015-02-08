package adventureArena;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class InventorySaver {


	public static void saveInventoryAndPlayerMeta(final Player player){
		player.sendMessage("getTotalExperience: " + player.getTotalExperience());
		player.sendMessage("getExp: " + player.getExp());
		player.sendMessage("getLevel: " + player.getLevel());


		PlayerInventory inventory = player.getInventory();
		FileConfiguration config = AdventureArena.getInstance().getConfig();
		String path = "savedPlayerInventories." + player.getName();

		config.set(path + ".health", player.getHealth());
		config.set(path + ".xp", player.getExp());
		config.set(path + ".level", player.getLevel());
		config.set(path + ".foodLevel", player.getFoodLevel());

		int i = 0;
		for(ItemStack item : inventory.getContents()){
			config.set(path + ".contents." + i, item);
			i++;
		}

		i = 0;
		for(ItemStack item : inventory.getArmorContents()){
			config.set(path + ".armorContents." + i, item);
			i++;
		}

		//		config.set(path + ".maxstacksize", inventory.getMaxStackSize());
		//		config.set(path + ".inventorytitle", inventory.getTitle());
		//		config.set(path + ".inventorysize", inventory.getSize());
		//		config.set(path + ".inventoryholder", inventory.getHolder());

		player.setHealth(player.getMaxHealth());
		player.setLevel(0);
		player.setExp(0);
		player.setFoodLevel(20);
		inventory.clear();
		inventory.setArmorContents(new ItemStack[inventory.getArmorContents().length]);

		AdventureArena.getInstance().saveConfig();
	}


	public static void restoreInventoryAndPlayerMeta(final Player player){
		PlayerInventory inventory = player.getInventory();
		FileConfiguration config = AdventureArena.getInstance().getConfig();
		String path = "savedPlayerInventories." + player.getName();

		if(config.contains(path)){

			player.setHealth(config.getDouble(path + ".health", player.getMaxHealth()));
			player.setExp(config.getInt(path + ".xp"));
			player.setLevel(player.getLevel() + config.getInt(path + ".level"));
			player.setFoodLevel(config.getInt(path + ".foodLevel", 20));

			ItemStack[] items = inventory.getContents();
			for(int i=0; i<items.length; i++){
				items[i] = config.getItemStack(path + ".contents." + i, null);
			}
			inventory.setContents(items);

			ItemStack[] armor = inventory.getArmorContents();
			for(int i=0; i<armor.length; i++){
				armor[i] = config.getItemStack(path + ".armorContents." + i, null);
			}
			inventory.setArmorContents(armor);

		}
	}


}
