package adventureArena;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class AdventureArena extends JavaPlugin {

	private static AdventureArena instance;

	@Override
	public void onEnable() {
		super.onEnable();
		instance = this;
		getServer().getPluginManager().registerEvents(new AA_Events(), this);
		new AA_Commands(this);
	}


	public static AdventureArena getInstance() {
		return instance;
	}


	public static void broadcast(final Object o) {
		for (Player p: Bukkit.getOnlinePlayers()) {
			p.sendMessage(o!=null?o.toString():"null");
		}
	}

	public static void executeDelayed(final double delaySec, final Runnable runnable) {
		instance.getServer().getScheduler().scheduleSyncDelayedTask(instance, runnable, (long) (delaySec*20));
	}

	public static void executePeriodically(final int delaySec, final Runnable runnable) {
		instance.getServer().getScheduler().scheduleSyncRepeatingTask(instance, runnable, delaySec*20, delaySec*20);
	}


	public static Player getOnlinePlayerStartingWith(final String playerNameStart) {
		@SuppressWarnings("deprecation")
		Player fullNamePlayer = Bukkit.getPlayer(playerNameStart);
		if (fullNamePlayer!=null) return fullNamePlayer;
		for (Player p: Bukkit.getOnlinePlayers()) {
			if (p.getName().startsWith(playerNameStart))
				return p;
		}
		return null;
	}




}
