package com.httpedor.rpgdamageoverhaul;

import com.bawnorton.mixinsquared.api.MixinCanceller;

import java.util.List;

public class OptionalMixinCanceler implements MixinCanceller {

    @Override
    public boolean shouldCancel(List<String> targetClassNames, String mixinClassName) {
        if (!mixinClassName.contains("data/rpgdamageoverhaul"))
            return false;
        return false;
    }
}

