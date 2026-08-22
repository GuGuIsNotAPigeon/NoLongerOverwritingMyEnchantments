package top.g2inp.nlome;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import top.g2inp.nlome.protection.ProtectionHandler;

public class NoLongerOverwritingMyEnchantments implements ModInitializer {
	public static final String MOD_ID = "nlome";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ProtectionHandler.register();

		LOGGER.info("Hello Fabric world!");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
