package com.github.JumDa5he.moreanimation.client;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.AnimatableEntity;

/** 只认可包装中的原实体，不能用 IMaid 的渲染替身判断资格。 */
public final class ActualMaid {
    private ActualMaid() {}

    public static EntityMaid from(Object wrapper) {
        if (!(wrapper instanceof AnimatableEntity<?> animatable)) return null;
        Object entity = animatable.getEntity();
        return entity instanceof EntityMaid maid ? maid : null;
    }
}
