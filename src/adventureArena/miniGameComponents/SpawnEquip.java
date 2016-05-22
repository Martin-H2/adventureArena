package adventureArena.miniGameComponents;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import adventureArena.ItemHelper;


// WARNING SERIALIZED OBJECT ! Don't rename or refactor path
public class SpawnEquip implements ConfigurationSerializable {

	Vector		signPos;
	Material	itemMaterial;
	Material	targetMaterial;
	int			amount;
	Enchantment	ench;
	int			enchLevel;

	public SpawnEquip(final Vector signPos, final Material itemMaterial, final Material targetMaterial, final int amount, final Enchantment ench, final int enchLevel) {
		super();
		this.signPos = signPos;
		this.itemMaterial = itemMaterial;
		this.targetMaterial = targetMaterial;
		this.amount = amount;
		this.ench = ench;
		this.enchLevel = enchLevel;
	}



	public SpawnEquip(final Map<String, Object> serializedForm) {
		signPos = (Vector) serializedForm.get("signPos");
		itemMaterial = Material.valueOf((String) serializedForm.get("itemMaterial"));
		if (serializedForm.containsKey("targetMaterial")) {
			targetMaterial = Material.valueOf((String) serializedForm.get("targetMaterial"));
		}
		else {
			targetMaterial = null;
		}
		amount = (int) serializedForm.get("amount");
		if (serializedForm.containsKey("ench")) {
			ench = Enchantment.getByName((String) serializedForm.get("ench"));
			enchLevel = (int) serializedForm.get("enchLevel");
		}
		else {
			ench = null;
			enchLevel = 0;
		}
	}


	@Override
	public Map<String, Object> serialize() {
		Map<String, Object> serializedForm = new HashMap<String, Object>();
		serializedForm.put("signPos", signPos);
		serializedForm.put("itemMaterial", itemMaterial.toString());
		if (targetMaterial != null) {
			serializedForm.put("targetMaterial", targetMaterial.toString());
		}
		serializedForm.put("amount", amount);
		if (ench != null) {
			serializedForm.put("ench", ench.getName());
			serializedForm.put("enchLevel", enchLevel);
		}
		return serializedForm;
	}



	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof SpawnEquip)) return false;
		return signPos.equals(((SpawnEquip) obj).signPos);
	}



	public Vector getSignPos() {
		return signPos;
	}



	public ItemStack toItemStack() {
		return ItemHelper.createAdventureItem(itemMaterial, amount, targetMaterial, ench, enchLevel);
	}


}
