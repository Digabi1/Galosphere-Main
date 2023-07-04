package net.orcinus.galosphere.init;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.orcinus.galosphere.Galosphere;
import net.orcinus.galosphere.effects.GMobEffect;

public class GMobEffects {

    public static final MobEffect TRANSIT = new GMobEffect(MobEffectCategory.BENEFICIAL, 2376123);

    public static void init() {
        Registry.register(BuiltInRegistries.MOB_EFFECT, Galosphere.id("transit"), TRANSIT);
    }

}
