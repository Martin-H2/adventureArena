package adventureArena;

import java.io.File;
import java.io.IOException;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitWorld;

@SuppressWarnings("deprecation")
public class AA_TerrainHelper {



	static Block getAirBlockAboveGround(final Location start, final boolean searchUpwards) {
		return getAirBlockAboveGround(start.getBlock(), searchUpwards);
	}
	static Block getAirBlockAboveGround(final Block start, final boolean searchUpwards) {
		Block block = start;
		BlockFace searchDirection = searchUpwards ? BlockFace.UP : BlockFace.DOWN;
		while(!isUndestroyableArenaBorder(block)) {
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
		return
				block.getType()==Material.BEDROCK ||
				block.getType()==Material.STAINED_GLASS;
	}










	// ##################### SCHEMATIC SAVING LOADING ###########################

	public static boolean saveMiniGameToSchematic(final Vector northWestMin, final Vector southEastMax, final int id, final World world) {
		// TODO save & load async ?
		File file = getMiniGameFile(id);
		com.sk89q.worldedit.Vector min = new com.sk89q.worldedit.Vector(northWestMin.getX(),northWestMin.getY(),northWestMin.getZ());
		com.sk89q.worldedit.Vector max = new com.sk89q.worldedit.Vector(southEastMax.getX(),southEastMax.getY(),southEastMax.getZ());
		EditSession es = WorldEdit.getInstance().getEditSessionFactory().getEditSession(new BukkitWorld(world), -1);

		CuboidClipboard cc = new CuboidClipboard(max.subtract(min).add(new com.sk89q.worldedit.Vector(1, 1, 1)), min);
		cc.copy(es);
		try {
			cc.saveSchematic(file);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public static boolean loadMinigameFromSchematic(final int id, final World world) {
		File file = getMiniGameFile(id);
		EditSession es = new EditSession(new BukkitWorld(world), -1);
		es.enableQueue();
		es.setFastMode(true);

		try {
			CuboidClipboard cc = CuboidClipboard.loadSchematic(file);
			cc.paste(es, cc.getOrigin(), false, true);
			//cc.place(es, null, true);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		es.flushQueue();
		return true;
	}










	private static File getMiniGameFile(final int id) {
		File miniGameSchemFolder = new File(AdventureArena.getInstance().getDataFolder(), "miniGameSchematics");
		if (!miniGameSchemFolder.exists()) {
			miniGameSchemFolder.mkdirs();
		}
		File mgf = new File(miniGameSchemFolder, File.separator + "miniGame_" + id + ".schematic");
		if (!mgf.exists()) {
			try {
				mgf.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return mgf;
	}



}
