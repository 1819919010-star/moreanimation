package com.github.JumDa5he.moreanimation.compat.cute;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import java.lang.reflect.*;
import java.util.*;

/** 已核对可爱互动 1.0.1 的结束入口；不触碰战斗、死亡或永久外观状态。 */
public final class CuteActivityAccess {
    private static final String PREFIX="cn.autoforged.maid_cute_activity.";
    private static final Map<String,Method> METHODS=new java.util.concurrent.ConcurrentHashMap<>();
    private static boolean reported;
    public static void clearClientPose(UUID id) throws ReflectiveOperationException {
        String[][] entries={{"ScareAnimClient","SCARE_END_GAME_TIME","SCARE_PLAYING"},
            {"PullwbAnimClient","PULLWB_END_GAME_TIME","PULLWB_PLAYING"},
            {"CirclewbAnimClient","CIRCLEWB_ACTIVE","CIRCLEWB_PLAYING"},
            {"MoreExpressionAnimClient","ANIM_CHOICES","EXPRESSION_END_GAME_TIME","EXPRESSION_PLAYING"},
            {"AfraidWeaponAnimClient","AFRAID_END_GAME_TIME","AFRAID_PLAYING"}};
        for(var entry:entries)for(int i=1;i<entry.length;i++) {
            Field f=Class.forName(PREFIX+entry[0]).getDeclaredField(entry[i]);f.setAccessible(true);
            Object values=f.get(null);
            if(values instanceof Map<?,?> map)map.remove(id);
            else if(values instanceof Set<?> set)set.remove(id);
        }
    }
    private static final String[][] CANCEL={
        {"MaidsitHandler","leaveMaidsit"},{"MercyHandler","cancelMercy"},
        {"CompanionHandler","cancelCompanion"},{"SleepWithYouHandler","cancelSleepWithYou"},
        {"BegFoodHandler","cancelBegFood"},{"EatTogetherHandler","cancelEatTogether"},
        {"ComfortOwnerHandler","cancelComfortOwner"},{"FoodGuardHandler","cancelGuard"},
        {"MoreDanceHandler","cancelDance"},{"LaowuPoseHandler","cancelPose"},
        {"PullwbHandler","cancelPullwb"},{"TailPullInteractionHandler","cancelInteraction"},
        {"MoreExpressionHandler","cancelMoreExpression"},{"AfraidWeaponHandler","cancelAfraidWeapon"},
        {"SlapHandler","cancelSlap"},{"MaidBellHandler","cancelBegging"},
        {"LonelinessHandler","cancelInteractionEventsForPullwb"},
        {"LonelinessHandler","cancelGoheiDanceAndCleanWbForPetReaction"},
        {"LonelinessHandler","cancelHiGreeting"}
    };
    public static Object call(String owner,String method,Object...args) throws ReflectiveOperationException {
        String key=owner+"#"+method+"/"+args.length;
        Method found=METHODS.get(key);
        if(found==null) {
            for(Method candidate:Class.forName(PREFIX+owner).getDeclaredMethods()) {
                if(candidate.getName().equals(method)&&candidate.getParameterCount()==args.length) {
                    found=candidate; found.setAccessible(true); METHODS.put(key,found); break;
                }
            }
            if(found==null)throw new NoSuchMethodException(key);
        }
        return found.invoke(null,args);
    }
    private static Object state(String handler,UUID id) throws ReflectiveOperationException {
        Field f=Class.forName(PREFIX+handler).getDeclaredField("STATES");f.setAccessible(true);
        return ((Map<?,?>)f.get(null)).get(id);
    }
    private static Field field(Object state,String name) throws ReflectiveOperationException {
        Field f=state.getClass().getDeclaredField(name); f.setAccessible(true); return f;
    }
    public static boolean canYield(EntityMaid maid) {
        try {
            if(maid.isSleeping()||maid.getTarget()!=null||maid.isLeashed()||maid.isPassenger()
                    ||maid.getBrain().hasMemoryValue(net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET))return false;
            String[][] danger={{"LonelinessHandler","isChokeActive"},{"FlattenHandler","isFlattenActive"},
                {"HangMaidHandler","isHangActive"},{"HangTypeHandler","isHangTypeActive"},
                {"WbhurtHandler","isWbhurtActive"},{"PullearHandler","isPullearActive"},
                {"FleeHandler","isFleeActive"},{"EscapeAttackHandler","isEscapeAttackActive"}};
            for(var entry:danger)if(Boolean.TRUE.equals(call(entry[0],entry[1],maid)))return false;
            Object state=state("LonelinessHandler",maid.getUUID());
            if(state!=null && (field(state,"overeatingEventActive").getBoolean(state)
                    ||field(state,"wildMaidTargetUUID").get(state)!=null||field(state,"fightArenaTargetUUID").get(state)!=null
                    ||field(state,"selfReliancePanicPhase").getInt(state)>0)) return false;
            return true;
        } catch(ReflectiveOperationException|LinkageError e) {
            if(!reported) { reported=true;org.apache.logging.log4j.LogManager.getLogger().error("可爱互动接口校验失败，拒绝接管",e); }
            return false;
        }
    }
    public static void yield(EntityMaid maid) throws ReflectiveOperationException {
        for(var entry:CANCEL)call(entry[0],entry[1],maid);
        Object brush=state("TailBrushHandler",maid.getUUID());
        if(brush!=null)call("TailBrushHandler","exitTailBrushMode",maid,brush);
        if(state("PetReactionHandler",maid.getUUID())!=null) {
            Field reactions=Class.forName(PREFIX+"PetReactionHandler").getDeclaredField("STATES");
            reactions.setAccessible(true);((Map<?,?>)reactions.get(null)).remove(maid.getUUID());
            call("PetReactionHandler","clearMaidAnimationIfMine",maid);
        }
        Object state=state("LonelinessHandler",maid.getUUID());
        if(state!=null) {
            String[][] ends={{"guduAnimEndTick","interruptGuduAnim"},{"yaowbAnimEndTick","interruptYaowbAnim"},
                    {"petHeadAnimEndTick","restorePetHeadAnim"},{"hugAnimEndTick","restoreHugAnim"}};
            for(var entry:ends)if(field(state,entry[0]).getLong(state)>=0)call("LonelinessHandler",entry[1],maid,state);
            if(field(state,"jijijiTrapped").getBoolean(state))call("LonelinessHandler","interruptJijiji",maid,state);
            field(state,"selfEatActive").setBoolean(state,false);
            field(state,"baotouAnimEndTick").setLong(state,-1L);
            field(state,"hitBaotouAnimEndTick").setLong(state,-1L);
            if(field(state,"beggingEndTick").getLong(state)>=0) {
                maid.setBegging(false);field(state,"beggingEndTick").setLong(state,-1L);
            }
        }
        Object weather=state("WeatherAnimHandler",maid.getUUID());
        if(weather!=null) {
            call("WeatherAnimHandler","endWeatherAnim",maid,weather,true);
            call("WeatherAnimHandler","endWeatherAnim",maid,weather,false);
        }
        // 结束 API 处理各自状态后，仅清理残余的可中断普通身体姿势一次。
        if(Set.of(100,101,103,104,105,106,107,108,109,110,111,112,113,114,115,120,122,123,137,138,158,159,160,161,162,163,164).contains(maid.animationId)) {
            maid.animationId=0;maid.animationRecordTime=System.currentTimeMillis();
            com.github.tartaricacid.touhoulittlemaid.network.NetworkHandler.sendToNearby(maid,
                    new com.github.tartaricacid.touhoulittlemaid.network.message.MaidAnimationMessage(maid.getId(),0));
        }
    }
}
