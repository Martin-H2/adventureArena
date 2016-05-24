package adventureArena;

import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import adventureArena.commands.JoinMghCommand;
import adventureArena.commands.LeaveMghCommand;
import adventureArena.commands.MgCommands;
import adventureArena.commands.ServerInfoCommand;
import adventureArena.control.HubControl;
import adventureArena.control.MiniGameLoading;
import adventureArena.messages.MessageSystem;
import adventureArena.miniGameComponents.MiniGameTrigger;
import adventureArena.miniGameComponents.SpawnEquip;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;

public class PluginManagement extends JavaPlugin {

	private static PluginManagement	instance;
	public static WorldEditPlugin	wep;

	//	public static BountifulAPI		bfp;

	@Override
	public void onEnable() {
		super.onEnable();
		instance = this;

		wep = (WorldEditPlugin) getServer().getPluginManager().getPlugin("WorldEdit");
		if (wep == null) {
			MessageSystem.consoleError("can't find WorldEdit");
		}

		ConfigurationSerialization.registerClass(SpawnEquip.class);
		ConfigurationSerialization.registerClass(MiniGameTrigger.class);

		getServer().getPluginManager().registerEvents(new Events(), this);

		new JoinMghCommand();
		new LeaveMghCommand();
		new MgCommands();
		new ServerInfoCommand();

		MiniGameLoading.loadMiniGamesFromConfig();
		MessageSystem.consoleInfo("loaded " + MiniGameLoading.getNumberOfMiniGames() + " miniGame configs.");
	}


	@Override
	public void onDisable() {
		for (Player p: Bukkit.getOnlinePlayers()) {
			if (HubControl.isInMiniGameHub(p)) { // important for consistency (non-persistent objects will be lost)
				HubControl.kickFromMiniGameAndHub(p);
			}
		}
	}

	public static PluginManagement getInstance() {
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

	public static int executePeriodically(final int delaySec, final Runnable runnable) {
		return instance.getServer().getScheduler().scheduleSyncRepeatingTask(instance, runnable, delaySec * 20, delaySec * 20);
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
