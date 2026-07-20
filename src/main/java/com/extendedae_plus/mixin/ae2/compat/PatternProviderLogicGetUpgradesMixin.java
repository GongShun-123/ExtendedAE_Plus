package com.extendedae_plus.mixin.ae2.compat;

import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import com.extendedae_plus.compat.UpgradeSlotCompat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在 readFromNBT 之后（prio 1500，Appflux 的 loadUpgrade 在 prio 1000 之后）
 * 直接检查并扩展 af_$upgrades 字段的槽位数。
 * 不依赖 getUpgrades() 方法覆盖，完全绕开 Mixin interface 方法解析问题。
 */
@Mixin(value = PatternProviderLogic.class, priority = 1500, remap = false)
public abstract class PatternProviderLogicGetUpgradesMixin {

    @Final
    @Shadow
    private PatternProviderLogicHost host;

    @Inject(method = "readFromNBT", at = @At("TAIL"))
    private void eap$expandSlotsAfterAppfluxLoad(CompoundTag tag, CallbackInfo ci) {
        try {
            if (!UpgradeSlotCompat.shouldListenToAppfluxUpgrades()) return;

            // 直接反射获取 af_$upgrades
            java.lang.reflect.Field afField = this.getClass().getDeclaredField("af_$upgrades");
            afField.setAccessible(true);
            Object value = afField.get(this);
            if (!(value instanceof IUpgradeInventory)) return;

            IUpgradeInventory current = (IUpgradeInventory) value;
            int target = UpgradeSlotCompat.getPatternProviderAppfluxUpgradeSlots();
            int currentSize = current.size();

            if (currentSize == target) return;

            // 创建扩展库存
            IUpgradeInventory expanded = UpgradeInventories.forMachine(
                    host.getTerminalIcon().getItem(),
                    target,
                    () -> {
                        try { UpgradeSlotCompat.invokePatternProviderAppfluxUpgradesChanged(this); }
                        catch (Exception ignored) {}
                    }
            );

            // 复制已有物品
            int copyCount = Math.min(currentSize, target);
            for (int i = 0; i < copyCount; i++) {
                ItemStack stack = current.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    expanded.insertItem(i, stack.copy(), false);
                }
            }

            // 替换库存
            afField.set(this, expanded);
            System.out.println("[EAPFix] Expanded slots after readFromNBT: " + currentSize + " -> " + target);

        } catch (Exception e) {
            System.out.println("[EAPFix] Expansion failed: " + e.getMessage());
        }
    }
}
