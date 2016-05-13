package adventureArena;

import me.dpohvar.powernbt.api.NBTCompound;
import me.dpohvar.powernbt.api.NBTManager;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class AA_ItemHelper {

	// nbt = {display:{Lore:["lore1"]}}

	//
	//	@SuppressWarnings("deprecation")
	//	public static ItemStack createAdventureToolFor(final Material tool, final Material destroyable) {
	//		ItemStack item = new ItemStack(tool);
	//		NBTCompound nbt = new NBTCompound();
	//		nbt.list("CanDestroy").add(destroyable.getId());
	//		nbt.compound("display").list("Lore").add(ChatColor.GOLD + "Can destroy " + destroyable.toString());
	//		NBTManager.getInstance().write(item, nbt);
	//		return item;
	//	}
	//
	//	public static ItemStack createAdventureToolFor(final Material tool, final Material destroyable, final Enchantment ench, final int level) {
	//		ItemStack item = createAdventureToolFor(tool, destroyable);
	//		item.addEnchantment(ench, level);
	//		return item;
	//	}
	//
	//



	@SuppressWarnings ("deprecation")
	public static ItemStack createAdventureItem(final Material itemMaterial, int amount, final Material targetMaterial) {
		if (amount > itemMaterial.getMaxStackSize()) {
			amount = itemMaterial.getMaxStackSize();
		}
		String nbtFlag;
		if (itemMaterial.isBlock()) {
			nbtFlag = "CanPlaceOn";
		}
		else {
			nbtFlag = "CanDestroy";
		}
		ItemStack itemStack = new ItemStack(itemMaterial, amount);
		if (targetMaterial != null) {
			NBTCompound nbt = new NBTCompound();
			nbt.list(nbtFlag).add(String.valueOf(targetMaterial.getId()));
			//nbt.compound("display").list("Lore").add(ChatColor.GOLD + nbtFlag + ": " + targetable.toString());
			NBTManager.getInstance().write(itemStack, nbt);
		}
		return itemStack;
	}

	public static ItemStack createAdventureItem(final Material itemMaterial, final int amount, final Material targetMaterial, final Enchantment ench, int level) {
		ItemStack itemStack = createAdventureItem(itemMaterial, amount, targetMaterial);
		if (ench != null && ench.canEnchantItem(itemStack)) {
			if (level > ench.getMaxLevel()) {
				level = ench.getMaxLevel();
			}
			itemStack.addEnchantment(ench, level);
		}
		return itemStack;
	}

	public static boolean isHelmet(final ItemStack item) {
		return item.getType() == Material.LEATHER_HELMET ||
			item.getType() == Material.IRON_HELMET ||
			item.getType() == Material.CHAINMAIL_HELMET ||
			item.getType() == Material.SKULL_ITEM ||
			item.getType() == Material.PUMPKIN ||
			item.getType() == Material.DIAMOND_HELMET ||
			item.getType() == Material.GOLD_HELMET;
	}

	public static boolean isChestplate(final ItemStack item) {
		return item.getType() == Material.LEATHER_CHESTPLATE ||
			item.getType() == Material.IRON_CHESTPLATE ||
			item.getType() == Material.CHAINMAIL_CHESTPLATE ||
			item.getType() == Material.DIAMOND_CHESTPLATE ||
			item.getType() == Material.GOLD_CHESTPLATE;
	}

	public static boolean isLeggings(final ItemStack item) {
		return item.getType() == Material.LEATHER_LEGGINGS ||
			item.getType() == Material.IRON_LEGGINGS ||
			item.getType() == Material.CHAINMAIL_LEGGINGS ||
			item.getType() == Material.DIAMOND_LEGGINGS ||
			item.getType() == Material.GOLD_LEGGINGS;
	}

	public static boolean isBoots(final ItemStack item) {
		return item.getType() == Material.LEATHER_BOOTS ||
			item.getType() == Material.IRON_BOOTS ||
			item.getType() == Material.CHAINMAIL_BOOTS ||
			item.getType() == Material.DIAMOND_BOOTS ||
			item.getType() == Material.GOLD_BOOTS;
	}

	public static void addItemSmart(final Player p, final ItemStack item) {
		PlayerInventory inv = p.getInventory();
		if (inv.getItemInOffHand() == null && item.getType() == Material.SHIELD) {
			inv.setItemInOffHand(item);
		}
		if (inv.getHelmet() == null && isHelmet(item)) {
			inv.setHelmet(item);
		}
		else if (inv.getChestplate() == null && isChestplate(item)) {
			inv.setChestplate(item);
		}
		else if (inv.getLeggings() == null && isLeggings(item)) {
			inv.setLeggings(item);
		}
		else if (inv.getBoots() == null && isBoots(item)) {
			inv.setBoots(item);
		}
		else {
			inv.addItem(item);
		}
	}


	//	private static boolean isTool(final Material item) {
	//		return
	//				item == Material.SHEARS ||
	//				item == Material.WOOD_AXE ||
	//				item == Material.WOOD_PICKAXE ||
	//				item == Material.WOOD_SPADE ||
	//				item == Material.WOOD_HOE ||
	//				item == Material.IRON_AXE ||
	//				item == Material.IRON_PICKAXE ||
	//				item == Material.IRON_SPADE ||
	//				item == Material.IRON_HOE ||
	//				item == Material.DIAMOND_AXE ||
	//				item == Material.DIAMOND_PICKAXE ||
	//				item == Material.DIAMOND_SPADE ||
	//				item == Material.DIAMOND_HOE ||
	//				item == Material.STONE_AXE ||
	//				item == Material.STONE_PICKAXE ||
	//				item == Material.STONE_SPADE ||
	//				item == Material.STONE_HOE;
	//	}

	//	private static boolean isWeapon(final Material item) {
	//		return
	//				item == Material.BOW ||
	//				item == Material.WOOD_SWORD ||
	//				item == Material.IRON_SWORD ||
	//				item == Material.DIAMOND_SWORD ||
	//				item == Material.STONE_SWORD;
	//	}



}
