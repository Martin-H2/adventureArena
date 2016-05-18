package adventureArena;

import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;

public class AdventureArena extends JavaPlugin {

	private static AdventureArena	instance;
	public static WorldEditPlugin	wep;

	//	public static BountifulAPI		bfp;

	@Override
	public void onEnable() {
		super.onEnable();
		wep = (WorldEditPlugin) getServer().getPluginManager().getPlugin("WorldEdit");
		if (wep == null) {
			AA_MessageSystem.consoleError("can't find WorldEdit");
		}

		//		bfp = (BountifulAPI) getServer().getPluginManager().getPlugin("BountifulAPI"); //TODO what for ?
		//		if (bfp == null) {
		//			AA_MessageSystem.consoleError("can't find BountifulAPI");
		//		}


		ConfigurationSerialization.registerClass(AA_SpawnEquip.class);
		ConfigurationSerialization.registerClass(AA_BlockTrigger.class);
		instance = this;
		getServer().getPluginManager().registerEvents(new AA_Events(), this);
		new AA_Commands(this);
		AA_MiniGameControl.loadMiniGamesFromConfig();
		AA_MessageSystem.consoleInfo("loaded " + AA_MiniGameControl.getNumberOfMiniGames() + " miniGame configs.");
	}


	@Override
	public void onDisable() {
		for (Player p: Bukkit.getOnlinePlayers()) {
			if (AA_MiniGameControl.isInMiniGameHub(p)) {
				AA_MiniGameControl.kickFromMiniGameAndHub(p);
			}
		}
	}

	public static AdventureArena getInstance() {
		return instance;
	}

	public static void cancelTask(final int id) {
		instance.getServer().getScheduler().cancelTask(id);
	}

	public static int executeDelayed(final double delaySec, final Runnable runnable) {
		return instance.getServer().getScheduler().scheduleSyncDelayedTask(instance, runnable, (long) (delaySec * 20));
	}

	//	public static void executePeriodically(final int delaySec, final int count, final Runnable runnable) {
	//		instance.getServer().getScheduler().scheduleSyncRepeatingTask(instance, runnable, delaySec*20, delaySec*20);
	//	}

	public static void executePeriodically(final int delaySec, final Runnable runnable) {
		instance.getServer().getScheduler().scheduleSyncRepeatingTask(instance, runnable, delaySec * 20, delaySec * 20);
	}


	public static void simulateServerCommand(final String command) {
		Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
	}


	public static Player getOnlinePlayerStartingWith(final String playerNameStart) {
		Player fullNamePlayer = Bukkit.getPlayer(playerNameStart);
		if (fullNamePlayer != null) return fullNamePlayer;
		for (Player p: Bukkit.getOnlinePlayers()) {
			if (p.getName().startsWith(playerNameStart))
				return p;
		}
		return null;
	}


	//	public static File getWorldSpecificDataFolder(World world) { //TODO ? use world folder ?
	//		File folder = new File(world.getWorldFolder(), "MiniGameData");
	//		if (!folder.exists()) {
	//			folder.mkdirs();
	//		}
	//		return folder;
	//	}



}
