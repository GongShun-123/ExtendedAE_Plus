package com.extendedae_plus.mixin.extendedae.common.matrix;

import com.extendedae_plus.content.matrix.CrafterCorePlusBlockEntity;
import com.glodblock.github.extendedae.common.me.matrix.ClusterAssemblerMatrix;
import com.glodblock.github.extendedae.common.tileentities.matrix.TileAssemblerMatrixCrafter;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ClusterAssemblerMatrix.class, remap = false)
public class ClusterAssemblerMatrixMixin {

    @Shadow
    @Final
    private ReferenceSet<TileAssemblerMatrixCrafter> availableCrafters;

    @Shadow
    @Final
    private ReferenceSet<TileAssemblerMatrixCrafter> busyCrafters;

    @Inject(method = "addCrafter", at = @At("HEAD"), cancellable = true)
    private void onAddCrafter(TileAssemblerMatrixCrafter crafter, CallbackInfo ci) {
        if (crafter instanceof CrafterCorePlusBlockEntity plusCrafter) {
            if (plusCrafter.usedThread() < CrafterCorePlusBlockEntity.MAX_THREAD) {
                this.availableCrafters.add(crafter);
            } else {
                this.busyCrafters.add(crafter);
            }
            ci.cancel();
        }
    }

    @Inject(method = "getBusyCrafterAmount", at = @At("HEAD"), cancellable = true)
    private void onGetBusyCrafterAmount(CallbackInfoReturnable<Integer> cir) {
        int count = 0;
        for (var crafter : this.busyCrafters) {
            count += crafter instanceof CrafterCorePlusBlockEntity
                    ? CrafterCorePlusBlockEntity.MAX_THREAD
                    : TileAssemblerMatrixCrafter.MAX_THREAD;
        }
        for (var crafter : this.availableCrafters) {
            count += crafter.usedThread();
        }
        cir.setReturnValue(count);
    }
}
