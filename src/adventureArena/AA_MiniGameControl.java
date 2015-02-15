package adventureArena;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;

public class AA_MiniGameControl {

	public static final GameMode MINIGAMES_GAMEMODE = GameMode.ADVENTURE;


	public static void joinMiniGameHub(final Player player, Location target) {
		FileConfiguration config = getConfig();
		if (target == null) {
			target = getMiniGameHubSpawn(player.getWorld());
		}
		if (target != null && AA_InventorySaver.saveInventoryAndPlayerMeta(player)) {
			config.set(AA_ConfigPaths.isInMiniGameHub + "." + player.getName(), true);
			config.set(AA_ConfigPaths.playerStates + "." + player.getName(), PlayerState.IS_WATCHING.toString());
			saveConfig();
			AA_MiniGameControl.setNeutralPlayerState(player);
			player.setGameMode(MINIGAMES_GAMEMODE);
			player.setBedSpawnLocation(target, true);
			teleportSafe(player, target);
		}
	}

	public static void leaveMiniGameHub(final Player player, Location target) {
		FileConfiguration config = getConfig();
		if (target == null) {
			target = player.getWorld().getSpawnLocation();
		}
		config.set(AA_ConfigPaths.isInMiniGameHub + "." + player.getName(), false);
		saveConfig();
		player.setGameMode(Bukkit.getDefaultGameMode());
		player.setBedSpawnLocation(target, true);
		teleportSafe(player, target);
		AA_InventorySaver.restoreInventoryAndPlayerMeta(player);
	}


	public static boolean isInMiniGameHub(final Player player) {
		FileConfiguration config = getConfig();
		return config.getBoolean( AA_ConfigPaths.isInMiniGameHub + "." + player.getName(), false);
	}




	private static void teleportSafe(final Player player, final Location target) {
		target.add(0.5, 0.0, 0.5);
		player.teleport(target);
	}




	public static void setNeutralPlayerState(final Player player) {
		PlayerInventory inventory = player.getInventory();
		player.setHealth(player.getMaxHealth());
		player.setFoodLevel(20);
		player.setSaturation(20);
		inventory.clear();
		inventory.setArmorContents(new ItemStack[inventory.getArmorContents().length]);
		for (PotionEffect pe: player.getActivePotionEffects()) {
			player.removePotionEffect(pe.getType());
		}
	}





	public static Location getMiniGameHubSpawn(final World world) {
		FileConfiguration config = getConfig();
		if(!config.contains(AA_ConfigPaths.miniGameHubSpawnX)) return null;
		double x = config.getDouble(AA_ConfigPaths.miniGameHubSpawnX);
		double y = config.getDouble(AA_ConfigPaths.miniGameHubSpawnY);
		double z = config.getDouble(AA_ConfigPaths.miniGameHubSpawnZ);
		return new Location(world, x, y, z);
	}




	private static void saveConfig() {
		AdventureArena.getInstance().saveConfig();
	}

	private static FileConfiguration getConfig() {
		return AdventureArena.getInstance().getConfig();
	}




}
