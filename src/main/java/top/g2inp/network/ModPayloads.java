package top.g2inp.network;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

import top.g2inp.NoLongerOverwritingMyEnchantments;

public final class ModPayloads {
	private static final StreamCodec<ByteBuf, ResourceKey<Enchantment>> FAVORITE_CODEC = ResourceKey
		.streamCodec(Registries.ENCHANTMENT);

	private static final StreamCodec<ByteBuf, List<ResourceKey<Enchantment>>> FAVORITES_CODEC = ByteBufCodecs
		.collection(ArrayList::new, FAVORITE_CODEC);

	public record SetConfigPayload(List<ResourceKey<Enchantment>> favorites, int breakThreshold) implements CustomPacketPayload {
		public static final Type<SetConfigPayload> TYPE = new Type<>(NoLongerOverwritingMyEnchantments.id("set_config"));
		public static final StreamCodec<RegistryFriendlyByteBuf, SetConfigPayload> CODEC = StreamCodec.composite(
			FAVORITES_CODEC, SetConfigPayload::favorites,
			ByteBufCodecs.VAR_INT, SetConfigPayload::breakThreshold,
			SetConfigPayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record SyncConfigPayload(List<ResourceKey<Enchantment>> favorites, int breakThreshold) implements CustomPacketPayload {
		public static final Type<SyncConfigPayload> TYPE = new Type<>(NoLongerOverwritingMyEnchantments.id("sync_config"));
		public static final StreamCodec<RegistryFriendlyByteBuf, SyncConfigPayload> CODEC = StreamCodec.composite(
			FAVORITES_CODEC, SyncConfigPayload::favorites,
			ByteBufCodecs.VAR_INT, SyncConfigPayload::breakThreshold,
			SyncConfigPayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record InterceptedPayload(int count, int threshold) implements CustomPacketPayload {
		public static final Type<InterceptedPayload> TYPE = new Type<>(NoLongerOverwritingMyEnchantments.id("intercepted"));
		public static final StreamCodec<ByteBuf, InterceptedPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, InterceptedPayload::count,
			ByteBufCodecs.VAR_INT, InterceptedPayload::threshold,
			InterceptedPayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static void register() {
		PayloadTypeRegistry.playC2S().register(SetConfigPayload.TYPE, SetConfigPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(SyncConfigPayload.TYPE, SyncConfigPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(InterceptedPayload.TYPE, InterceptedPayload.CODEC);
	}

	private ModPayloads() {
	}
}
