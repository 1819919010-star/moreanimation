package com.github.JumDa5he.moreanimation.groom;

import com.github.JumDa5he.moreanimation.client.ClientKeyMappings;
import com.github.JumDa5he.moreanimation.core.ModItems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;

public final class GroomingScreen extends Screen {
    public boolean brushing,hairFocus=true;
    private int selected=1;
    private boolean exitKeyHeld;
    private static final int VIEW_WIDTH=142, VIEW_TOP=76;
    private static final float YAW_RANGE=45, PITCH_RANGE=30;
    private float viewYaw,viewPitch;
    public float viewYaw(){return viewYaw;}
    public float viewPitch(){return viewPitch;}
    public GroomingScreen(){super(Component.translatable("screen.moreanimation.groom.title"));}
    @Override protected void init(){
        int code=ClientKeyMappings.GROOMING.getKey().getValue();exitKeyHeld=code>=0&&com.mojang.blaze3d.platform.InputConstants.isKeyDown(minecraft.getWindow().getWindow(),code);
        addRenderableWidget(Button.builder(Component.translatable("screen.moreanimation.groom.focus"),b->{hairFocus=!hairFocus;brushing=false;GroomingTails.release();GroomingClient.resetStroke();}).bounds(10,10,110,20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.moreanimation.groom.flip"),b->{brushing=false;GroomingClient.flip();}).bounds(125,10,85,20).build());
        addRenderableWidget(new ViewSlider(true,VIEW_TOP));
        addRenderableWidget(new ViewSlider(false,VIEW_TOP+26));
        addRenderableWidget(Button.builder(Component.translatable("screen.moreanimation.groom.end"),b->onClose()).bounds(width/2-50,height-28,100,20).build());
    }
    private Item item(int i){return switch(i){case 1->ModItems.GROOMING_BRUSH.get();case 2->Items.FLINT_AND_STEEL;case 3->Items.SHEARS;case 4->Items.SAND;default->Items.ICE;};}
    private int top(){return height/2-96;}
    public boolean brushSelected(){return selected==1;}
    public boolean mouseSelected(){return selected==0;}
    public boolean overTools(double x,double y){return x>width-58||y<35||y>height-35
            ||x>=6&&x<=14+VIEW_WIDTH&&y>=VIEW_TOP-18&&y<=VIEW_TOP+50;}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        g.drawString(font,title,10,36,0xFFFFFF,true);
        g.fill(6,VIEW_TOP-18,14+VIEW_WIDTH,VIEW_TOP+50,0x99303030);
        g.drawString(font,Component.translatable("screen.moreanimation.groom.view"),10,VIEW_TOP-14,0xFFFFFF,true);
        for(int i=0;i<6;i++){int x=width-42,y=top()+i*32;boolean has=i==0||GroomingSession.hasTool(minecraft.player,item(i));
            g.fill(x-3,y-3,x+23,y+23,selected==i?0xDDAACC66:0xBB333333);if(i==0){
                // 指针图标是操作模式，不占背包物品槽。
                for(int row=0;row<15;row++)g.fill(x+4,y+2+row,x+5+row/2,y+3+row,0xFFEEEEFF);
                g.fill(x+8,y+12,x+11,y+20,0xFFEEEEFF);
            }else g.renderItem(new ItemStack(item(i)),x+2,y+2);
            if(!has)g.fill(x,y,x+20,y+20,0xAA222222);
            if(mx>=x-3&&mx<x+23&&my>=y-3&&my<y+23)g.renderTooltip(font,Component.translatable(i==0?"screen.moreanimation.groom.mouse":!has?"screen.moreanimation.groom.missing":i>1?"screen.moreanimation.groom.todo":"screen.moreanimation.groom.brush"),mx-110,my);
        }
        g.drawCenteredString(font,Component.translatable(mouseSelected()?"screen.moreanimation.groom.mouse_help":"screen.moreanimation.groom.help"),width/2,height-43,0xFFFFFF);
        if(GroomingClient.debug)GroomingClient.drawEarDebug(g);
        if(GroomingClient.debug)g.drawString(font,"F8 · "+GroomingClient.debugPart(),10,52,0x66FFCC,true);
        super.render(g,mx,my,partial);
    }
    @Override public boolean mouseClicked(double x,double y,int button){
        if(super.mouseClicked(x,y,button)){brushing=false;GroomingTails.release();GroomingClient.resetStroke();return true;}
        if(x>width-58){
            if(button==0){int i=(int)java.lang.Math.floor((y-top()+3)/32);if(i>=0&&i<6){selected=i;GroomingClient.selectedTool(brushSelected());}}
            brushing=false;GroomingTails.release();GroomingClient.resetStroke();return true;
        }
        if(overTools(x,y)||GroomingClient.inputBlocked())return true;
        if(mouseSelected()){
            if(button==0)GroomingTails.begin(x,y);
            if(button==1)GroomingTails.toggle(x,y);
        }else if(button==0&&brushSelected())brushing=true;
        return true;
    }
    @Override public boolean mouseReleased(double x,double y,int button){super.mouseReleased(x,y,button);if(button==0){brushing=false;GroomingTails.release(x,y);GroomingClient.resetStroke();}return true;}
    private final class ViewSlider extends AbstractSliderButton {
        private final boolean horizontal;
        ViewSlider(boolean horizontal,int y){
            super(10,y,VIEW_WIDTH,20,Component.empty(),.5+(horizontal?viewYaw/YAW_RANGE:viewPitch/PITCH_RANGE)*.5);
            this.horizontal=horizontal;updateMessage();
        }
        @Override protected void updateMessage(){
            int angle=(int)Math.round((value*2-1)*(horizontal?YAW_RANGE:PITCH_RANGE));
            setMessage(Component.translatable(horizontal?"screen.moreanimation.groom.view_horizontal":"screen.moreanimation.groom.view_vertical",angle));
        }
        @Override protected void applyValue(){
            // 调视角先结束输入，必须重新按下才能开始下一笔，避免相机变化造成虚假刷动。
            brushing=false;GroomingTails.release();GroomingClient.resetStroke();
            if(horizontal)viewYaw=(float)((value*2-1)*YAW_RANGE);
            else viewPitch=(float)((value*2-1)*PITCH_RANGE);
        }
    }
    @Override public boolean keyPressed(int key,int scan,int mods){
        if(key==256){onClose();return true;}
        if(ClientKeyMappings.GROOMING.matches(key,scan)){if(!exitKeyHeld){exitKeyHeld=true;onClose();}return true;}
        if(key==297){GroomingClient.debug=!GroomingClient.debug;return true;}
        return super.keyPressed(key,scan,mods);
    }
    @Override public boolean keyReleased(int key,int scan,int mods){if(ClientKeyMappings.GROOMING.matches(key,scan)){exitKeyHeld=false;return true;}return super.keyReleased(key,scan,mods);}
    @Override public void onClose(){brushing=false;GroomingTails.release();GroomingClient.resetStroke();GroomingClient.send(1);minecraft.setScreen(null);}
    @Override public void renderBackground(net.minecraft.client.gui.GuiGraphics graphics,int mouseX,int mouseY,float partialTick) {}
    @Override public boolean isPauseScreen(){return false;}
}
