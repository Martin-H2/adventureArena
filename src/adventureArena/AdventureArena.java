package adventureArena;

import org.bukkit.plugin.java.JavaPlugin;

public class AdventureArena extends JavaPlugin {

	@Override
	public void onEnable() {
		super.onEnable();
		getServer().getPluginManager().registerEvents(new AdventureEvents(), this);
	}

}
