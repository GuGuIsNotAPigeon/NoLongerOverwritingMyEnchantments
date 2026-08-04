package top.g2inp.nlome;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
//import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceLocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import top.g2inp.nlome.config.FavoritesManager;
import top.g2inp.nlome.network.ModPayloads;
import top.g2inp.nlome.network.ModPayloads.SetConfigPayload;
import top.g2inp.nlome.network.ModPayloads.SyncConfigPayload;
import top.g2inp.nlome.protection.ProtectionHandler;

public class NoLongerOverwritingMyEnchantments implements ModInitializer {
	public static final String MOD_ID = "nlome";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {

		ModPayloads.register();
		ProtectionHandler.register();
		ServerPlayNetworking.registerGlobalReceiver(SetConfigPayload.TYPE, (payload, context) -> {
			FavoritesManager.get().setFavorites(payload.favorites());
			FavoritesManager.get().setBreakThreshold(payload.breakThreshold());
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
			sender.sendPacket(new SyncConfigPayload(
				FavoritesManager.get().getFavorites(),
				FavoritesManager.get().getBreakThreshold())));

		LOGGER.info("Hello Fabric world!");
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}
