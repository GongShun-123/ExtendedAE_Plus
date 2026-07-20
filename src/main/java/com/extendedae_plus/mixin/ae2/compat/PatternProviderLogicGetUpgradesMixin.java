package com.extendedae_plus.mixin.ae2.compat;

import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import appeng.helpers.patternprovider.PatternProviderLogic;
import com.extendedae_plus.compat.UpgradeSlotCompat;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * readFromNBT 后扩展 Appflux 槽位。prio 1500 确保在 Appflux loadUpgrade(prio 1000) 之后执行。
 * 遍历类继承链查找 af_$upgrades 字段，兼容 MeteoritePatternProviderLogic 等子类。
 */
@Mixin(value = PatternProviderLogic.class, priority = 1500, remap = false)
public abstract class PatternProviderLogicGetUpgradesMixin {

    @Inject(method = "readFromNBT", at = @At("TAIL"))
    private void expandSlotsAfterReadFromNBT(CompoundTag tag, CallbackInfo ci) {
        try {
            if (!UpgradeSlotCompat.shouldListenToAppfluxUpgrades()) return;

            // 遍历继承链查找 af_$upgrades（Mixins 字段在父类 PatternProviderLogic 声明）
            java.lang.reflect.Field afField = findFieldInHierarchy(this.getClass(), "af_$upgrades");
            if (afField == null) return;
            afField.setAccessible(true);
            Object val = afField.get(this);
            if (!(val instanceof IUpgradeInventory)) return;

            IUpgradeInventory current = (IUpgradeInventory) val;
            int target = UpgradeSlotCompat.getPatternProviderAppfluxUpgradeSlots();
            if (current.size() == target) return;

            // 获取 host
            java.lang.reflect.Field hostField = findFieldInHierarchy(this.getClass(), "host");
            if (hostField == null) return;
            hostField.setAccessible(true);
            Object host = hostField.get(this);
            if (host == null) return;

            Object icon = host.getClass().getMethod("getTerminalIcon").invoke(host);
            Object item = icon.getClass().getMethod("getItem").invoke(icon);

            IUpgradeInventory expanded = UpgradeInventories.forMachine(
                (net.minecraft.world.level.ItemLike) item, target,
                () -> {
                    try { UpgradeSlotCompat.invokePatternProviderAppfluxUpgradesChanged(this); }
                    catch (Exception ignored) {}
                }
            );

            // 先设置字段再复制物品——防止无限递归
            afField.set(this, expanded);

            int copyCount = Math.min(current.size(), target);
            for (int i = 0; i < copyCount; i++) {
                net.minecraft.world.item.ItemStack stack = current.getStackInSlot(i);
                if (!stack.isEmpty()) expanded.insertItem(i, stack.copy(), false);
            }
            System.out.println("[EAPFix] Expanded: " + current.size() + " -> " + target + " for " + this.getClass().getSimpleName());
        } catch (Exception e) {
            System.out.println("[EAPFix] Error: " + e.getMessage());
        }
    }

    private static java.lang.reflect.Field findFieldInHierarchy(Class<?> clazz, String name) {
        while (clazz != null && clazz != Object.class) {
            try { return clazz.getDeclaredField(name); }
            catch (NoSuchFieldException e) { clazz = clazz.getSuperclass(); }
        }
        return null;
    }
}
