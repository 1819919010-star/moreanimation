package com.github.JumDa5he.moreanimation.core;

import com.github.JumDa5he.moreanimation.MoreAnimation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MoreAnimation.MOD_ID);
    public static final RegistryObject<SoundEvent> SLAP = SOUNDS.register("slap", () ->
            SoundEvent.createVariableRangeEvent(new ResourceLocation(MoreAnimation.MOD_ID, "slap")));
    private ModSounds() {}
}
