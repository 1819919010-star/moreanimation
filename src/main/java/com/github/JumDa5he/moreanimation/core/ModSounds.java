package com.github.JumDa5he.moreanimation.core;

import com.github.JumDa5he.moreanimation.MoreAnimation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, MoreAnimation.MOD_ID);
    public static final DeferredHolder<SoundEvent, SoundEvent> SLAP = SOUNDS.register("slap", () ->
            SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MoreAnimation.MOD_ID, "slap")));
    public static final DeferredHolder<SoundEvent, SoundEvent> TAIL_SNIFF = SOUNDS.register("tail_sniff", () ->
            SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MoreAnimation.MOD_ID, "tail_sniff")));
    private ModSounds() {}
}
