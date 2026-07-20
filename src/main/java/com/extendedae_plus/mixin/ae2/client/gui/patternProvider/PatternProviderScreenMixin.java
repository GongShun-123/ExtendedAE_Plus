package com.extendedae_plus.mixin.ae2.client.gui.patternProvider;

import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.Upgrades;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.implementations.PatternProviderScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.UpgradesPanel;
import appeng.core.localization.GuiText;
import appeng.menu.SlotSemantics;
import appeng.menu.implementations.PatternProviderMenu;
import com.extendedae_plus.compat.UpgradeSlotCompat;
import com.extendedae_plus.mixin.ae2.accessor.PatternProviderMenuAccessor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(PatternProviderScreen.class)
public abstract class PatternProviderScreenMixin<C extends PatternProviderMenu> extends AEBaseScreen<C> {
    public PatternProviderScreenMixin(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void eap$initCompatUpgrades(PatternProviderMenu menu, Inventory playerInventory, Component title, ScreenStyle style, CallbackInfo ci) {
        if (!UpgradeSlotCompat.shouldAddUpgradePanelToScreen()) {
            return;
        }

        try {
            this.widgets.add("upgrades", new UpgradesPanel(
                    menu.getSlots(SlotSemantics.UPGRADE),
                    this::eap$getCompatibleUpgrades));
        } catch (Exception e) {
            com.extendedae_plus.util.Logger.EAP$LOGGER.error("PatternProviderScreen兼容性升级面板初始化失败", e);
        }
    }

    /**
     * 显示样板供应器的customName
     */
    @Inject(method = "updateBeforeRender", at = @At("RETURN"), remap = false)
    private void onUpdateBeforeRender(CallbackInfo ci) {
        Component t = this.getTitle();
        if (!t.getString().isEmpty()) {
            this.setTextContent(AEBaseScreen.TEXT_ID_DIALOG_TITLE, t);
        }
    }

    @Unique
    private List<Component> eap$getCompatibleUpgrades() {
        var list = new ArrayList<Component>();
        list.add(GuiText.CompatibleUpgrades.text());

        try {
            if (((PatternProviderMenuAccessor) this.menu).eap$logic() instanceof IUpgradeableObject upgradeableLogic) {
                var upgrades = upgradeableLogic.getUpgrades();
                if (upgrades != null) {
                    list.addAll(Upgrades.getTooltipLinesForMachine(upgrades.getUpgradableItem()));
                }
            }
        } catch (Exception e) {
            com.extendedae_plus.util.Logger.EAP$LOGGER.error("获取兼容升级列表失败", e);
        }

        return list;
    }
}
