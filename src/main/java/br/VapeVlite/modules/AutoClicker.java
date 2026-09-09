package br.vapevlite.modules;

import br.vapevlite.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class AutoClicker extends Module {
    private int cps=10;
    private long last;
    public AutoClicker(){super("AutoClicker",Category.COMBAT);}
    public int getCps(){return cps;}
    public void setCps(int v){cps=Math.max(1,Math.min(20,v));}
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent e){
        if(!isEnabled() || Minecraft.getMinecraft().thePlayer==null)return;
        Minecraft mc=Minecraft.getMinecraft();
        if(!mc.gameSettings.keyBindAttack.isKeyDown())return;
        long now=System.currentTimeMillis(), delay=1000L/cps;
        if(now-last>=delay){
            KeyBinding.onTick(mc.gameSettings.keyBindAttack.getKeyCode());
            last=now;
        }
    }
}
