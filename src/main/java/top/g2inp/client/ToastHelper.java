package top.g2inp.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public class ToastHelper {
	private static final SystemToast.SystemToastId INTERCEPTED_TOAST_ID = new SystemToast.SystemToastId();
	private static final SystemToast.SystemToastId REFRESHED_TOAST_ID = new SystemToast.SystemToastId();

	public static void showIntercepted(int count, int threshold) {
		show(
			INTERCEPTED_TOAST_ID,
			Component.translatable("toast.nlome.intercepted.title"),
			Component.translatable("toast.nlome.intercepted", count, threshold));
	}

	public static void showRefreshed() {
		show(
			REFRESHED_TOAST_ID,
			Component.translatable("toast.nlome.refreshed.title"),
			Component.translatable("toast.nlome.refreshed"));
	}

	private static void show(SystemToast.SystemToastId id, Component title, Component description) {
		Minecraft.getInstance().execute(() ->
			Minecraft.getInstance().getToastManager().addToast(new SystemToast(id, title, description)));
	}
}