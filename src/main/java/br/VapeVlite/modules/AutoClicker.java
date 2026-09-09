package br.vapevlite.modules;

import br.vapevlite.BooleanSetting;
import br.vapevlite.Category;
import br.vapevlite.Module;
import br.vapevlite.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.input.Mouse;
import java.util.concurrent.ThreadLocalRandom;

/** Combat autoclicker: attacks the entity under the vanilla crosshair at a randomized CPS. */
public class AutoClicker extends Module {
    private final NumberSetting minCps = new NumberSetting("Min CPS", 15, 1, 20, 1);
    private final NumberSetting maxCps = new NumberSetting("Max CPS", 20, 1, 20, 1);
    private final BooleanSetting randomize = new BooleanSetting("Randomize", true);
    private long lastClick;
    private long nextDelay = 55L;

    public AutoClicker() {
        super("AutoClicker", Category.COMBAT);
        addSetting(minCps);
        addSetting(maxCps);
        addSetting(randomize);
    }

    @Override
    public void onClientTick() {
        if (!isEnabled()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null || !Mouse.isButtonDown(0)) return;

        long now = System.currentTimeMillis();
        if (now - lastClick < nextDelay) return;

        MovingObjectPosition hit = mc.objectMouseOver;
        if (hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY
                && hit.entityHit instanceof EntityLivingBase) {
            EntityLivingBase target = (EntityLivingBase) hit.entityHit;
            if (!target.isDead && mc.thePlayer.getDistanceToEntity(target) <= 6.0F) {
                mc.playerController.attackEntity(mc.thePlayer, target);
                mc.thePlayer.swingItem();
                lastClick = now;
                double cps = getNextCps();
                nextDelay = Math.max(1L, Math.round(1000D / cps));
            }
        }
    }

    private double getNextCps() {
        int min = (int)Math.round(Math.min(minCps.getValue(), maxCps.getValue()));
        int max = (int)Math.round(Math.max(minCps.getValue(), maxCps.getValue()));
        if (!randomize.getValue() || min == max) return min;
        return ThreadLocalRandom.current().nextDouble(min, max + 1.0D);
    }
}
