import java.lang.reflect.*;
public class OfficialBoneCheck {
    public static void main(String[] args) throws Exception {
        String p = "com.elfmcys.yesstevemodel.";
        Class<?> geo = Class.forName(p + "oO0OO0OooO0Oo0OoO0oOooo0");
        Object template = geo.getConstructors()[0].newInstance("Head", false, false, false, 0f, 0f, 0f, .1f, .2f, .3f);
        Class<?> bone = Class.forName(p + "OO0oo000o00O0O0oo00oO000");
        float[] first = new float[12], second = new float[12];
        var constructor = bone.getConstructor(geo, float[].class, int.class, float[].class, int.class);
        Object a = constructor.newInstance(template, first, 0, new float[4], 0);
        constructor.newInstance(template, second, 0, new float[4], 0);
        String[] methods = {"Oo0Oo0o00O00Oo0OOoOOoooo","o0OOooo0o0OO00OoOOOo0o0O","O00OOOooOoooOoo0o0o0oO0O",
            "oOOOo0OOO0ooooo0O00OO0o0","OOOOo0O0oO0OOo0O0O0Oo0O0","Ooooo0oooO0oooOOOoO0000O",
            "oo0OoO00oOoo000O0000o0oo","oooooooOOoOOoO00OooOo00O","Oo00o0OooOOo0ooOoo0oO0o0"};
        float[] original = first.clone(), other = second.clone();
        for (int i=0;i<9;i++) {
            Method setter = bone.getMethod(methods[i], float.class);
            setter.invoke(a, 20f+i);
            if(first[i] != 20f+i || (float)bone.getMethod(methods[i]).invoke(a) != 20f+i) throw new AssertionError("Wrong buffer component " + i);
            setter.invoke(a, original[i]);
        }
        if(!java.util.Arrays.equals(first, original) || !java.util.Arrays.equals(second, other)) throw new AssertionError("Restore/isolation failure");
        if(!bone.getMethod("oOOo0Ooo0oOoo0O0OOOOo0oo").invoke(a).equals("Head")) throw new AssertionError("Bone name mismatch");
        System.out.println("PASS: supplied official YSM bone class; nine buffer accessors, restore and separate instance buffers. Does not test Minecraft render/mixin lifecycle.");
    }
}
