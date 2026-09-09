package br.vapevlite.modules;

import br.vapevlite.BooleanSetting;
import br.vapevlite.Category;
import br.vapevlite.Module;
import br.vapevlite.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;

public class AimAssist extends Module {
    private final NumberSetting range = new NumberSetting("Range", 4.5, 1.0, 6.0, 0.1);
    private final NumberSetting speed = new NumberSetting("Speed", 6.0, 0.5, 10.0, 0.5);
    private final BooleanSetting sticky = new BooleanSetting("Sticky", true);
    private EntityLivingBase lockedTarget;
    private long lockUntil;

    public AimAssist() {
        super("Aim Assist", Category.COMBAT);
        addSetting(range);
        addSetting(speed);
        addSetting(sticky);
    }

    @Override
    protected void onDisable() {
        lockedTarget = null;
        lockUntil = 0L;
    }

    @Override
    public void onClientTick() {
        if (!isEnabled()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null) return;
        if (!mc.gameSettings.keyBindAttack.isKeyDown()) {
            lockedTarget = null;
            return;
        }

        long now = System.currentTimeMillis();
        if (!isValid(mc, lockedTarget) || !sticky.getValue() || now > lockUntil) {
            lockedTarget = findBestTarget(mc);
            lockUntil = now + 650L;
        }
        if (lockedTarget == null) return;

        double dx = lockedTarget.posX - mc.thePlayer.posX;
        double dz = lockedTarget.posZ - mc.thePlayer.posZ;
        double centerY = lockedTarget.posY + lockedTarget.height * 0.62D;
        double dy = centerY - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        float wantedYaw = (float)(Math.atan2(dz, dx) * 180D / Math.PI) - 90F;
        float wantedPitch = (float)-(Math.atan2(dy, horizontal) * 180D / Math.PI);
        float yawDiff = MathHelper.wrapAngleTo180_float(wantedYaw - mc.thePlayer.rotationYaw);
        float pitchDiff = MathHelper.wrapAngleTo180_float(wantedPitch - mc.thePlayer.rotationPitch);

        float factor = (float)Math.min(1D, speed.getValue() / 8D);
        if (Math.abs(yawDiff) > 0.25F) mc.thePlayer.rotationYaw += yawDiff * factor;
        if (Math.abs(pitchDiff) > 0.25F) mc.thePlayer.rotationPitch += pitchDiff * factor * 0.75F;
    }

    private EntityLivingBase findBestTarget(Minecraft mc) {
        EntityLivingBase best = null;
        double bestScore = Double.MAX_VALUE;
        double maxRange = range.getValue();
        for (Object o : mc.theWorld.loadedEntityList) {
            if (!(o instanceof EntityLivingBase)) continue;
            EntityLivingBase e = (EntityLivingBase)o;
            if (!isValid(mc, e)) continue;
            double d = mc.thePlayer.getDistanceToEntity(e);
            if (d > maxRange) continue;
            double yaw = Math.abs(MathHelper.wrapAngleTo180_float(getYawTo(mc, e) - mc.thePlayer.rotationYaw));
            double score = d + yaw * 0.025D;
            if (score < bestScore) {
                bestScore = score;
                best = e;
            }
        }
        return best;
    }

    private boolean isValid(Minecraft mc, EntityLivingBase e) {
        return e != null && e != mc.thePlayer && !e.isDead &&
                mc.thePlayer.getDistanceToEntity(e) <= range.getValue() + 0.25D &&
                mc.thePlayer.canEntityBeSeen(e);
    }

    private float getYawTo(Minecraft mc, EntityLivingBase e) {
        return (float)(Math.atan2(e.posZ - mc.thePlayer.posZ, e.posX - mc.thePlayer.posX) * 180D / Math.PI) - 90F;
    }
}
