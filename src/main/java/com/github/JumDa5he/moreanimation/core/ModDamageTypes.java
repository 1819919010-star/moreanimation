package com.github.JumDa5he.moreanimation.core;

import com.github.JumDa5he.moreanimation.MoreAnimation;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

/** DamageType entries are loaded by the server's data registry from damage_type JSON. */
public final class ModDamageTypes {
    public static final ResourceKey<DamageType> PLAYING = ResourceKey.create(Registries.DAMAGE_TYPE,
            new ResourceLocation(MoreAnimation.MOD_ID, "playing"));

    private ModDamageTypes() {}

    public static DamageSource playing(Level level) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(PLAYING));
    }
}
