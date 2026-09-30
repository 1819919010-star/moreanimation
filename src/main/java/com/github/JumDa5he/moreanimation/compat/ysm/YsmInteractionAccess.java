package com.github.JumDa5he.moreanimation.compat.ysm;

import java.lang.reflect.Method;

/** 本轮交互复用 1.21 已验证的成员映射，不参与普通动画调度。 */
public final class YsmInteractionAccess {
    public static Method entity, model, bones, name, locator, location, bind, hidden, childrenHidden;
    public static final Method[] GET = new Method[9], SET = new Method[9];
    private static boolean ready;
    public static void initialize() throws ReflectiveOperationException {
        if (ready) return;
        String prefix = "com.elfmcys.yesstevemodel.";
        Class<?> base = Class.forName(prefix + "OoO0oo0o0o0oOoo0oOOO0Ooo");
        Class<?> runtime = Class.forName(prefix + "o0ooO0ooO00oo0o00Oo00000");
        Class<?> bone = Class.forName(prefix + "ooOO0OoOoO0o0o00oO0oo00o");
        entity = base.getMethod("ooo00OoO00OOOO0oOooOo0Oo");
        model = base.getMethod("O00OOOo00Oo0OO0000oOo0oo");
        bones = runtime.getMethod("OO000o0ooOooooOOOOO0Ooo0");
        name = bone.getMethod("OOO0oooOOo00OOooo0OooOOo");
        locator = runtime.getMethod("O0O0o0Oo0Oo00O0OooO00oOo");
        location = Class.forName(prefix + "oOoOoO0OoOoOOoOO00O000O0").getMethod("oO0O000o0oooOOO0O0oooOO0");
        bind = bone.getMethod("O0OO0O0o00o0o00oOoO0o0oO");
        hidden = bone.getMethod("OoOO0o0O00o00OoOO0OO0OOo");
        childrenHidden = bone.getMethod("O0oo0O0O0O0oO0oooOOo00o0");
        String[] components = {"oOo0OO0O0o000OO0O000oo0o", "oOoo00O0o0oO0o0oO00OO0O0", "OO000o0ooOooooOOOOO0Ooo0",
                "OOo0o0000Ooo0o00OO0oOOoO", "Oo0O0OoOo0O0oOoo0000O0oO", "O0o0OoOOooOo0O0OOoo0Oo00",
                "OoooO0OO0000O00oo0Oo00OO", "oOOO00ooO0oOOoOOo0OoOOOo", "ooOooOO0oO00o00o0o0oOOoO"};
        for (int i=0; i<9; i++) { GET[i]=bone.getMethod(components[i]); SET[i]=bone.getMethod(components[i],float.class); }
        ready=true;
    }
    private YsmInteractionAccess() {}
}
