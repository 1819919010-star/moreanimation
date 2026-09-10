package com.github.JumDa5he.moreanimation.mixin;

import com.github.JumDa5he.moreanimation.MoreAnimation;
import com.github.tartaricacid.touhoulittlemaid.client.resource.GeckoModelLoader;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.file.AnimationFile;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.io.InputStream;


@Mixin(GeckoModelLoader.class)
public class GeckoModelLoaderMixin {
    @Unique private static final Logger MOREANIMATION_LOGGER = LogManager.getLogger();
    @Unique private static final ResourceLocation MOREANIMATION_ACTIONS =
            new ResourceLocation(MoreAnimation.MOD_ID, "animation/unknown.animation.json");
    @Unique private static final ResourceLocation MOREANIMATION_COMPAT =
            new ResourceLocation(MoreAnimation.MOD_ID, "animation/compat.animation.json");
    @Unique private static AnimationFile moreanimation$animations;

    @Inject(method = "reload", at = @At("HEAD"), remap = false)
    private static void moreanimation$clearAnimationCache(CallbackInfo ci) {
        moreanimation$animations = null;
    }

    @Inject(method = "registerMaidAnimations", at = @At("HEAD"), remap = false)
    private static void moreanimation$mergeAnimations(ResourceLocation id, AnimationFile animationFile,
                                                       CallbackInfo ci) {
        AnimationFile additions = moreanimation$getAnimations();
        if (additions != null) {
            additions.animations().forEach((name, animation) -> {
                // A skin pack's own animation is authoritative. Replacing an existing idle/main
                // animation can remove its model-specific visibility rules (for example Expression_7).
                if (!animationFile.animations().containsKey(name)) {
                    animationFile.putAnimation(name, animation);
                }
            });
        }
    }

    @Unique
    private static AnimationFile moreanimation$getAnimations() {
        if (moreanimation$animations != null) return moreanimation$animations;
        AnimationFile loaded = new AnimationFile();
        if (!moreanimation$merge(MOREANIMATION_ACTIONS, loaded)) return null;
        moreanimation$merge(MOREANIMATION_COMPAT, loaded);
        moreanimation$animations = loaded;
        MOREANIMATION_LOGGER.info("Loaded {} shared maid animations", loaded.animations().size());
        return loaded;
    }

    @Unique
    private static boolean moreanimation$merge(ResourceLocation location, AnimationFile target) {
        try (InputStream stream = Minecraft.getInstance().getResourceManager().open(location)) {
            GeckoModelLoader.mergeAnimationFile(stream, target);
            return true;
        } catch (IOException e) {
            MOREANIMATION_LOGGER.error("Failed to load shared maid animation file {}", location, e);
            return false;
        }
    }
}
