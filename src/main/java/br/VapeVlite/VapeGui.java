package br.vapevlite;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import java.io.IOException;
import java.util.List;

/** Compact Vape-inspired GUI using reference GUI PNG assets with independent code. */
public class VapeGui extends GuiScreen {
    private static final ResourceLocation COMBAT = new ResourceLocation("vapevlite", "newcombat.png");
    private static final ResourceLocation BIND = new ResourceLocation("vapevlite", "newbind.png");
    private static final ResourceLocation CLOSE = new ResourceLocation("vapevlite", "newclose.png");

    private Module selected;
    private boolean listening;
    private int x, y;
    private final int w = 420, h = 250;

    @Override public void initGui() {
        selected = VapeVlite.MODULES.getModules().isEmpty() ? null : VapeVlite.MODULES.getModules().get(0);
        x = (width-w)/2; y = (height-h)/2;
    }

    @Override public void drawScreen(int mouseX,int mouseY,float partialTicks) {
        drawGradientRect(0,0,width,height,0xC0080808,0xD0000000);
        drawRect(x,y,x+w,y+h,0xF0141418);
        drawRect(x,y,x+72,y+h,0xF00E0E12);
        drawRect(x+72,y,x+73,y+h,0xFF28282D);
        drawRect(x,y,x+w,y+29,0xF019191D);

        mc.getTextureManager().bindTexture(COMBAT);
        drawModalRectWithCustomSizedTexture(x+25,y+39,0,0,23,24,23,24);
        drawCenteredString(fontRendererObj,"Combat",x+36,y+66,0xFFFFFFFF);
        drawString(fontRendererObj,"VapeVlite",x+8,y+8,0xFFFFFFFF);

        mc.getTextureManager().bindTexture(CLOSE);
        drawModalRectWithCustomSizedTexture(x+w-23,y+5,0,0,18,18,32,32);

        int ly=y+79;
        for(Module m:VapeVlite.MODULES.getModules()) {
            boolean hov=mouseX>=x+7&&mouseX<=x+65&&mouseY>=ly-3&&mouseY<=ly+23;
            if(m==selected) drawRect(x+7,ly-3,x+65,ly+23,0xFF29292F);
            else if(hov) drawRect(x+7,ly-3,x+65,ly+23,0xFF1E1E23);
            drawCenteredString(fontRendererObj,shortName(m.getName()),x+36,ly+5,
                    m.isEnabled()?0xFFFFFFFF:0xFF8C8C94);
            ly+=26;
        }

        if(selected!=null) {
            int cx=x+88;
            drawString(fontRendererObj,selected.getName(),cx,y+39,0xFFFFFFFF);
            int tx=x+w-66;
            drawRect(tx,y+34,tx+50,y+52,selected.isEnabled()?0xFF7038D8:0xFF29292F);
            drawCenteredString(fontRendererObj,selected.isEnabled()?"ON":"OFF",tx+25,y+40,0xFFFFFFFF);

            mc.getTextureManager().bindTexture(BIND);
            drawModalRectWithCustomSizedTexture(cx,y+63,0,0,16,16,16,16);
            drawString(fontRendererObj,"Bind: "+keyName(selected.getKeybind()),cx+21,y+67,0xFFB9B9C1);

            int sy=y+91;
            for(Setting<?> s:selected.getSettings()) {
                if(sy>y+h-25) break;
                drawString(fontRendererObj,s.getId(),cx,sy+5,0xFFE2E2E6);
                if(s instanceof BooleanSetting) {
                    boolean on=((BooleanSetting)s).getValue();
                    drawRect(cx+205,sy,cx+253,sy+18,on?0xFF7038D8:0xFF29292F);
                    drawCenteredString(fontRendererObj,on?"ON":"OFF",cx+229,sy+5,0xFFFFFFFF);
                } else {
                    NumberSetting n=(NumberSetting)s;
                    drawRect(cx+154,sy,cx+173,sy+18,0xFF29292F);
                    drawRect(cx+235,sy,cx+254,sy+18,0xFF29292F);
                    drawCenteredString(fontRendererObj,"-",cx+163,sy+5,0xFFFFFFFF);
                    drawCenteredString(fontRendererObj,value(n),cx+213,sy+5,0xFFFFFFFF);
                    drawCenteredString(fontRendererObj,"+",cx+244,sy+5,0xFFFFFFFF);
                }
                sy+=26;
            }
        }
        if(listening) {
            drawRect(x+105,y+h-34,x+w-12,y+h-12,0xFF29292F);
            drawCenteredString(fontRendererObj,"Press a key...",x+w/2,y+h-28,0xFFFFFFFF);
        }
        super.drawScreen(mouseX,mouseY,partialTicks);
    }

    @Override protected void mouseClicked(int mx,int my,int button)throws IOException {
        if(button!=0){super.mouseClicked(mx,my,button);return;}
        if(mx>=x+w-28&&my>=y&&my<=y+30){mc.displayGuiScreen(null);return;}

        int ly=y+79;
        for(Module m:VapeVlite.MODULES.getModules()){
            if(mx>=x+7&&mx<=x+65&&my>=ly-3&&my<=ly+23){selected=m;return;}
            ly+=26;
        }
        if(selected==null)return;

        int tx=x+w-66;
        if(mx>=tx&&mx<=tx+50&&my>=y+34&&my<=y+54){
            selected.toggle();VapeVlite.save();return;
        }
        int cx=x+88;
        if(mx>=cx&&mx<=cx+150&&my>=y+60&&my<=y+88){listening=true;return;}

        int sy=y+91;
        for(Setting<?> s:selected.getSettings()){
            if(sy>y+h-25)break;
            if(s instanceof BooleanSetting){
                if(mx>=cx+195&&mx<=cx+260&&my>=sy&&my<=sy+20){
                    ((BooleanSetting)s).toggle();VapeVlite.save();return;
                }
            } else {
                NumberSetting n=(NumberSetting)s;
                if(my>=sy&&my<=sy+20){
                    if(mx>=cx+145&&mx<=cx+180)n.decrement();
                    else if(mx>=cx+228&&mx<=cx+260)n.increment();
                    else return;
                    VapeVlite.save();return;
                }
            }
            sy+=26;
        }
        super.mouseClicked(mx,my,button);
    }

    @Override protected void keyTyped(char c,int key)throws IOException {
        if(listening&&selected!=null){
            selected.setKeybind(key==Keyboard.KEY_ESCAPE?Keyboard.KEY_NONE:key);
            listening=false;VapeVlite.save();return;
        }
        if(key==Keyboard.KEY_ESCAPE){VapeVlite.save();mc.displayGuiScreen(null);return;}
        super.keyTyped(c,key);
    }
    @Override public void onGuiClosed(){VapeVlite.save();}

    private String shortName(String s){
        if(s.equals("AutoClicker"))return "Clicker";
        if(s.equals("Auto Tool"))return "AutoTool";
        if(s.equals("No Jump Delay"))return "NoJump";
        if(s.equals("No Hit Delay"))return "NoHit";
        return s.length()>9?s.substring(0,9):s;
    }
    private String value(NumberSetting n){
        double d=n.getValue();
        return d==Math.rint(d)?Integer.toString((int)d):String.format(java.util.Locale.US,"%.1f",d);
    }
    private String keyName(int k){return k<=0?"NONE":Keyboard.getKeyName(k);}
}