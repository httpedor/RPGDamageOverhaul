package com.httpedor.rpgdamageoverhaul;

import com.bawnorton.mixinsquared.api.MixinCanceller;
import net.fabricmc.loader.api.FabricLoader;

import java.util.List;

public class OptionalMixinCanceler implements MixinCanceller {

    @Override
    public boolean shouldCancel(List<String> targetClassNames, String mixinClassName) {
        var instance = FabricLoader.getInstance();
        if (!instance.isModLoaded("soulsweapons") && mixinClassName.contains("soulsweapons"))
            return true;
        if (!instance.isModLoaded("bettercombat") && mixinClassName.contains("bettercombat"))
            return true;
        if (!instance.isModLoaded("simplyswords") && mixinClassName.contains("simplyswords"))
            return true;
        return false;
    }
}
