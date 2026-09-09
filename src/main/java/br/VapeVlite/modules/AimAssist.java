package br.vapevlite.modules;

import br.vapevlite.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class AimAssist extends Module {
    private double range=4.0;
    private float speed=2.0f;
    public AimAssist(){super("Aim Assist",Category.COMBAT);}
    public double getRange(){return range;}
    public void setRange(double v){range=Math.max(1,Math.min(6,v));}
    public float getSpeed(){return speed;}
    public void setSpeed(float v){speed=Math.max(.1f,Math.min(10,v));}

    @SubscribeEvent public void tick(TickEvent.ClientTickEvent e){
        Minecraft mc=Minecraft.getMinecraft();
        if(!isEnabled()||mc.thePlayer==null||mc.theWorld==null)return;
        EntityLivingBase target=null; double best=range;
        for(Object o:mc.theWorld.loadedEntityList){
            if(!(o instanceof EntityLivingBase)||o==mc.thePlayer)continue;
            EntityLivingBase en=(EntityLivingBase)o;
            if(en.isDead)continue;
            double d=mc.thePlayer.getDistanceToEntity(en);
            if(d<best){best=d;target=en;}
        }
        if(target==null)return;
        double dx=target.posX-mc.thePlayer.posX;
        double dz=target.posZ-mc.thePlayer.posZ;
        float desired=(float)(Math.atan2(dz,dx)*180/Math.PI)-90f;
        float diff=wrap(desired-mc.thePlayer.rotationYaw);
        mc.thePlayer.rotationYaw += diff * (speed/10f);
    }
    private float wrap(float a){while(a>180)a-=360;while(a<-180)a+=360;return a;}
}

