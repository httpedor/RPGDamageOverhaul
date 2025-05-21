package com.httpedor.rpgdamageoverhaul.mixin;

import com.httpedor.rpgdamageoverhaul.RPGDamageOverhaul;
import net.minecraft.network.Connection;
import net.minecraftforge.network.HandshakeHandler;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;


// As we deal with a non-dynamic registry, we need to make sure that the server and client are synced
// as soon as possible, and that means sending our login packet before all others
// I fucking hate origins man, it took me so long to figure this out

@Mixin(HandshakeHandler.class)
public class HandshakeHandlerMixin {

    @Shadow private List<NetworkRegistry.LoginPayload> messageList;

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    void boostOrder(Connection networkManager, NetworkDirection side, CallbackInfo ci)
    {
        int index = -1;
        NetworkRegistry.LoginPayload payload = null;
        for (int i = 0; i < messageList.size(); i++)
        {
            var p = messageList.get(i);
            if (p.getMessageContext().equals("rpgdologinpacket"))
            {
                index = i;
                payload = p;
                break;
            }
        }
        if (index == -1)
            return;

        messageList.remove(index);
        messageList.add(0, payload);
    }

}
