package com.github.JumDa5he.moreanimation.compat.animation;

import com.github.JumDa5he.moreanimation.compat.network.AnimationSyncPacket;
import com.github.JumDa5he.moreanimation.compat.network.ExpressionSyncPacket;
import com.github.JumDa5he.moreanimation.compat.network.MoreAnimationNetwork;
import com.github.JumDa5he.moreanimation.config.MoreAnimationConfig;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Per-maid animation preferences and the lightweight special-action lock. */
public final class MaidAnimationData {
    public static final String TAIL_INTERACTION_ACTIVE = "moreanimation_tail_drag_active";
    public static final String FACE_INTERACTION_ACTIVE = "moreanimation_face_interaction_active";
    private static final String ENABLED_PREFIX = "moreanimation_enabled_";
    private static final String ACTIVE = "moreanimation_active_action";
    private static final String ACTIVE_UNTIL = "moreanimation_active_until";
    private static final String ACTIVE_START = "moreanimation_active_start";
    private static final String ACTIVE_PRIORITY = "moreanimation_active_priority";
    private static final String ACTIVE_LOCK_MOVEMENT = "moreanimation_active_lock_movement";
    private static final String INJURED_AUTO = "moreanimation_injured_auto";
    private static final String INJURED_AUTO_SET = "moreanimation_injured_auto_set";
    private static final String FOX_FORM = "moreanimation_fox_form";
    private static final String FOX_FORM_SET = "moreanimation_fox_form_set";
    private static final String AUTO_PET = "moreanimation_auto_pet";
    private static final String AUTO_PET_SET = "moreanimation_auto_pet_set";
    private static final String AUTO_HUG = "moreanimation_auto_hug";
    private static final String AUTO_HUG_SET = "moreanimation_auto_hug_set";
    private static final String RANDOM_SLEEP_POSE = "moreanimation_random_sleep_pose";
    private static final String RANDOM_SLEEP_POSE_SET = "moreanimation_random_sleep_pose_set";
    private static final String FORM_MODE = "moreanimation_form_mode";
    private static final String MANUAL_EXPRESSION = "moreanimation_expression";
    private static final String RANDOM_EXPRESSION = "moreanimation_random_expression";
    private static final String RANDOM_EXPRESSION_UNTIL = "moreanimation_random_expression_until";
    private static final String RANDOM_EXPRESSION_NEXT_CHECK = "moreanimation_random_expression_next_check";
    private static final String CLIENT_EXPRESSION = "moreanimation_expression_render";
    public static final String AUTO_INTERACTION_COOLDOWN = "moreanimation_auto_interaction_cooldown";

    public static final int FORM_AUTO = 0;
    public static final int FORM_HUMAN = 1;
    public static final int FORM_FOX = 2;

    public static final int PRIORITY_RANDOM = 10;
    public static final int PRIORITY_MANUAL = 20;
    public static final int PRIORITY_WATER_SHAKE = 30;
    public static final int PRIORITY_INJURED = 50;
    public static final int PRIORITY_INTERACTION = 40;
    public static final int PRIORITY_KICK_BUTT = 60;
    public static final int PRIORITY_KICK_LAUNCH = 70;
    public static final int PRIORITY_DEATH = 100;

    public static final List<String> EXPRESSIONS = List.of(
            "veryangry", "wuyu", "sosad", "provoke", "lips", "sneer", "dizziness", "kuang", "uhoh");

    public static final Map<String, List<String>> ACTIONS = new LinkedHashMap<>();
    private static final Set<String> PARALLEL_ACTIONS = Set.of(
            "pet_other_head_raise", "pet_other_head", "pet_reaction", "pet_reaction_hold", "hugtogether",
            "lips", "ear_pull_left", "ear_pull_right", "hang", "game_lost2", "tailcircle", "dance1",
            "circledance", "CLEANTAIL", "!??!", "beg2", "fallen_broken_leg", "broken_leg_crawl",
            "slapright", "slapleft");
    private static final Set<String> LOOPING_ACTIONS = Set.of(
            "come", "come2", "weidu", "ha", "morebeg", "sleep2", "eattail", "catchbyhook",
            "drowning", "situp", "pet_reaction_hold", "pet_other_head", "tailpull", "lips",
            "ear_pull_left", "ear_pull_right", "hang", "game_lost2", "tailcircle", "dance1",
            "circledance", "CLEANTAIL", "!??!", "sit2", "moresleep2", "moresleep3",
            "moresleep4", "moresleep5", "moresleep6", "cold_hug_shiver", "ground_hurt",
            "kick_launch_front", "beg2", "fallen_broken_leg", "broken_leg_crawl");
    private static final Set<String> SLEEP_BODY_ACTIONS = Set.of(
            "sleep", "come", "sleep2", "situp", "moresleep2", "moresleep3",
            "moresleep4", "moresleep5", "moresleep6");
    static {
        ACTIONS.put("stand", List.of("circledance", "!??!"));
        ACTIONS.put("sit", List.of("come2", "ha", "tastetail"));
        ACTIONS.put("sleep", List.of("come", "sleep2", "situp"));
    }

    private MaidAnimationData() {
    }

    public static boolean isParallelAction(String action) {
        return PARALLEL_ACTIONS.contains(action);
    }

    public static boolean isLoopingAction(String action) {
        return LOOPING_ACTIONS.contains(action);
    }

    public static List<String> enabledActions(EntityMaid maid, String category) {
        CompoundTag data = maid.getPersistentData();
        String key = ENABLED_PREFIX + category;
        if (!data.contains(key, Tag.TAG_LIST)) {
            List<String> defaults = new ArrayList<>(MoreAnimationConfig.getEnabledActions(category));
            writeList(data, key, defaults);
            return defaults;
        }
        List<String> result = new ArrayList<>();
        ListTag tag = data.getList(key, Tag.TAG_STRING);
        for (int i = 0; i < tag.size(); i++) {
            String action = tag.getString(i);
            if (ACTIONS.getOrDefault(category, List.of()).contains(action)) {
                result.add(action);
            }
        }
        return result;
    }

    public static boolean isEnabled(EntityMaid maid, String category, String action) {
        return enabledActions(maid, category).contains(action);
    }

    public static void setEnabled(EntityMaid maid, String category, String action, boolean enabled) {
        if (!ACTIONS.getOrDefault(category, List.of()).contains(action)) return;
        List<String> values = enabledActions(maid, category);
        if (enabled && !values.contains(action)) values.add(action);
        if (!enabled) values.remove(action);
        writeList(maid.getPersistentData(), ENABLED_PREFIX + category, values);
    }

    private static void writeList(CompoundTag data, String key, List<String> values) {
        ListTag list = new ListTag();
        values.forEach(value -> list.add(StringTag.valueOf(value)));
        data.put(key, list);
    }

    public static boolean injuredAuto(EntityMaid maid) {
        CompoundTag data = maid.getPersistentData();
        return !data.getBoolean(INJURED_AUTO_SET) || data.getBoolean(INJURED_AUTO);
    }

    public static void setInjuredAuto(EntityMaid maid, boolean enabled) {
        maid.getPersistentData().putBoolean(INJURED_AUTO_SET, true);
        maid.getPersistentData().putBoolean(INJURED_AUTO, enabled);
    }

    public static boolean foxFormEnabled(EntityMaid maid) {
        if (!maid.level().isClientSide()) return MoreAnimationConfig.isWinefoxLowHealthFoxEnabled();
        CompoundTag data = maid.getPersistentData();
        return data.getBoolean(FOX_FORM_SET)
                ? data.getBoolean(FOX_FORM) : MoreAnimationConfig.isWinefoxLowHealthFoxEnabled();
    }

    public static void setFoxFormEnabledLocal(EntityMaid maid, boolean enabled) {
        maid.getPersistentData().putBoolean(FOX_FORM_SET, true);
        maid.getPersistentData().putBoolean(FOX_FORM, enabled);
    }

    public static boolean autoPet(EntityMaid maid) {
        CompoundTag data = maid.getPersistentData();
        return data.getBoolean(AUTO_PET_SET)
                ? data.getBoolean(AUTO_PET) : MoreAnimationConfig.isAutoPetDefaultEnabled();
    }

    public static void setAutoPet(EntityMaid maid, boolean enabled) {
        maid.getPersistentData().putBoolean(AUTO_PET_SET, true);
        maid.getPersistentData().putBoolean(AUTO_PET, enabled);
    }

    public static boolean autoHug(EntityMaid maid) {
        CompoundTag data = maid.getPersistentData();
        return data.getBoolean(AUTO_HUG_SET)
                ? data.getBoolean(AUTO_HUG) : MoreAnimationConfig.isAutoHugDefaultEnabled();
    }

    public static void setAutoHug(EntityMaid maid, boolean enabled) {
        maid.getPersistentData().putBoolean(AUTO_HUG_SET, true);
        maid.getPersistentData().putBoolean(AUTO_HUG, enabled);
    }

    public static boolean randomSleepPose(EntityMaid maid) {
        CompoundTag data = maid.getPersistentData();
        return !data.getBoolean(RANDOM_SLEEP_POSE_SET) || data.getBoolean(RANDOM_SLEEP_POSE);
    }

    public static void setRandomSleepPose(EntityMaid maid, boolean enabled) {
        maid.getPersistentData().putBoolean(RANDOM_SLEEP_POSE_SET, true);
        maid.getPersistentData().putBoolean(RANDOM_SLEEP_POSE, enabled);
    }

    public static int formMode(EntityMaid maid) {
        int mode = maid.getPersistentData().getInt(FORM_MODE);
        return mode >= FORM_AUTO && mode <= FORM_FOX ? mode : FORM_AUTO;
    }

    public static void setFormMode(EntityMaid maid, int mode) {
        maid.getPersistentData().putInt(FORM_MODE, clampFormMode(mode));
    }

    public static void setFormModeLocal(EntityMaid maid, int mode) {
        setFormMode(maid, mode);
    }

    public static boolean shouldForceHuman(EntityMaid maid) {
        int mode = formMode(maid);
        return mode == FORM_HUMAN || (mode == FORM_AUTO && !foxFormEnabled(maid));
    }

    public static boolean shouldForceFox(EntityMaid maid) {
        return formMode(maid) == FORM_FOX;
    }

    private static int clampFormMode(int mode) {
        return Math.max(FORM_AUTO, Math.min(FORM_FOX, mode));
    }

    public static boolean start(EntityMaid maid, String action, int duration, int priority, boolean lockMovement) {
        long now = maid.level().getGameTime();
        CompoundTag data = maid.getPersistentData();
        if (data.getBoolean(TAIL_INTERACTION_ACTIVE) && priority < PRIORITY_DEATH) return false;
        if (maid.isSleeping() && !isAllowedWhileSleeping(action)) return false;
        if (isActive(maid) && data.getInt(ACTIVE_PRIORITY) > priority) return false;
        data.putString(ACTIVE, action);
        data.putLong(ACTIVE_START, now);
        data.putLong(ACTIVE_UNTIL, now + Math.max(1, duration));
        data.putInt(ACTIVE_PRIORITY, priority);
        data.putBoolean(ACTIVE_LOCK_MOVEMENT, lockMovement);
        GameLostAnimation.ACTIVE.put(maid.getUUID(), "special:" + action);
        if (maid.level() instanceof ServerLevel) {
            MoreAnimationNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> maid),
                    new AnimationSyncPacket(maid.getId(), action, duration, priority, lockMovement));
        }
        return true;
    }

    public static void clientStart(EntityMaid maid, String action, int duration, int priority, boolean lockMovement) {
        if (maid.isSleeping() && !isAllowedWhileSleeping(action)) return;
        CompoundTag data = maid.getPersistentData();
        data.putString(ACTIVE, action);
        data.putLong(ACTIVE_START, maid.level().getGameTime());
        data.putLong(ACTIVE_UNTIL, maid.level().getGameTime() + Math.max(1, duration));
        data.putInt(ACTIVE_PRIORITY, priority);
        data.putBoolean(ACTIVE_LOCK_MOVEMENT, lockMovement);
        if (action.startsWith("death_")) {
            data.putInt("moreanimation_death_delay", duration);
            data.putInt("moreanimation_death_animation_elapsed", 0);
            maid.deathTime = 0;
        }
        GameLostAnimation.ACTIVE.put(maid.getUUID(), "special:" + action);
    }

    public static void stop(EntityMaid maid) {
        clearLocal(maid);
        maid.getNavigation().setSpeedModifier(1.0D);
        if (maid.level() instanceof ServerLevel) {
            MoreAnimationNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> maid),
                    new AnimationSyncPacket(maid.getId(), "", 0, 0, false));
        }
    }

    public static void clearLocal(EntityMaid maid) {
        CompoundTag data = maid.getPersistentData();
        data.remove(ACTIVE);
        data.remove(ACTIVE_UNTIL);
        data.remove(ACTIVE_START);
        data.remove(ACTIVE_PRIORITY);
        data.remove(ACTIVE_LOCK_MOVEMENT);
        String active = GameLostAnimation.ACTIVE.get(maid.getUUID());
        if (active != null && active.startsWith("special:")) GameLostAnimation.ACTIVE.remove(maid.getUUID());
    }

    public static boolean isActive(EntityMaid maid) {
        CompoundTag data = maid.getPersistentData();
        return !data.getString(ACTIVE).isEmpty() && maid.level().getGameTime() < data.getLong(ACTIVE_UNTIL);
    }

    public static boolean isActive(EntityMaid maid, String action) {
        return isActive(maid) && action.equals(maid.getPersistentData().getString(ACTIVE));
    }

    public static String activeAction(EntityMaid maid) {
        return isActive(maid) ? maid.getPersistentData().getString(ACTIVE) : "";
    }

    public static long activeStart(EntityMaid maid) {
        return isActive(maid) ? maid.getPersistentData().getLong(ACTIVE_START) : Long.MIN_VALUE;
    }

    public static int activePriority(EntityMaid maid) {
        return isActive(maid) ? maid.getPersistentData().getInt(ACTIVE_PRIORITY) : Integer.MIN_VALUE;
    }

    public static boolean isTailInteractionActive(EntityMaid maid) {
        return maid.getPersistentData().getBoolean(TAIL_INTERACTION_ACTIVE);
    }

    public static boolean isFaceInteractionActive(EntityMaid maid) {
        return maid.getPersistentData().getBoolean(FACE_INTERACTION_ACTIVE);
    }

    /** Manual terminal expressions always take precedence over the timed random overlay. */
    public static String effectiveExpression(EntityMaid maid) {
        CompoundTag data = maid.getPersistentData();
        if (maid.level().isClientSide()) return data.getString(CLIENT_EXPRESSION);
        String manual = data.getString(MANUAL_EXPRESSION);
        if (!manual.isEmpty()) return manual;
        return maid.level().getGameTime() < data.getLong(RANDOM_EXPRESSION_UNTIL)
                ? data.getString(RANDOM_EXPRESSION) : "";
    }

    public static void setManualExpression(EntityMaid maid, String expression) {
        if (!expression.isEmpty() && !EXPRESSIONS.contains(expression)) return;
        if (expression.isEmpty()) maid.getPersistentData().remove(MANUAL_EXPRESSION);
        else maid.getPersistentData().putString(MANUAL_EXPRESSION, expression);
        syncExpression(maid);
    }

    public static void setExpressionLocal(EntityMaid maid, String expression) {
        if (expression.isEmpty()) maid.getPersistentData().remove(CLIENT_EXPRESSION);
        else maid.getPersistentData().putString(CLIENT_EXPRESSION, expression);
    }

    public static void syncExpression(EntityMaid maid) {
        if (maid.level() instanceof ServerLevel) {
            MoreAnimationNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> maid),
                    new ExpressionSyncPacket(maid.getId(), effectiveExpression(maid)));
        }
    }

    private static void tickRandomExpression(EntityMaid maid) {
        CompoundTag data = maid.getPersistentData();
        long now = maid.level().getGameTime();
        if (!maid.isAlive() || isTailInteractionActive(maid)) {
            boolean wasVisible = data.getString(MANUAL_EXPRESSION).isEmpty()
                    && !data.getString(RANDOM_EXPRESSION).isEmpty();
            data.remove(RANDOM_EXPRESSION);
            data.remove(RANDOM_EXPRESSION_UNTIL);
            if (wasVisible) syncExpression(maid);
            return;
        }

        if (activePriority(maid) >= PRIORITY_INJURED
                && data.getString(MANUAL_EXPRESSION).isEmpty()
                && !data.getString(RANDOM_EXPRESSION).isEmpty()) {
            data.remove(RANDOM_EXPRESSION);
            data.remove(RANDOM_EXPRESSION_UNTIL);
            syncExpression(maid);
        }

        if (!data.getString(RANDOM_EXPRESSION).isEmpty()
                && now >= data.getLong(RANDOM_EXPRESSION_UNTIL)) {
            data.remove(RANDOM_EXPRESSION);
            data.remove(RANDOM_EXPRESSION_UNTIL);
            syncExpression(maid);
        }

        if (!data.contains(RANDOM_EXPRESSION_NEXT_CHECK, Tag.TAG_LONG)) {
            data.putLong(RANDOM_EXPRESSION_NEXT_CHECK, now + 1 + maid.getRandom().nextInt(1200));
            return;
        }
        if (now < data.getLong(RANDOM_EXPRESSION_NEXT_CHECK)) return;
        data.putLong(RANDOM_EXPRESSION_NEXT_CHECK, now + 1200);
        if (!data.getString(MANUAL_EXPRESSION).isEmpty()
                || !data.getString(RANDOM_EXPRESSION).isEmpty()
                || activePriority(maid) >= PRIORITY_INJURED) return;
        if (maid.getRandom().nextFloat() >= 0.10F) return;

        String expression = EXPRESSIONS.get(maid.getRandom().nextInt(EXPRESSIONS.size()));
        data.putString(RANDOM_EXPRESSION, expression);
        data.putLong(RANDOM_EXPRESSION_UNTIL, now + 200);
        syncExpression(maid);
    }

    public static void clearTransientExpression(EntityMaid maid) {
        CompoundTag data = maid.getPersistentData();
        data.remove(RANDOM_EXPRESSION);
        data.remove(RANDOM_EXPRESSION_UNTIL);
        data.remove(RANDOM_EXPRESSION_NEXT_CHECK);
    }

    public static void serverTick(EntityMaid maid) {
        if (!maid.level().isClientSide()) tickRandomExpression(maid);
        if (maid.isSleeping() && isActive(maid) && !isAllowedWhileSleeping(activeAction(maid))) {
            stop(maid);
            return;
        }
        if (!isActive(maid)) {
            if (!maid.getPersistentData().getString(ACTIVE).isEmpty()) stop(maid);
            return;
        }
        CompoundTag data = maid.getPersistentData();
        if ("tastetail".equals(data.getString(ACTIVE))
                && maid.level().getGameTime() - data.getLong(ACTIVE_START) >= 45) {
            start(maid, "eattail", 100, data.getInt(ACTIVE_PRIORITY), false);
            return;
        }
        if (data.getBoolean(ACTIVE_LOCK_MOVEMENT)) freeze(maid);
    }

    public static boolean isAllowedWhileSleeping(String action) {
        return SLEEP_BODY_ACTIONS.contains(action) || action.startsWith("death_");
    }

    public static void freeze(EntityMaid maid) {
        maid.getNavigation().stop();
        maid.getNavigation().setSpeedModifier(0.0D);
        maid.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        maid.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        maid.getBrain().eraseMemory(MemoryModuleType.PATH);
        maid.setDeltaMovement(0, maid.getDeltaMovement().y, 0);
    }

    public static int duration(String action) {
        return switch (action) {
            case "refuse" -> 20;
            case "maid_bow" -> 48;
            case "death_fall" -> 28;
            case "death_drown" -> 72;
            case "death_burn" -> 56;
            case "death_ranged" -> 32;
            case "injured_kneel" -> 60;
            case "fear_retreat_fall" -> 50;
            case "pet_reaction" -> 64;
            case "pet_reaction_hold", "pet_other_head" -> 30;
            case "pet_other_head_raise" -> 16;
            case "come2" -> 100;
            case "ha" -> 120;
            case "tastetail" -> 145;
            case "eattail" -> 100;
            case "circledance" -> 90;
            case "!??!" -> 30;
            case "come" -> 200;
            case "sleep2" -> 130;
            case "situp" -> 400;
            case "ground_hurt" -> 20;
            case "cold_hug_shiver" -> 12;
            case "water_shake" -> 34;
            case "kick_butt" -> 20;
            case "kick_launch_front" -> 400;
            default -> 100;
        };
    }
}
