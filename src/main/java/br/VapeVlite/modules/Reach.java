package br.vapevlite.modules;

import br.vapevlite.*;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class Reach extends Module {
    private double range=3.0;
    public Reach(){super("Reach",Category.COMBAT);}
    public double getRange(){return range;}
    public void setRange(double v){range=Math.max(3.0,Math.min(6.0,v));}
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent e){
        if(!isEnabled()||Minecraft.getMinecraft().thePlayer==null)return;
        // Attack reach is handled by the client ray-trace layer in the GUI/client.
    }
}
