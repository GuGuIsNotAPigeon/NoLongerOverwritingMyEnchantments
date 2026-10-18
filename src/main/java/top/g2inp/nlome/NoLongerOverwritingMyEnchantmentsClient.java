package top.g2inp.nlome;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

import org.lwjgl.glfw.GLFW;

import top.g2inp.nlome.client.ConfigScreen;

public class NoLongerOverwritingMyEnchantmentsClient implements ClientModInitializer {
	private static final KeyMapping.Category CATEGORY =
		KeyMapping.Category.register(Identifier.fromNamespaceAndPath(NoLongerOverwritingMyEnchantments.MOD_ID, "config"));

	private static KeyMapping openConfigKey;

	@Override
	public void onInitializeClient() {
		openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.nlome.open_config", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, CATEGORY));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openConfigKey.consumeClick()) {
				if (client.screen == null && client.player != null && client.hasSingleplayerServer()) {
					client.setScreen(new ConfigScreen(null));
				}
			}
		});
	}
}
