package adventureArena.tasks;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;
import adventureArena.AA_MiniGame;
import adventureArena.AA_SignCommand;
import adventureArena.AA_TerrainHelper;
import adventureArena.AdventureArena;


public class TimedBlockTask implements Runnable {

	private final World				world;
	private final Block				airBlockAboveAttachedBlock;
	private final Material			blockType;
	private final double			hp;
	private final boolean			explodeOnDeath;
	private final double			lifeTime;
	private final AA_MiniGame		miniGame;
	private final List<Integer>		runningTasks;
	private final int				count;
	private final ArrayList<Block>	spawnedBlocks;



	public TimedBlockTask(World w, Vector attachedBlockPosition, Material blockType, double hp, boolean explodeOnDeath, double lifeTime, AA_MiniGame mg, List<Integer> runningTasks, int count) {
		super();
		world = w;
		airBlockAboveAttachedBlock = AA_TerrainHelper.getAirBlockAboveGround(attachedBlockPosition.toLocation(w), true);
		this.blockType = blockType;
		this.hp = hp;
		this.explodeOnDeath = explodeOnDeath;
		this.lifeTime = lifeTime;
		miniGame = mg;
		this.runningTasks = runningTasks;
		this.count = count;
		spawnedBlocks = new ArrayList<Block>();
	}


	@Override
	public void run() {
		tryToSetTypeAndData(airBlockAboveAttachedBlock, blockType, hp);
		for (int i = 1; i <= count - 1; i++) {
			tryToSetTypeAndData(airBlockAboveAttachedBlock.getRelative(BlockFace.UP, i), blockType, hp);
		}

		if (lifeTime > 0) {
			Runnable explosionTimerTask = new Runnable() {

				@Override
				public void run() {
					for (Block b: spawnedBlocks) {
						b.setType(Material.AIR);
						if (explodeOnDeath) {
							world.createExplosion(b.getLocation(), 4);
						}
					}
				}
			};

			runningTasks.add(AdventureArena.executeDelayed(Math.round(lifeTime) + 1, explosionTimerTask));
		}
	}



	@SuppressWarnings ("deprecation")
	private void tryToSetTypeAndData(Block b, Material mat, double data) {
		if (!AA_SignCommand.isClickCommandSign(b) && !AA_TerrainHelper.isUndestroyableArenaBorder(b) && miniGame.isInsidePlayableBounds(b.getLocation())) {
			b.setType(mat);
			if (lifeTime > 0) {
				spawnedBlocks.add(b);
			}
			if (hp >= 0.0) {
				try {
					b.setData((byte) data);
				}
				catch (Exception e) {
				}
			}
		}
	}



}
