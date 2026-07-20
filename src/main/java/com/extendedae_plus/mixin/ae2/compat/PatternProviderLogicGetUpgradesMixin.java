package com.extendedae_plus.mixin.ae2.compat;

import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import appeng.helpers.patternprovider.PatternProviderLogic;
import com.extendedae_plus.compat.UpgradeSlotCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * 独立的 getUpgrades() 覆盖，优先级 1500 高于 Appflux 的 1000，
 * 确保样板供应器的升级槽位经过惰性扩展检查。
 * 与 PatternProviderLogicCompatMixin 分离，避免其构造函数注入受优先级影响。
 */
@Mixin(value = PatternProviderLogic.class, priority = 1500, remap = false)
public abstract class PatternProviderLogicGetUpgradesMixin implements IUpgradeableObject {

    /**
     * 覆盖 Appflux 的 getUpgrades()，注入惰性槽位扩展逻辑。
     */
    @Override
    public IUpgradeInventory getUpgrades() {
        if (UpgradeSlotCompat.shouldManageLocalUpgradeInventory()) {
            // 无 Appflux 时使用本地管理的库存
            try {
                java.lang.reflect.Field f = this.getClass().getDeclaredField("eap$compatUpgrades");
                f.setAccessible(true);
                IUpgradeInventory inv = (IUpgradeInventory) f.get(this);
                return inv != null ? inv : UpgradeInventories.empty();
            } catch (Exception e) {
                return UpgradeInventories.empty();
            }
        }

        if (!UpgradeSlotCompat.shouldListenToAppfluxUpgrades()) {
            try {
                java.lang.reflect.Field f = this.getClass().getDeclaredField("eap$compatUpgrades");
                f.setAccessible(true);
                IUpgradeInventory inv = (IUpgradeInventory) f.get(this);
                return inv != null ? inv : UpgradeInventories.empty();
            } catch (Exception e) {
                return UpgradeInventories.empty();
            }
        }

        // Appflux 路径：通过 UpgradeSlotCompat 获取库存（含惰性扩展）
        return UpgradeSlotCompat.getPatternProviderAppfluxUpgrades(this);
    }
}
