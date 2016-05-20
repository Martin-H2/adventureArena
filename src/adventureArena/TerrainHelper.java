package adventureArena;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.util.Vector;
import adventureArena.miniGameComponents.MiniGame;
import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitWorld;

@SuppressWarnings ("deprecation")
public class TerrainHelper {



	public static Location getAirBlockAboveGroundTelePos(final Location start, final boolean searchUpwards, MiniGame mg) {
		return getAirBlockAboveGround(start.getBlock(), searchUpwards, mg).getLocation().add(0.5, 0, 0.5);
	}

	public static Block getAirBlockAboveGround(final Location start, final boolean searchUpwards, MiniGame mg) {
		return getAirBlockAboveGround(start.getBlock(), searchUpwards, mg);
	}

	public static Block getAirBlockAboveGround(final Block start, final boolean searchUpwards, MiniGame mg) {
		BlockFace searchDirection = searchUpwards ? BlockFace.UP : BlockFace.DOWN;
		Block block = start.getRelative(searchDirection);
		while (mg.isInsidePlayableBounds(block.getLocation())) {
			if (isAirBlockAboveGround(block)) return block;
			block = block.getRelative(searchDirection);
		}
		return start;
	}

	static boolean isAirBlockAboveGround(final Block block) {
		return !isSolid(block) && isSolid(block.getRelative(BlockFace.DOWN));
	}



	public static boolean isSolid(final Block testBlock) {
		return testBlock.getType().isSolid();
	}

	public static boolean isUndestroyableArenaBorder(final Block block) {
		return block.getType() == Material.BEDROCK ||
			block.getType() == Material.BARRIER ||
			block.getType() == Material.STAINED_GLASS;
	}



	public static List<Block> getAttachedSigns(final Block block) {
		List<Block> attachedSigns = new ArrayList<Block>();
		if (isAttachedSign(block.getRelative(BlockFace.UP), BlockFace.UP)) {
			attachedSigns.add(block.getRelative(BlockFace.UP));
		}
		if (isAttachedSign(block.getRelative(BlockFace.NORTH), BlockFace.NORTH)) {
			attachedSigns.add(block.getRelative(BlockFace.NORTH));
		}
		if (isAttachedSign(block.getRelative(BlockFace.SOUTH), BlockFace.SOUTH)) {
			attachedSigns.add(block.getRelative(BlockFace.SOUTH));
		}
		if (isAttachedSign(block.getRelative(BlockFace.EAST), BlockFace.EAST)) {
			attachedSigns.add(block.getRelative(BlockFace.EAST));
		}
		if (isAttachedSign(block.getRelative(BlockFace.WEST), BlockFace.WEST)) {
			attachedSigns.add(block.getRelative(BlockFace.WEST));
		}
		return attachedSigns;
	}

	private static boolean isAttachedSign(final Block signBlock, final BlockFace searchDirection) {
		if (signBlock != null && signBlock.getState() instanceof Sign) {
			Sign signState = (Sign) signBlock.getState();
			org.bukkit.material.Sign signData = (org.bukkit.material.Sign) signState.getData();
			return signData.getAttachedFace().getOppositeFace() == searchDirection;
		}
		else return false;
	}



	// ##################### SCHEMATIC SAVING LOADING ###########################

	public static boolean saveMiniGameToSchematic(final Vector northWestMin, final Vector southEastMax, final int id, final World world) {
		//TODO ? save & load async
		File file = getMiniGameSchematicFile(id);
		int specRoomHeight = 2; //TODO ? dynamic spec room height
		com.sk89q.worldedit.Vector min = new com.sk89q.worldedit.Vector(northWestMin.getX(), northWestMin.getY(), northWestMin.getZ());
		com.sk89q.worldedit.Vector max = new com.sk89q.worldedit.Vector(southEastMax.getX(), southEastMax.getY() - specRoomHeight, southEastMax.getZ());
		EditSession es = WorldEdit.getInstance().getEditSessionFactory().getEditSession(new BukkitWorld(world), -1);

		CuboidClipboard cc = new CuboidClipboard(max.subtract(min).add(new com.sk89q.worldedit.Vector(1, 1, 1)), min);
		cc.copy(es);
		try {
			cc.saveSchematic(file);
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public static boolean loadMinigameFromSchematic(final int id, final World world) {
		File file = getMiniGameSchematicFile(id);
		EditSession es = new EditSession(new BukkitWorld(world), -1);
		es.enableQueue();
		es.setFastMode(true);

		try {
			CuboidClipboard cc = CuboidClipboard.loadSchematic(file);
			cc.paste(es, cc.getOrigin(), false, true);
			//cc.place(es, null, true);
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		es.flushQueue();
		return true;
	}



	private static File getMiniGameSchematicFile(final int id) {
		File miniGameSchemFolder = new File(AdventureArena.getInstance().getDataFolder(), "miniGameSchematics");
		if (!miniGameSchemFolder.exists()) {
			miniGameSchemFolder.mkdirs();
		}
		File mgf = new File(miniGameSchemFolder, File.separator + "miniGame_" + id + ".schematic");
		if (!mgf.exists()) {
			try {
				mgf.createNewFile();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		return mgf;
	}

	public static void resetMiniGameRoom(MiniGame mg) {
		Block block;
		World world = mg.getWorld();
		for (int x = mg.getNorthWestMin().getBlockX(); x <= mg.getSouthEastMax().getBlockX(); x++) {
			for (int y = mg.getNorthWestMin().getBlockY(); y <= mg.getSouthEastMax().getBlockY() - 3; y++) {
				for (int z = mg.getNorthWestMin().getBlockZ(); z <= mg.getSouthEastMax().getBlockZ(); z++) {
					block = world.getBlockAt(x, y, z);
					if (!AA_SignCommand.isBorderCommandSign(block) && !isUndestroyableArenaBorder(block)
						&& mg.isInsideBounds(block.getLocation())) {
						//replace
						if (y == mg.getNorthWestMin().getBlockY()) {
							block.setType(Material.STATIONARY_LAVA);
						}
						else {
							block.setType(Material.AIR);
						}
					}
				}
			}
		}
	}

	public static void fixSigns(MiniGame mg) {
		Block block;
		World world = mg.getWorld();
		for (int x = mg.getNorthWestMin().getBlockX(); x <= mg.getSouthEastMax().getBlockX(); x++) {
			for (int y = mg.getNorthWestMin().getBlockY(); y <= mg.getSouthEastMax().getBlockY(); y++) {
				for (int z = mg.getNorthWestMin().getBlockZ(); z <= mg.getSouthEastMax().getBlockZ(); z++) {
					block = world.getBlockAt(x, y, z);
					if (block.getState() instanceof Sign) {
						Sign signState = (Sign) block.getState();
						String[] lines = signState.getLines();
						for (int i = 0; i < lines.length; i++) {
							if ("\"\"".equals(lines[i])) {
								signState.setLine(i, "");
							}
						}
						signState.update();
					}
				}
			}
		}
	}



}
