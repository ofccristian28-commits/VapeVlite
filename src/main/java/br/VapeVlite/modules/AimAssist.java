package br.vapevlite.modules;

import br.vapevlite.Category;
import br.vapevlite.Module;
import br.vapevlite.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;

public class AimAssist extends Module {
    private final NumberSetting range = new NumberSetting("Range", 4.0, 1.0, 6.0, 0.5);
    private final NumberSetting speed = new NumberSetting("Speed", 2.0, 0.5, 10.0, 0.5);

    public AimAssist() {
        super("Aim Assist", Category.COMBAT);
        addSetting(range);
        addSetting(speed);
    }

    @Override
    public void onClientTick() {
        if (!isEnabled()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null) return;
        if (!mc.gameSettings.keyBindAttack.isKeyDown()) return;

        EntityLivingBase target = null;
        double best = range.getValue();
        for (Object o : mc.theWorld.loadedEntityList) {
            if (!(o instanceof EntityLivingBase)) continue;
            EntityLivingBase e = (EntityLivingBase)o;
            if (e == mc.thePlayer || e.isDead || !mc.thePlayer.canEntityBeSeen(e)) continue;
            double d = mc.thePlayer.getDistanceToEntity(e);
            if (d < best) { best = d; target = e; }
        }
        if (target == null) return;

        double dx = target.posX - mc.thePlayer.posX;
        double dz = target.posZ - mc.thePlayer.posZ;
        double dy = target.posY + target.getEyeHeight() - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        float wantedYaw = (float)(Math.atan2(dz, dx) * 180D / Math.PI) - 90F;
        float wantedPitch = (float)-(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * 180D / Math.PI);
        float yawDiff = MathHelper.wrapAngleTo180_float(wantedYaw - mc.thePlayer.rotationYaw);
        float pitchDiff = MathHelper.wrapAngleTo180_float(wantedPitch - mc.thePlayer.rotationPitch);
        float factor = (float)Math.min(1D, speed.getValue() / 10D);
        mc.thePlayer.rotationYaw += yawDiff * factor;
        mc.thePlayer.rotationPitch += pitchDiff * factor * 0.55F;
    }
}
