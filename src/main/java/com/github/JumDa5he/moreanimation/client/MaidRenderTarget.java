package com.github.JumDa5he.moreanimation.client;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.AnimatableEntity;

/** 只认可包装对象的原实体，不把渲染接口或替身当成女仆。 */
public final class MaidRenderTarget {
    private MaidRenderTarget() {}

    public static EntityMaid resolve(Object animatable) {
        if (!(animatable instanceof AnimatableEntity<?> wrapper)) return null;
        Object actualEntity = wrapper.getEntity();
        return actualEntity instanceof EntityMaid maid ? maid : null;
    }
}
