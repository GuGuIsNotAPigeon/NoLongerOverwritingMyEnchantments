package top.g2inp.mixin;

import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.trading.MerchantOffers;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import top.g2inp.protection.ProtectionHandler;

@Mixin(Villager.class)
public abstract class VillagerMixin {
	@Inject(method = "setVillagerData", at = @At("HEAD"), cancellable = true)
	private void nlome$preserveOffers(VillagerData villagerData, CallbackInfo ci) {
		Villager villager = (Villager) (Object) this;
		VillagerProfession oldProfession = villager.getVillagerData().getProfession();
		if (oldProfession != VillagerProfession.NONE
			&& villagerData.getProfession() == VillagerProfession.NONE
			&& ProtectionHandler.isProtected(villager)) {
			ci.cancel();
		}
	}

	@Inject(method = "setOffers", at = @At("HEAD"), cancellable = true)
	private void nlome$interceptTradeRefresh(MerchantOffers offers, CallbackInfo ci) {
		if (offers == null && ProtectionHandler.onTradeRefreshIntercepted((Villager) (Object) this)) {
			ci.cancel();
		}
	}

	@Inject(method = "updateTrades", at = @At("TAIL"))
	private void nlome$recheckProtection(CallbackInfo ci) {
		ProtectionHandler.onTradesUpdated((Villager) (Object) this);
	}
}
