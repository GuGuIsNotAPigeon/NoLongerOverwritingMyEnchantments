package top.g2inp.protection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

import org.jspecify.annotations.Nullable;

import top.g2inp.NoLongerOverwritingMyEnchantments;
import top.g2inp.client.ToastHelper;
import top.g2inp.config.FavoritesManager;

public final class ProtectionHandler {
	private static final int SEARCH_RADIUS = 16;
	private static final int GLOW_TICKS = 5 * 20;

	private static final Map<GlobalPos, UUID> PROTECTED_STATIONS = new HashMap<>();

	public static final AttachmentType<ProtectionData> PROTECTION = AttachmentRegistry.create(
		NoLongerOverwritingMyEnchantments.id("protection"));

	public record ProtectionData(GlobalPos station, MerchantOffer savedOffer, int breaks) {
	}

	public static void register() {
		PlayerBlockBreakEvents.AFTER.register(ProtectionHandler::onBlockBreak);
	}

	public static boolean isProtected(Villager villager) {
		if (villager.getAttached(PROTECTION) == null) {
			return false;
		}
		if (!(villager.level() instanceof ServerLevel)) {
			return true;
		}
		return hasFavoritedOffer(villager);
	}

	public static boolean hasFavoritedOffer(Villager villager) {
		if (!(villager.level() instanceof ServerLevel)) {
			return false;
		}
		return findFavoritedOffer(villager) != null;
	}

	public static boolean onTradeRefreshIntercepted(Villager villager) {
		if (!(villager.level() instanceof ServerLevel serverLevel)) {
			return false;
		}

		ProtectionData data = villager.getAttached(PROTECTION);
		MerchantOffer favoriteOffer = findFavoritedOffer(villager);
		if (favoriteOffer == null) {
			return false;
		}

		int breaks = (data == null ? 0 : data.breaks()) + 1;
		int breakThreshold = FavoritesManager.get().getBreakThreshold();
		if (breaks >= breakThreshold) {
			villager.removeAttached(PROTECTION);
			if (data != null) {
				PROTECTED_STATIONS.remove(data.station());
			}
			Player player = villager.getTradingPlayer();
			if (player instanceof ServerPlayer serverPlayer) {
				serverPlayer.sendSystemMessage(Component.translatable("message.nlome.refreshed"));
			}
			return false;
		}

		GlobalPos station = data == null
			? villager.getBrain().getMemory(MemoryModuleType.JOB_SITE)
				.map(site -> GlobalPos.of(site.dimension(), site.pos()))
				.orElse(GlobalPos.of(serverLevel.dimension(), villager.blockPosition()))
			: data.station();
		villager.setAttached(PROTECTION, new ProtectionData(station, favoriteOffer.copy(), breaks));
		PROTECTED_STATIONS.put(station, villager.getUUID());
		Player player = villager.getTradingPlayer();
		if (player instanceof ServerPlayer) {
			ToastHelper.showIntercepted(breaks, breakThreshold - 1);
		}
		return true;
	}

	public static void onTradesUpdated(Villager villager) {
		if (!(villager.level() instanceof ServerLevel serverLevel)) {
			return;
		}

		ProtectionData data = villager.getAttached(PROTECTION);
		MerchantOffer favoriteOffer = findFavoritedOffer(villager);
		if (favoriteOffer != null) {
			List<ResourceKey<Enchantment>> favorites = FavoritesManager.get().getFavorites();
			if (data == null) {
				GlobalPos station = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE)
					.map(site -> GlobalPos.of(site.dimension(), site.pos()))
					.orElse(GlobalPos.of(serverLevel.dimension(), villager.blockPosition()));
				villager.setAttached(PROTECTION, new ProtectionData(station, favoriteOffer.copy(), 0));
				PROTECTED_STATIONS.put(station, villager.getUUID());
			} else if (!favoritedEnchantment(favoriteOffer.getResult(), favorites)
				.equals(favoritedEnchantment(data.savedOffer().getResult(), favorites))) {
				villager.setAttached(PROTECTION, new ProtectionData(data.station(), favoriteOffer.copy(), data.breaks()));
			}
		} else if (data != null) {
			MerchantOffers offers = villager.getOffers();
			if (offers != null) {
				offers.add(data.savedOffer().copy());
			}
		}
	}

	private static void onBlockBreak(Level world, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
		if (!(world instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
			return;
		}

		GlobalPos station = GlobalPos.of(serverLevel.dimension(), pos.immutable());

		UUID protectedVillagerUuid = PROTECTED_STATIONS.remove(station);
		if (protectedVillagerUuid != null) {
			if (serverLevel.getEntity(protectedVillagerUuid) instanceof Villager villager) {
				ProtectionData data = villager.getAttached(PROTECTION);
				MerchantOffer favoriteOffer = findFavoritedOffer(villager);
				if (favoriteOffer == null) {
					if (data != null) {
						villager.removeAttached(PROTECTION);
					}
					return;
				}

				int breakThreshold = FavoritesManager.get().getBreakThreshold();
				int breaks = (data == null ? 0 : data.breaks()) + 1;
				if (breaks < breakThreshold) {
					villager.setAttached(PROTECTION, new ProtectionData(station, favoriteOffer.copy(), breaks));
					PROTECTED_STATIONS.put(station, villager.getUUID());
					Component bookName = bookName(serverLevel, favoriteOffer);
					serverPlayer.sendSystemMessage(Component.translatable("message.nlome.protected", bookName));
					glowVillager(villager);
				} else {
					villager.removeAttached(PROTECTION);
					villager.getBrain().eraseMemory(MemoryModuleType.JOB_SITE);
					villager.setVillagerData(villager.getVillagerData()
						.withProfession(serverLevel.registryAccess(), VillagerProfession.NONE));
					villager.refreshBrain(serverLevel);
					serverPlayer.sendSystemMessage(Component.translatable("message.nlome.refreshed"));
					glowVillager(villager);
				}
			}

			return;
		}

		List<Villager> villagers = serverLevel.getEntities(
			EntityTypeTest.forClass(Villager.class),
			new AABB(pos).inflate(SEARCH_RADIUS),
			villager -> villager.getBrain().getMemory(MemoryModuleType.JOB_SITE)
				.map(jobSite -> jobSite.dimension().equals(serverLevel.dimension()) && jobSite.pos().equals(pos))
				.orElse(false));
		if (villagers.isEmpty()) {
			return;
		}

		for (Villager villager : villagers) {
			MerchantOffer favoriteOffer = findFavoritedOffer(villager);
			if (favoriteOffer == null) {
				continue;
			}

			villager.setAttached(PROTECTION, new ProtectionData(station, favoriteOffer.copy(), 0));
			PROTECTED_STATIONS.put(station, villager.getUUID());
			Component bookName = bookName(serverLevel, favoriteOffer);
			serverPlayer.sendSystemMessage(Component.translatable("message.nlome.protected", bookName));
			glowVillager(villager);
			return;
		}
	}

	private static Component bookName(ServerLevel serverLevel, MerchantOffer favoriteOffer) {
		ResourceKey<Enchantment> favorite = favoritedEnchantment(favoriteOffer.getResult(), FavoritesManager.get().getFavorites());
		return serverLevel.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(favorite).value().description();
	}

	private static void glowVillager(Villager villager) {
		villager.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOW_TICKS));
	}

	private static MerchantOffer findFavoritedOffer(Villager villager) {
		List<ResourceKey<Enchantment>> favorites = FavoritesManager.get().getFavorites();
		if (favorites.isEmpty()) {
			return null;
		}

		MerchantOffers offers = villager.getOffers();
		if (offers == null) {
			return null;
		}

		for (MerchantOffer offer : offers) {
			if (favoritedEnchantment(offer.getResult(), favorites) != null) {
				return offer;
			}
		}

		return null;
	}

	private static ResourceKey<Enchantment> favoritedEnchantment(ItemStack stack, List<ResourceKey<Enchantment>> favorites) {
		if (!stack.is(Items.ENCHANTED_BOOK)) {
			return null;
		}

		ItemEnchantments enchantments = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
		for (Holder<Enchantment> holder : enchantments.keySet()) {
			Optional<ResourceKey<Enchantment>> enchantmentKey = holder.unwrapKey();
			if (enchantmentKey.isPresent() && favorites.contains(enchantmentKey.get())) {
				return enchantmentKey.get();
			}
		}

		return null;
	}

	private ProtectionHandler() {
	}
}
