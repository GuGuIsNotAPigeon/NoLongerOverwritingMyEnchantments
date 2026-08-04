package top.g2inp.nlome.config;

import java.util.List;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

public final class ClientFavorites {
	private static List<ResourceKey<Enchantment>> favorites = List.of();
	private static int breakThreshold = FavoritesManager.DEFAULT_BREAK_THRESHOLD;

	private ClientFavorites() {
	}

	public static void set(List<ResourceKey<Enchantment>> entries, int breakThreshold) {
		favorites = List.copyOf(entries);
		ClientFavorites.breakThreshold = breakThreshold;
	}

	public static List<ResourceKey<Enchantment>> get() {
		return favorites;
	}

	public static int getBreakThreshold() {
		return breakThreshold;
	}
}
