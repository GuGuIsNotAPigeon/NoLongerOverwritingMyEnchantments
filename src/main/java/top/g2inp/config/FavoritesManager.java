package top.g2inp.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

import top.g2inp.NoLongerOverwritingMyEnchantments;

public final class FavoritesManager {
	public static final int DEFAULT_BREAK_THRESHOLD = 3;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("nlome-favorites.json");
	private static final FavoritesManager INSTANCE = new FavoritesManager();

	public record ConfigData(List<ResourceKey<Enchantment>> favorites, int breakThreshold) {
	}

	private List<ResourceKey<Enchantment>> favorites = List.of();
	private int breakThreshold = DEFAULT_BREAK_THRESHOLD;

	private FavoritesManager() {
		this.load();
	}

	public static FavoritesManager get() {
		return INSTANCE;
	}

	public static ConfigData loadData() {
		if (!Files.isRegularFile(FILE)) {
			return new ConfigData(List.of(), DEFAULT_BREAK_THRESHOLD);
		}

		try {
			JsonElement root = JsonParser.parseString(Files.readString(FILE, StandardCharsets.UTF_8));
			if (root.isJsonArray()) {
				return new ConfigData(parseFavorites(root.getAsJsonArray()), DEFAULT_BREAK_THRESHOLD);
			}

			JsonObject object = root.getAsJsonObject();
			List<ResourceKey<Enchantment>> favorites = object.has("favorites")
				? parseFavorites(object.getAsJsonArray("favorites"))
				: List.of();
			int breakThreshold = object.has("breakThreshold") ? object.get("breakThreshold").getAsInt() : DEFAULT_BREAK_THRESHOLD;
			return new ConfigData(favorites, breakThreshold);
		} catch (Exception exception) {
			NoLongerOverwritingMyEnchantments.LOGGER.warn("Failed to load config", exception);
			return new ConfigData(List.of(), DEFAULT_BREAK_THRESHOLD);
		}
	}

	public static void saveData(ConfigData data) {
		try {
			JsonObject object = new JsonObject();
			JsonArray array = new JsonArray();
			for (ResourceKey<Enchantment> enchantment : data.favorites()) {
				array.add(enchantment.identifier().toString());
			}

			object.add("favorites", array);
			object.addProperty("breakThreshold", data.breakThreshold());
			Files.writeString(FILE, GSON.toJson(object), StandardCharsets.UTF_8);
		} catch (IOException exception) {
			NoLongerOverwritingMyEnchantments.LOGGER.warn("Failed to save config", exception);
		}
	}

	private static List<ResourceKey<Enchantment>> parseFavorites(JsonArray array) {
		List<ResourceKey<Enchantment>> entries = new ArrayList<>();
		for (JsonElement element : array) {
			entries.add(ResourceKey.create(Registries.ENCHANTMENT, Identifier.parse(element.getAsString())));
		}

		return List.copyOf(entries);
	}

	public List<ResourceKey<Enchantment>> getFavorites() {
		return this.favorites;
	}

	public boolean isFavorite(ResourceKey<Enchantment> enchantment) {
		return this.favorites.contains(enchantment);
	}

	public int getBreakThreshold() {
		return this.breakThreshold;
	}

	public void setFavorites(List<ResourceKey<Enchantment>> favorites) {
		this.favorites = List.copyOf(favorites);
		this.save();
	}

	public void setBreakThreshold(int breakThreshold) {
		this.breakThreshold = Math.max(1, breakThreshold);
		this.save();
	}

	private void load() {
		ConfigData data = loadData();
		this.favorites = data.favorites();
		this.breakThreshold = data.breakThreshold();
	}

	private void save() {
		saveData(new ConfigData(this.favorites, this.breakThreshold));
	}
}
