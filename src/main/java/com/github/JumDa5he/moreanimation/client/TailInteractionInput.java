package com.github.JumDa5he.moreanimation.client;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * 尾巴互动界面的输入来源。
 *
 * <p>默认用真实鼠标；外部模组（例如摄像头手部追踪）可以通过 {@link #setSource(Source)}
 * 注册一个「虚拟指针」，界面就会改用它来定位、抓取和松开。
 *
 * <p>这里刻意不引用任何外部模组的类，moreanimation 单独编译/发布都不受影响；
 * 没人注册时行为跟以前完全一致。
 */
public final class TailInteractionInput {

    /** 虚拟指针的数据来源。坐标是 GUI 缩放坐标，和 render 里的 mouseX / mouseY 同一套。 */
    public interface Source {
        /** 当前有没有可用的指针位置；返回 false 时界面退回真实鼠标。 */
        boolean hasPointer();

        double pointerX();

        double pointerY();

        /** 抓取键（相当于鼠标左键）是否按下。 */
        boolean primaryDown();
    }

    private static volatile Source source;

    private TailInteractionInput() {
    }

    /** 注册虚拟指针来源，传 null 表示恢复成真实鼠标。 */
    public static void setSource(Source newSource) {
        source = newSource;
    }

    public static Source source() {
        return source;
    }

    /** 当前是否正在使用虚拟指针。 */
    public static boolean hasVirtualPointer() {
        Source current = source;
        return current != null && current.hasPointer();
    }

    public static int pointerXInt() {
        Source current = source;
        return current == null ? 0 : (int) Math.round(current.pointerX());
    }

    public static int pointerYInt() {
        Source current = source;
        return current == null ? 0 : (int) Math.round(current.pointerY());
    }

    /** 抓取键状态：有虚拟指针就用它，否则看真实鼠标左键。 */
    public static boolean isPrimaryDown(Minecraft minecraft) {
        Source current = source;
        if (current != null && current.hasPointer()) {
            return current.primaryDown();
        }
        return GLFW.glfwGetMouseButton(minecraft.getWindow().getWindow(),
                GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
    }
}