package com.github.JumDa5he.moreanimation.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;

public class MoreAnimationConfig {
    public static ForgeConfigSpec.IntValue SIT_INTERVAL_SECONDS;
    public static ForgeConfigSpec.DoubleValue SIT_CHANCE;
    public static ForgeConfigSpec.BooleanValue SIT_COME2;
    public static ForgeConfigSpec.BooleanValue SIT_HA;
    public static ForgeConfigSpec.BooleanValue SIT_TASTETAIL;

    public static ForgeConfigSpec.IntValue STAND_INTERVAL_SECONDS;
    public static ForgeConfigSpec.DoubleValue STAND_CHANCE;
    public static ForgeConfigSpec.BooleanValue STAND_CIRCLEDANCE;
    public static ForgeConfigSpec.BooleanValue STAND_QUESTION;

    public static ForgeConfigSpec.IntValue SLEEP_INTERVAL_SECONDS;
    public static ForgeConfigSpec.DoubleValue SLEEP_CHANCE;
    public static ForgeConfigSpec.BooleanValue SLEEP_COME;
    public static ForgeConfigSpec.BooleanValue SLEEP_SLEEP2;
    public static ForgeConfigSpec.BooleanValue SLEEP_SITUP;

    public static final ForgeConfigSpec SPEC;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.push("sit");
        SIT_INTERVAL_SECONDS = b.comment("坐着时动作尝试间隔（秒）").defineInRange("intervalSeconds", 60, 1, 3600);
        SIT_CHANCE = b.comment("坐着时每次尝试触发概率（0~1）").defineInRange("chance", 0.2, 0.0, 1.0);
        SIT_COME2 = b.comment("坐着动作：come2").define("come2", true);
        SIT_HA = b.comment("坐着动作：ha").define("ha", true);
        SIT_TASTETAIL = b.comment("坐着动作：tastetail（吃尾巴）").define("tastetail", true);
        b.pop();

        b.push("stand");
        STAND_INTERVAL_SECONDS = b.comment("站着时动作尝试间隔（秒）").defineInRange("intervalSeconds", 60, 1, 3600);
        STAND_CHANCE = b.comment("站着时每次尝试触发概率（0~1）").defineInRange("chance", 0.2, 0.0, 1.0);
        STAND_CIRCLEDANCE = b.comment("站着动作：circledance").define("circledance", true);
        STAND_QUESTION = b.comment("站着动作：!??!").define("question", true);
        b.pop();

        b.push("sleep");
        SLEEP_INTERVAL_SECONDS = b.comment("睡觉时动作尝试间隔（秒）").defineInRange("intervalSeconds", 60, 1, 3600);
        SLEEP_CHANCE = b.comment("睡觉时每次尝试触发概率（0~1）").defineInRange("chance", 0.2, 0.0, 1.0);
        SLEEP_COME = b.comment("睡觉动作：come").define("come", true);
        SLEEP_SLEEP2 = b.comment("睡觉动作：sleep2").define("sleep2", true);
        SLEEP_SITUP = b.comment("睡觉动作：situp").define("situp", true);
        b.pop();

        SPEC = b.build();
    }

    public static long getIntervalTicks(String state) {
        switch (state) {
            case "sit": return SIT_INTERVAL_SECONDS.get() * 20L;
            case "stand": return STAND_INTERVAL_SECONDS.get() * 20L;
            case "sleep": return SLEEP_INTERVAL_SECONDS.get() * 20L;
            default: return 1200L;
        }
    }

    public static double getChance(String state) {
        switch (state) {
            case "sit": return SIT_CHANCE.get();
            case "stand": return STAND_CHANCE.get();
            case "sleep": return SLEEP_CHANCE.get();
            default: return 0.2;
        }
    }

    public static List<String> getEnabledActions(String state) {
        List<String> list = new ArrayList<>();
        switch (state) {
            case "sit":
                if (SIT_COME2.get()) list.add("come2");
                if (SIT_HA.get()) list.add("ha");
                if (SIT_TASTETAIL.get()) list.add("tastetail");
                break;
            case "stand":
                if (STAND_CIRCLEDANCE.get()) list.add("circledance");
                if (STAND_QUESTION.get()) list.add("!??!");
                break;
            case "sleep":
                if (SLEEP_COME.get()) list.add("come");
                if (SLEEP_SLEEP2.get()) list.add("sleep2");
                if (SLEEP_SITUP.get()) list.add("situp");
                break;
        }
        return list;
    }
}
