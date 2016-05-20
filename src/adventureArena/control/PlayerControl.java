package adventureArena.control;

import java.util.Random;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;
import adventureArena.ConfigAccess;
import adventureArena.enums.ConfigPaths;
import adventureArena.enums.PlayerState;
import adventureArena.miniGameComponents.MiniGame;


public class PlayerControl {

	private static Random	rnd	= new Random();


	public static void clearInventoryAndBuffs(final Player player) {
		PlayerInventory inventory = player.getInventory();
		player.setHealth(player.getMaxHealth());
		player.setFireTicks(0);
		player.setFoodLevel(20);
		player.setSaturation(20);
		inventory.clear();
		inventory.setArmorContents(new ItemStack[inventory.getArmorContents().length]);
		for (PotionEffect pe: player.getActivePotionEffects()) {
			player.removePotionEffect(pe.getType());
		}
		player.updateInventory();
	}


	public static void teleportSafe(final Player player, final Block target, Vector viewTarget) {
		player.teleport(target.getLocation().add(0.5, 0.0, 0.5).setDirection(viewTarget));
	}

	/**
	 * Prevents player stacking
	 *
	 * @param player
	 * @param target
	 * @param bounce
	 */
	public static void teleportSafeAndShove(final Player player, Location target, boolean bounce) {
		target = target.clone();
		target.add(0.5, 0.0, 0.5);
		player.teleport(target);
		if (bounce) {
			player.setVelocity(new Vector(rnd.nextDouble() * 2.0 - 1.0, 0, rnd.nextDouble() * 2.0 - 1.0));
		}
	}

	public static boolean isPlayerInsideHisEditableArea(final Player player) {
		MiniGame mg = MiniGameLoading.getMiniGameContainingLocation(player.getLocation());
		return mg == null ? false : mg.isEditableByPlayer(player);
	}


	public static boolean isPlayingMiniGame(final Player player) {
		return PlayerState.IS_PLAYING.toString().equals(ConfigAccess.getPluginConfig().getString(ConfigPaths.playerStates + "." + player.getName()));
	}

	public static boolean isEditingMiniGame(final Player player) {
		return PlayerState.IS_EDITING.toString().equals(ConfigAccess.getPluginConfig().getString(ConfigPaths.playerStates + "." + player.getName()));
	}

	public static boolean isWatchingMiniGames(final Player player) {
		return PlayerState.IS_WATCHING.toString().equals(ConfigAccess.getPluginConfig().getString(ConfigPaths.playerStates + "." + player.getName()));
	}


	static void setPlayerState(final Player p, final PlayerState playerState, final MiniGame optionalMiniGame) {
		ConfigAccess.getPluginConfig().set(ConfigPaths.playerStates + "." + p.getName(), playerState.toString());
		if (optionalMiniGame != null) {
			if (playerState == PlayerState.IS_EDITING) {
				optionalMiniGame.setLockedByEditSession(true);
				ConfigAccess.getPluginConfig().set(ConfigPaths.activeMinigameId + "." + p.getName(), optionalMiniGame.getID());
			}
			else if (playerState == PlayerState.IS_PLAYING) {
				optionalMiniGame.setInProgress(true);
				ConfigAccess.getPluginConfig().set(ConfigPaths.activeMinigameId + "." + p.getName(), optionalMiniGame.getID());
			}
		}
		if (playerState == PlayerState.IS_WATCHING) {
			ConfigAccess.getPluginConfig().set(ConfigPaths.activeMinigameId + "." + p.getName(), null);
		}
		ConfigAccess.savePluginConfig();
	}

}
