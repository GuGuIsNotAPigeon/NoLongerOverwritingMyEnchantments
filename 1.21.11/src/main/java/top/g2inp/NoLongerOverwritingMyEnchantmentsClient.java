package top.g2inp;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

import top.g2inp.config.ClientFavorites;
import top.g2inp.network.ModPayloads;

public class NoLongerOverwritingMyEnchantmentsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(ModPayloads.SyncConfigPayload.TYPE, (payload, context) ->
			ClientFavorites.set(payload.favorites(), payload.breakThreshold()));

		ClientPlayNetworking.registerGlobalReceiver(ModPayloads.InterceptedPayload.TYPE, (payload, context) ->
			context.client().execute(() ->
				context.client().getToastManager().addToast(
					new SystemToast(
						new SystemToast.SystemToastId(),
						Component.translatable("toast.nlome.intercepted.title"),
						Component.translatable("toast.nlome.intercepted", payload.count(), payload.threshold())))));
	}
}
