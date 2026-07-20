package com.extendedae_plus.mixin.extendedae.accessor;

import appeng.util.inv.AppEngInternalInventory;
import com.glodblock.github.extendedae.common.tileentities.matrix.TileAssemblerMatrixPattern;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = TileAssemblerMatrixPattern.class, remap = false)
public interface TileAssemblerMatrixPatternAccessor {

    @Accessor(value = "patternInventory", remap = false)
    @Mutable
    void extendedae_plus$setPatternInventory(AppEngInternalInventory inventory);
}
