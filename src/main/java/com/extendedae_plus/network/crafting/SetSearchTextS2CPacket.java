package com.extendedae_plus.network.crafting;  
  
import appeng.client.gui.me.common.MEStorageScreen;  
import com.extendedae_plus.mixin.ae2.accessor.MEStorageScreenAccessor;  
import com.extendedae_plus.mixin.extendedae.accessor.GuiExPatternTerminalAccessor;  
import com.glodblock.github.extendedae.client.gui.GuiExPatternTerminal;  
import net.minecraft.client.Minecraft;  
import net.minecraft.network.FriendlyByteBuf;  
import net.minecraftforge.api.distmarker.Dist;  
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
  
import java.util.function.Supplier;

public class SetSearchTextS2CPacket {  
    private final String text;  
  
    public SetSearchTextS2CPacket(String text) {  
        this.text = text;  
    }  
  
    public static void encode(SetSearchTextS2CPacket msg, FriendlyByteBuf buf) {  
        buf.writeUtf(msg.text, 256);  
    }  
  
    public static SetSearchTextS2CPacket decode(FriendlyByteBuf buf) {  
        return new SetSearchTextS2CPacket(buf.readUtf(256));  
    }  
  
    public static void handle(SetSearchTextS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {  
        ctx.get().enqueueWork(() -> handleClient(msg));  
        ctx.get().setPacketHandled(true);  
    }  
  
    @OnlyIn(Dist.CLIENT)  
    private static void handleClient(SetSearchTextS2CPacket msg) {  
        var screen = Minecraft.getInstance().screen;  
        if (screen instanceof MEStorageScreen<?> me) {  
            try {  
                MEStorageScreenAccessor acc = (MEStorageScreenAccessor) me;  
                acc.eap$getSearchField().setValue(msg.text);  
                acc.eap$setSearchText(msg.text);  
            } catch (Throwable ignored) {  
            }  
        } else if (screen instanceof GuiExPatternTerminal<?> gpt) {  
            try {  
                GuiExPatternTerminalAccessor acc = (GuiExPatternTerminalAccessor) gpt;  
                acc.getSearchOutField().setValue(msg.text);  
            } catch (Throwable ignored) {  
            }  
        }  
    }  
}