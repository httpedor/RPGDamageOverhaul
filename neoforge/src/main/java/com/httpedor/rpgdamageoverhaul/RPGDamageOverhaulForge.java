package com.httpedor.rpgdamageoverhaul;


import com.httpedor.rpgdamageoverhaul.network.ForgeNetworkInit;
import com.httpedor.rpgdamageoverhaul.network.payload.RPGDODatapackSyncPayload;
import com.httpedor.rpgdamageoverhaul.reload.DamageClassReloadListener;
import com.httpedor.rpgdamageoverhaul.reload.OverridesReloadListener;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@Mod(Constants.MOD_ID)
public class RPGDamageOverhaulForge {
    public RPGDamageOverhaulForge(IEventBus eventBus) {
        RPGDamageOverhaul.init();
        // Optional GearSockets compat. Guarded so the compat classes (and thus GearSockets) are only
        // ever resolved when the mod is actually present; RPGDO runs fine without it.
        if (net.neoforged.fml.ModList.get().isLoaded("gearsockets"))
            com.httpedor.rpgdamageoverhaul.compat.gearsockets.GearSocketsCompatInit.init();
        eventBus.addListener(EventPriority.HIGHEST, ForgeNetworkInit::register);
        NeoForge.EVENT_BUS.addListener(RPGDamageOverhaulForge::onDatapackRegister);
        // LOWEST so we run after mods that rebuild the attribute section wholesale (AttributeSetter's
        // mergeTooltips clears and re-emits it from raw modifier data). Coloring before that happens
        // means our lines get thrown away and replaced with uncolored ones.
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, RPGDamageOverhaulForge::onTooltip);
        NeoForge.EVENT_BUS.addListener(RPGDamageOverhaulForge::onDatapackSync);
    }

    private static void onTooltip(ItemTooltipEvent e)
    {
        SharedLogic.modifyTooltip(e.getToolTip(), e.getItemStack(), e.getFlags().isAdvanced());
    }

    private static void onDatapackRegister(AddReloadListenerEvent e)
    {
        e.addListener(new DamageClassReloadListener(e.getRegistryAccess()));
        e.addListener(new OverridesReloadListener(e.getRegistryAccess()));
    }

    private static void onDatapackSync(OnDatapackSyncEvent e)
    {
        e.getRelevantPlayers().forEach(p -> {
            PacketDistributor.sendToPlayer(p, RPGDODatapackSyncPayload.INSTANCE);
        });
    }
}
