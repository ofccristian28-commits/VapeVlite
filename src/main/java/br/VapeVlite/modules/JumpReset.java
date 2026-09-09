package br.vapevlite.modules;

import br.vapevlite.*;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class JumpReset extends Module {
    private int delay=2;
    public JumpReset(){super("Jump Reset",Category.COMBAT);}
    public int getDelay(){return delay;}
    public void setDelay(int v){delay=Math.max(0,Math.min(5,v));}
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent e){
        // Configuration hook. The module intentionally does not force movement
        // every tick; jump-reset input handling belongs in the client input layer.
        if(!isEnabled()||Minecraft.getMinecraft().thePlayer==null)return;
    }
}
