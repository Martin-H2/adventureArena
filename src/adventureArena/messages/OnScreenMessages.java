package adventureArena.messages;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import adventureArena.control.PlayerControl;
import adventureArena.miniGameComponents.MiniGame;
import adventureArena.score.HighScoreManager;
import com.connorlinfoot.actionbarapi.ActionBarAPI;
import com.connorlinfoot.titleapi.TitleAPI;


public class OnScreenMessages {

	public static void sendPvpHitMessage(Player victim, Player attacker, EntityDamageEvent event) {
		long dmg = Math.round(event.getFinalDamage());
		if (dmg > 0) {
			sendActionBarMessage(attacker, ChatColor.DARK_RED + victim.getName() + ":" + ChatColor.RED + "  -" + dmg
				+ "  (" + Math.round(victim.getHealth() - event.getFinalDamage()) + "/" + Math.round(victim.getMaxHealth()) + ")");
		}
		// ================= FOR DEBUGGING ==========================
		//		EntityDamageEvent le = victim.getLastDamageCause();
		//		String lastDmg = "?";
		//		if (le != null) {
		//			lastDmg = le.getEventName() + "." + le.getCause().toString();
		//		}
		//String attackerHitMessage = String.format(ChatColor.RED + "%s►%s►%s: %.2f (last:%s)",
		//		attacker == null ? "?" : attacker.getName(), event.getCause().toString(), victim.getName(), event.getFinalDamage(), lastDmg);
		//attacker.sendMessage(attackerHitMessage);
		//AA_MessageSystem.broadcast(attackerHitMessage);
	}


	public static void sendPvpKillMessages(Player killer, Player victim, double killerRating, double newKillerRating, double victimRating, double newVictimRating, MiniGame mg) {
		String killerMessage = ChatColor.DARK_GREEN + "You killed " + ChatColor.GREEN + victim.getName();
		String killerSub = ChatColor.GRAY + String.format("  (stealing %.0f rating)", newKillerRating - killerRating);
		String victimMessage = ChatColor.RED + killer.getName() + ChatColor.DARK_RED + " killed you";
		String othersMessage = ChatColor.DARK_RED + killer.getName() + ChatColor.RED + " killed " + ChatColor.DARK_RED + victim.getName();
		String spectatorsSub = String.format(ChatColor.DARK_GRAY + "%s: %.0f->%.0f   %s: %.0f->%.0f", killer.getName(), killerRating, newKillerRating, victim.getName(), victimRating, newVictimRating);

		sendSubtitle(killer, 0.5, 1.5, 1.0, killerMessage);
		sendActionBarMessage(killer, killerSub, 4.1);
		sendSubtitle(victim, 0.5, 1.5, 1.0, victimMessage);

		for (Player p: mg.getPlayersInArea()) {
			if (!p.equals(victim) && !p.equals(killer)) {
				if (PlayerControl.isPlayingMiniGame(p)) {
					sendSubtitle(p, 0.5, 1.5, 1.0, othersMessage);
				}
				else {
					sendTitleAndSubtitle(p, 0.5, 3.5, 1.0, othersMessage, spectatorsSub);
				}
			}
		}
	}


	public static void sendGameWonMessage(MiniGame mg, Player p) {
		sendTitleAndSubtitle(p, 0.5, 4.5, 1.0,
				ChatColor.GREEN + "You won '" + mg.getName() + "'",
				ChatColor.DARK_GREEN + String.format("Your new rating is %.0f", HighScoreManager.getHighScoreRating(mg, p)));
	}



	// ================ "ABSTRACTION LAYER" ================
	public static void sendActionBarMessage(Player player, String message, double durationSec) {
		//BountifulAPI.sendActionBar(player, message);
		//player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
		ActionBarAPI.sendActionBar(player, message, (int) (durationSec * 20.0));
	}

	public static void sendActionBarMessage(Player player, String message) {
		ActionBarAPI.sendActionBar(player, message);
	}

	public static void sendTitleAndSubtitle(Player player, double fadeInSec, double holdSec, double fadeOutSec, String title, String subTitle) {
		//BountifulAPI.sendTitle(player, (int) (fadeInSec * 20.0), (int) (holdSec * 20.0), (int) (fadeOutSec * 20.0), title, subTitle);
		TitleAPI.sendTitle(player, (int) (fadeInSec * 20.0), (int) (holdSec * 20.0), (int) (fadeOutSec * 20.0), title, subTitle);
	}

	public static void sendSubtitle(Player player, double fadeInSec, double holdSec, double fadeOutSec, String subTitle) {
		sendTitleAndSubtitle(player, fadeInSec, holdSec, fadeOutSec, " ", subTitle);
	}

	public static void sendTitle(Player player, double fadeInSec, double holdSec, double fadeOutSec, String title) {
		sendTitleAndSubtitle(player, fadeInSec, holdSec, fadeOutSec, title, " ");
	}



}
