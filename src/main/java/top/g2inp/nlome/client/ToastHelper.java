package top.g2inp.nlome.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public class ToastHelper {
	public static void showIntercepted(int count, int threshold) {
		Minecraft.getInstance().execute(() ->
			Minecraft.getInstance().getToastManager().addToast(
				new SystemToast(
					new SystemToast.SystemToastId(),
					Component.translatable("toast.nlome.intercepted.title"),
					Component.translatable("toast.nlome.intercepted", count, threshold))));
	}
}
