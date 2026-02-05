package com.httpedor.rpgdamageoverhaul;

import com.bawnorton.mixinsquared.api.MixinCanceller;
import net.minecraftforge.fml.loading.LoadingModList;

import java.util.List;

public class OptionalMixinCanceler implements MixinCanceller {

    @Override
    public boolean shouldCancel(List<String> targetClassNames, String mixinClassName) {
        if (!mixinClassName.contains("rpgdamageoverhaul"))
            return false;

        var instance = LoadingModList.get();
        var deps = List.of("soulsweapons", "bettercombat", "simplyswords", "simplymore", "bettermobcombat");
        for (var dep : deps)
        {
            if (instance.getModFileById(dep) == null && mixinClassName.contains(dep))
                return true;
        }
        return false;
    }
}

