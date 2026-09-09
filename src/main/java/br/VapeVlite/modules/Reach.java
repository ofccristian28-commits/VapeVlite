package br.vapevlite.modules;

import br.vapevlite.Category;
import br.vapevlite.Module;
import br.vapevlite.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.lwjgl.input.Mouse;

public class Reach extends Module {
    private final NumberSetting range = new NumberSetting("Range", 3.5, 3.0, 6.0, 0.25);
    private long lastAttack;

    public Reach() {
        super("Reach", Category.COMBAT);
        addSetting(range);
    }

    @Override
    public void onClientTick() {
        if (!isEnabled()) return;
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.thePlayer;
        if (player == null || mc.theWorld == null || mc.currentScreen != null || !Mouse.isButtonDown(0)) return;
        if (range.getValue() <= 3.01D) return;
        long now = System.currentTimeMillis();
        if (now - lastAttack < 120) return;
        EntityLivingBase target = findTarget(mc, player, range.getValue());
        if (target == null) return;
        if (player.getDistanceToEntity(target) <= 3.0F) return;
        mc.playerController.attackEntity(player, target);
        player.swingItem();
        lastAttack = now;
    }

    private EntityLivingBase findTarget(Minecraft mc, EntityPlayerSP player, double reach) {
        Vec3 eyes = player.getPositionEyes(1.0F);
        Vec3 look = player.getLook(1.0F);
        Vec3 end = eyes.addVector(look.xCoord * reach, look.yCoord * reach, look.zCoord * reach);
        EntityLivingBase best = null;
        double bestDist = reach;
        for (Object o : mc.theWorld.loadedEntityList) {
            if (!(o instanceof EntityLivingBase)) continue;
            EntityLivingBase e = (EntityLivingBase)o;
            if (e == player || e.isDead || !player.canEntityBeSeen(e)) continue;
            AxisAlignedBB box = e.getEntityBoundingBox().expand(e.getCollisionBorderSize(), e.getCollisionBorderSize(), e.getCollisionBorderSize());
            MovingObjectPosition hit = box.calculateIntercept(eyes, end);
            if (hit != null) {
                double d = eyes.distanceTo(hit.hitVec);
                if (d < bestDist) { bestDist = d; best = e; }
            }
        }
        return best;
    }
}
