package com.extendedae_plus.compat;

import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import com.extendedae_plus.init.ModItems;
import com.extendedae_plus.util.Logger;
import com.glodblock.github.extendedae.common.parts.PartExPatternProvider;
import com.glodblock.github.extendedae.common.tileentities.TileExPatternProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 统一管理样板供应器升级槽与扩展样板供应器的扩容页逻辑。
 */
public final class UpgradeSlotCompat {
    private static final String APPFLUX_MOD_ID = "appflux";
    private static final int BASE_PATTERN_PROVIDER_UPGRADE_SLOTS = 2;
    private static final int EXTENDED_PATTERN_PROVIDER_EXTRA_UPGRADE_SLOTS = 3;
    private static final int EXTENDED_PATTERN_PROVIDER_BASE_PAGES = 1;
    private static final int EXTENDED_PATTERN_PROVIDER_SLOTS_PER_PAGE = 36;
    private static final String[] APPFLUX_UPGRADES_FIELD_NAMES = { "af_upgrades", "af_$upgrades" };
    private static final String[] APPFLUX_UPGRADES_CHANGED_METHOD_NAMES = { "af_onUpgradesChanged", "af_$onUpgradesChanged" };

    private static Field patternProviderAppfluxUpgradesField;
    private static boolean patternProviderAppfluxUpgradesFieldResolved;
    private static Method patternProviderAppfluxUpgradesChangedMethod;
    private static boolean patternProviderAppfluxUpgradesChangedMethodResolved;

    private UpgradeSlotCompat() {
    }

    public static boolean isAppfluxPresent() {
        return ModList.get().isLoaded(APPFLUX_MOD_ID);
    }

    public static boolean usesDedicatedUpgradeSlots() {
        return !isAppfluxPresent();
    }

    public static boolean usesAppfluxUpgradeSlots() {
        return isAppfluxPresent();
    }

    public static boolean shouldEnableUpgradeSlots() {
        return usesDedicatedUpgradeSlots();
    }

    public static boolean shouldManageLocalUpgradeInventory() {
        return usesDedicatedUpgradeSlots();
    }

    public static boolean shouldEnableChannelCard() {
        return true;
    }

    public static boolean shouldListenToAppfluxUpgrades() {
        return usesAppfluxUpgradeSlots();
    }

    public static boolean shouldAddUpgradePanelToScreen() {
        return usesDedicatedUpgradeSlots();
    }

    public static int getPatternProviderLocalUpgradeSlots() {
        return getPatternProviderLocalUpgradeSlots(null);
    }

    public static int getPatternProviderLocalUpgradeSlots(Object host) {
        return BASE_PATTERN_PROVIDER_UPGRADE_SLOTS
                + (isExtendedPatternProviderHost(host) ? EXTENDED_PATTERN_PROVIDER_EXTRA_UPGRADE_SLOTS : 0);
    }

    public static int getPatternProviderAppfluxUpgradeSlots() {
        return getPatternProviderAppfluxUpgradeSlots(null);
    }

    public static int getPatternProviderAppfluxUpgradeSlots(Object host) {
        return BASE_PATTERN_PROVIDER_UPGRADE_SLOTS
                + (isExtendedPatternProviderHost(host) ? EXTENDED_PATTERN_PROVIDER_EXTRA_UPGRADE_SLOTS : 0);
    }

    public static boolean isExtendedPatternProviderHost(Object host) {
        return host instanceof TileExPatternProvider || host instanceof PartExPatternProvider;
    }

    public static int getExtendedPatternProviderPatternCapacity() {
        return getExtendedPatternProviderTotalPages() * EXTENDED_PATTERN_PROVIDER_SLOTS_PER_PAGE;
    }

    public static int getExtendedPatternProviderTotalPages() {
        return EXTENDED_PATTERN_PROVIDER_BASE_PAGES + EXTENDED_PATTERN_PROVIDER_EXTRA_UPGRADE_SLOTS;
    }

    public static int getUnlockedExtendedPatternProviderPages(Iterable<ItemStack> upgrades) {
        int expansionCards = 0;
        if (upgrades != null) {
            for (ItemStack stack : upgrades) {
                if (isExtendedPatternProviderExpansionCard(stack)) {
                    expansionCards++;
                }
            }
        }

        expansionCards = Math.min(expansionCards, EXTENDED_PATTERN_PROVIDER_EXTRA_UPGRADE_SLOTS);
        return EXTENDED_PATTERN_PROVIDER_BASE_PAGES + expansionCards;
    }

    public static int getUnlockedExtendedPatternProviderSlots(Iterable<ItemStack> upgrades) {
        return getUnlockedExtendedPatternProviderPages(upgrades) * EXTENDED_PATTERN_PROVIDER_SLOTS_PER_PAGE;
    }

    public static boolean isExtendedPatternProviderExpansionCard(ItemStack stack) {
        return stack != null
                && !stack.isEmpty()
                && stack.getItem() == ModItems.EXTENDED_PATTERN_PROVIDER_EXPANSION_CARD_PLUS.get();
    }

    public static IUpgradeInventory getPatternProviderAppfluxUpgrades(Object logicInstance) {
        Field field = resolvePatternProviderAppfluxUpgradesField(logicInstance.getClass());
        if (field == null) {
            return null;
        }

        try {
            Object value = field.get(logicInstance);
            if (!(value instanceof IUpgradeInventory)) {
                return null;
            }
            IUpgradeInventory inventory = (IUpgradeInventory) value;

            // 惰性槽位扩展：每次获取库存时自动检测并修正，不依赖任何 Mixin 回调
            int target = getPatternProviderAppfluxUpgradeSlots();
            int currentSize = inventory.size();
            if (currentSize != target) {
                expandAppfluxSlots(logicInstance, inventory, target);
            }

            return inventory;
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    private static void expandAppfluxSlots(Object logicInstance, IUpgradeInventory current, int target) {
        try {
            // 获取 host 以读取图标
            Field hostField = logicInstance.getClass().getDeclaredField("host");
            hostField.setAccessible(true);
            Object host = hostField.get(logicInstance);
            if (host == null) return;

            // 获取图标物品
            java.lang.reflect.Method getIcon = host.getClass().getMethod("getTerminalIcon");
            Object icon = getIcon.invoke(host);
            java.lang.reflect.Method getItem = icon.getClass().getMethod("getItem");
            Object item = getItem.invoke(icon);
            if (!(item instanceof net.minecraft.world.level.ItemLike)) return;

            // 创建扩展库存
            IUpgradeInventory expanded = UpgradeInventories.forMachine(
                    (net.minecraft.world.level.ItemLike) item,
                    target,
                    () -> {
                        try { invokePatternProviderAppfluxUpgradesChanged(logicInstance); }
                        catch (Exception ignored) {}
                    }
            );

            // 复制已有物品
            int copyCount = Math.min(current.size(), target);
            for (int i = 0; i < copyCount; i++) {
                net.minecraft.world.item.ItemStack stack = current.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    expanded.insertItem(i, stack.copy(), false);
                }
            }

            setPatternProviderAppfluxUpgrades(logicInstance, expanded);
            Logger.EAP$LOGGER.warn("[EAPFix-SC] Expanded Appflux slots from {} to {}", current.size(), target);
        } catch (Exception e) {
            Logger.EAP$LOGGER.warn("[EAPFix-SC] Slot expansion failed: {}", e.getMessage());
        }
    }

    public static boolean setPatternProviderAppfluxUpgrades(Object logicInstance, IUpgradeInventory inventory) {
        Field field = resolvePatternProviderAppfluxUpgradesField(logicInstance.getClass());
        if (field == null) {
            return false;
        }

        try {
            field.set(logicInstance, inventory);
            return true;
        } catch (IllegalAccessException e) {
            return false;
        }
    }

    public static boolean invokePatternProviderAppfluxUpgradesChanged(Object logicInstance) throws ReflectiveOperationException {
        Method method = resolvePatternProviderAppfluxUpgradesChangedMethod(logicInstance.getClass());
        if (method == null) {
            return false;
        }

        method.invoke(logicInstance);
        return true;
    }

    private static Field resolvePatternProviderAppfluxUpgradesField(Class<?> logicClass) {
        if (!patternProviderAppfluxUpgradesFieldResolved) {
            patternProviderAppfluxUpgradesField = findField(logicClass, APPFLUX_UPGRADES_FIELD_NAMES);
            patternProviderAppfluxUpgradesFieldResolved = true;
        }
        return patternProviderAppfluxUpgradesField;
    }

    private static Method resolvePatternProviderAppfluxUpgradesChangedMethod(Class<?> logicClass) {
        if (!patternProviderAppfluxUpgradesChangedMethodResolved) {
            patternProviderAppfluxUpgradesChangedMethod = findMethod(logicClass, APPFLUX_UPGRADES_CHANGED_METHOD_NAMES);
            patternProviderAppfluxUpgradesChangedMethodResolved = true;
        }
        return patternProviderAppfluxUpgradesChangedMethod;
    }

    private static Field findField(Class<?> owner, String[] candidates) {
        for (String candidate : candidates) {
            try {
                Field field = owner.getDeclaredField(candidate);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }

    private static Method findMethod(Class<?> owner, String[] candidates) {
        for (String candidate : candidates) {
            try {
                Method method = owner.getDeclaredMethod(candidate);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }
}
