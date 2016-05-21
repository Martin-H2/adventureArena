package adventureArena.tasks;

import org.bukkit.scheduler.BukkitRunnable;
import adventureArena.PluginManagement;

public abstract class AbstractPeriodicTask extends BukkitRunnable {

	private int numberOfExecutionsLeft;

	public void schedule(final double initialDelaySec, final double intervalDelaySec, final int numberOfExecutions) {
		if (numberOfExecutions < 1)
			throw new IllegalArgumentException("counter must be greater than 1");
		else {
			numberOfExecutionsLeft = numberOfExecutions;
		}
		runTaskTimer(PluginManagement.getInstance(), (long) (initialDelaySec*20), (long) (intervalDelaySec*20));
	}

	@Override
	public void run() {
		if (numberOfExecutionsLeft > 1) {
			numberOfExecutionsLeft--;
			tick();
		} else if (numberOfExecutionsLeft == 1) {
			numberOfExecutionsLeft--;
			lastTick();
		} else {
			cancel();
		}
	}

	public abstract void tick();
	public abstract void lastTick();

	public int getNumberOfExecutionsLeft() {
		return numberOfExecutionsLeft;
	}

}
