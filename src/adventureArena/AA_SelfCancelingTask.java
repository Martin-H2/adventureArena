package adventureArena;

import org.bukkit.scheduler.BukkitRunnable;

public abstract class AA_SelfCancelingTask extends BukkitRunnable {

	private int numberOfExecutionsLeft;

	public void schedule(final double initialDelaySec, final double intervalDelaySec, final int numberOfExecutions) {
		if (numberOfExecutions < 1)
			throw new IllegalArgumentException("counter must be greater than 1");
		else {
			numberOfExecutionsLeft = numberOfExecutions;
		}
		runTaskTimer(AdventureArena.getInstance(), (long) (initialDelaySec*20), (long) (intervalDelaySec*20));
	}

	@Override
	public void run() {
		if (numberOfExecutionsLeft > 0) {
			tick();
			numberOfExecutionsLeft--;
		} else {
			cancel();
		}
	}

	public abstract void tick();


}