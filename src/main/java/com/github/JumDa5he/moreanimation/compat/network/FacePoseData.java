package com.github.JumDa5he.moreanimation.compat.network;

import net.minecraft.network.FriendlyByteBuf;

/** Compact renderer-independent target pose shared by Gecko and YSM face interaction. */
public record FacePoseData(byte grabMode, boolean faceOverstretch,
                           float headYaw, float headPitch, float headOffsetX, float headOffsetY,
                           float leftYaw, float leftPitch, float leftRoll,
                           float leftOffsetX, float leftOffsetY, float leftStretch,
                           float rightYaw, float rightPitch, float rightRoll,
                           float rightOffsetX, float rightOffsetY, float rightStretch) {
    public static final byte NONE = 0;
    public static final byte FACE = 1;
    public static final byte LEFT_EAR = 2;
    public static final byte RIGHT_EAR = 3;

    public static FacePoseData zero() {
        return new FacePoseData(NONE, false,
                0, 0, 0, 0,
                0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0);
    }

    public static FacePoseData read(FriendlyByteBuf buffer) {
        return new FacePoseData(buffer.readByte(), buffer.readBoolean(),
                buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeByte(grabMode);
        buffer.writeBoolean(faceOverstretch);
        buffer.writeFloat(headYaw);
        buffer.writeFloat(headPitch);
        buffer.writeFloat(headOffsetX);
        buffer.writeFloat(headOffsetY);
        buffer.writeFloat(leftYaw);
        buffer.writeFloat(leftPitch);
        buffer.writeFloat(leftRoll);
        buffer.writeFloat(leftOffsetX);
        buffer.writeFloat(leftOffsetY);
        buffer.writeFloat(leftStretch);
        buffer.writeFloat(rightYaw);
        buffer.writeFloat(rightPitch);
        buffer.writeFloat(rightRoll);
        buffer.writeFloat(rightOffsetX);
        buffer.writeFloat(rightOffsetY);
        buffer.writeFloat(rightStretch);
    }

    public boolean finite() {
        return Float.isFinite(headYaw) && Float.isFinite(headPitch)
                && Float.isFinite(headOffsetX) && Float.isFinite(headOffsetY)
                && Float.isFinite(leftYaw) && Float.isFinite(leftPitch) && Float.isFinite(leftRoll)
                && Float.isFinite(leftOffsetX) && Float.isFinite(leftOffsetY) && Float.isFinite(leftStretch)
                && Float.isFinite(rightYaw) && Float.isFinite(rightPitch) && Float.isFinite(rightRoll)
                && Float.isFinite(rightOffsetX) && Float.isFinite(rightOffsetY) && Float.isFinite(rightStretch);
    }
}
