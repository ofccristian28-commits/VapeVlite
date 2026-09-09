package br.vapevlite.modules;

import br.vapevlite.BooleanSetting;
import br.vapevlite.Category;
import br.vapevlite.Module;
import br.vapevlite.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.lwjgl.input.Mouse;
import java.util.concurrent.ThreadLocalRandom;

public class Reach extends Module {
    private final NumberSetting minRange = new NumberSetting("Min Reach", 3.2, 3.0, 6.0, 0.1);
    private final NumberSetting maxRange = new NumberSetting("Max Reach", 3.3, 3.0, 6.0, 0.1);
    private final BooleanSetting randomize = new BooleanSetting("Randomize", true);
    private final NumberSetting attackChance = new NumberSetting("Attack Chance", 100.0, 10.0, 100.0, 1.0);
    private long lastAttack;

    public Reach() {
        super("Reach", Category.COMBAT);
        addSetting(minRange);
        addSetting(maxRange);
        addSetting(randomize);
        addSetting(attackChance);
    }

    @Override
    public void onClientTick() {
        if (!isEnabled()) return;
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.thePlayer;
        if (player == null || mc.theWorld == null || mc.currentScreen != null || !Mouse.isButtonDown(0)) return;

        long now = System.currentTimeMillis();
        if (now - lastAttack < 100L) return;

        double low = Math.min(minRange.getValue(), maxRange.getValue());
        double high = Math.max(minRange.getValue(), maxRange.getValue());
        double reach = randomize.getValue() && high > low
                ? ThreadLocalRandom.current().nextDouble(low, high + 0.001D)
                : low;
        reach = Math.max(3.0D, Math.min(6.0D, reach));
        if (reach <= 3.01D) return;

        EntityLivingBase target = findTarget(mc, player, reach);
        if (target == null || player.getDistanceToEntity(target) <= 3.0F) return;

        // Chance to perform the attack on each valid attack attempt.
        double chance = Math.max(10.0D, Math.min(100.0D, attackChance.getValue()));
        if (ThreadLocalRandom.current().nextDouble(0.0D, 100.0D) >= chance) {
            lastAttack = now;
            return;
        }

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
