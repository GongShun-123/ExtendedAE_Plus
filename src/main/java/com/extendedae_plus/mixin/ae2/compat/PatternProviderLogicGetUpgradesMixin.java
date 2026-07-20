package com.extendedae_plus.mixin.ae2.compat;

import appeng.helpers.patternprovider.PatternProviderLogic;
import com.extendedae_plus.compat.UpgradeSlotCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.nbt.CompoundTag;

/**
 * 最终方案：单一 @Inject，最小依赖。
 * - 仅注入 readFromNBT TAIL，优先级 1500 确保在 Appflux 加载后执行。
 * - 不使用 @Shadow，所有操作通过纯反射完成。
 * - 不使用 @Override，不实现任何接口。
 */
@Mixin(value = PatternProviderLogic.class, priority = 1500, remap = false)
public abstract class PatternProviderLogicGetUpgradesMixin {

    @Unique
    private static boolean eap$fixLogged = false;

    @Inject(method = "readFromNBT", at = @At("TAIL"), remap = false)
    private void onReadFromNBT(CompoundTag nbt, CallbackInfo ci) {
        if (!eap$fixLogged) {
            eap$fixLogged = true;
            // 使用 throw+catch 确保日志写入
            try {
                throw new RuntimeException("[EAPFix] readFromNBT injection FIRED at prio 1500! Class=" + this.getClass().getSimpleName());
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }

        try {
            if (!UpgradeSlotCompat.shouldListenToAppfluxUpgrades()) return;

            java.lang.reflect.Field afField = this.getClass().getDeclaredField("af_$upgrades");
            afField.setAccessible(true);
            Object val = afField.get(this);
            if (!(val instanceof appeng.api.upgrades.IUpgradeInventory)) return;

            appeng.api.upgrades.IUpgradeInventory current = (appeng.api.upgrades.IUpgradeInventory) val;
            int target = UpgradeSlotCompat.getPatternProviderAppfluxUpgradeSlots();
            if (current.size() == target) return;

            // 反射获取 host
            java.lang.reflect.Field hostField = this.getClass().getDeclaredField("host");
            hostField.setAccessible(true);
            Object host = hostField.get(this);
            if (host == null) return;

            Object icon = host.getClass().getMethod("getTerminalIcon").invoke(host);
            Object item = icon.getClass().getMethod("getItem").invoke(icon);

            appeng.api.upgrades.IUpgradeInventory expanded = appeng.api.upgrades.UpgradeInventories.forMachine(
                (net.minecraft.world.level.ItemLike) item,
                target,
                () -> {
                    try { UpgradeSlotCompat.invokePatternProviderAppfluxUpgradesChanged(this); }
                    catch (Exception ignored) {}
                }
            );

            int copyCount = Math.min(current.size(), target);
            for (int i = 0; i < copyCount; i++) {
                net.minecraft.world.item.ItemStack stack = current.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    expanded.insertItem(i, stack.copy(), false);
                }
            }

            afField.set(this, expanded);
            throw new RuntimeException("[EAPFix] EXPANDED: " + current.size() + " -> " + target);
        } catch (RuntimeException e) {
            e.printStackTrace();
        } catch (Exception e) {
            throw new RuntimeException("[EAPFix] FAILED: " + e.getMessage(), e);
        }
    }
}
