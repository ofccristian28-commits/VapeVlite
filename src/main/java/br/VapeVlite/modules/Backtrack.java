package br.vapevlite.modules;

import br.vapevlite.*;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class Backtrack extends Module {
    private int delay=100;
    public Backtrack(){super("Backtrack",Category.COMBAT);}
    public int getDelay(){return delay;}
    public void setDelay(int v){delay=Math.max(0,Math.min(1000,v));}
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent e){
        if(!isEnabled()||Minecraft.getMinecraft().thePlayer==null)return;
        // Network packet buffering/interpolation must be implemented at the
        // network layer; this class keeps the setting isolated and safe.
    }
}
