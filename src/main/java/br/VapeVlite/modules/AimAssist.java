package br.vapevlite.modules;

import br.vapevlite.BooleanSetting;
import br.vapevlite.Category;
import br.vapevlite.Module;
import br.vapevlite.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemSword;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Mouse;
import java.util.Random;

/** Elixe-style Aim Assist reimplemented for VapeVlite's simple module API. */
public class AimAssist extends Module {
    private final NumberSetting speedFactor = new NumberSetting("Speed Factor", 0.8, 0, 1, 0.05);
    private final NumberSetting maxSpeed = new NumberSetting("Max Speed", 5, 1, 40, 0.5);
    private final NumberSetting aimFov = new NumberSetting("Aim FOV", 20, 1, 90, 1);
    private final NumberSetting aimDistance = new NumberSetting("Aim Distance", 5, 0, 10, 0.1);
    private final NumberSetting randomSpeed = new NumberSetting("Random Speed", 5, 1, 40, 0.5);
    private final NumberSetting randomDecreaseFov = new NumberSetting("Random Decrease FOV", 5, 1, 90, 1);
    private final BooleanSetting randomDecrease = new BooleanSetting("Random Decrease", false);
    private final BooleanSetting afterAttack = new BooleanSetting("After Attack", false);
    private final BooleanSetting requireVisibility = new BooleanSetting("Require Visibility", false);
    private final BooleanSetting requireSprint = new BooleanSetting("Require Sprint", false);
    private final BooleanSetting requireAttack = new BooleanSetting("Require Attack Button", false);
    private final BooleanSetting requireWeapon = new BooleanSetting("Require Weapon", false);
    private final BooleanSetting ignoreNaked = new BooleanSetting("Ignore Naked", false);
    private final BooleanSetting ignoreRightClick = new BooleanSetting("Ignore On Right Click", false);
    private final BooleanSetting stopOnHitbox = new BooleanSetting("Stop On Hitbox", false);

    private final Random random = new Random();
    private long lastAttack;

    public AimAssist() {
        super("Aim Assist", Category.COMBAT);
        addSetting(speedFactor); addSetting(maxSpeed); addSetting(aimFov); addSetting(aimDistance);
        addSetting(randomSpeed); addSetting(randomDecreaseFov); addSetting(randomDecrease);
        addSetting(afterAttack); addSetting(requireVisibility); addSetting(requireSprint);
        addSetting(requireAttack); addSetting(requireWeapon); addSetting(ignoreNaked);
        addSetting(ignoreRightClick); addSetting(stopOnHitbox);
    }

    @Override
    protected void onDisable() { lastAttack = 0L; }

    @Override
    public void onClientTick() {
        if (!isEnabled()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null) return;

        boolean attack = Mouse.isButtonDown(0);
        if (attack) lastAttack = System.currentTimeMillis();
        boolean recentlyAttacked = System.currentTimeMillis() - lastAttack <= 350L;

        if (requireAttack.getValue() && !attack && !recentlyAttacked) return;
        if (afterAttack.getValue() && !recentlyAttacked) return;
        if (requireSprint.getValue() && !mc.thePlayer.isSprinting()) return;
        if (requireWeapon.getValue() && !isHoldingWeapon(mc)) return;
        if (ignoreRightClick.getValue() && Mouse.isButtonDown(1)) return;
        if (stopOnHitbox.getValue() && mc.objectMouseOver != null
                && mc.objectMouseOver.typeOfHit == net.minecraft.util.MovingObjectPosition.MovingObjectType.ENTITY) return;

        EntityPlayer target = getClosestPlayer(mc);
        if (target == null) return;

        setAngles(mc, target);
    }

    private EntityPlayer getClosestPlayer(Minecraft mc) {
        EntityPlayer best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Object o : mc.theWorld.playerEntities) {
            if (!(o instanceof EntityPlayer)) continue;
            EntityPlayer p = (EntityPlayer)o;
            if (!isValid(mc, p)) continue;
            double d = mc.thePlayer.getDistanceToEntity(p);
            if (d < bestDistance) { bestDistance = d; best = p; }
        }
        return best;
    }

    private boolean isValid(Minecraft mc, EntityPlayer p) {
        if (p == mc.thePlayer || p.isDead) return false;
        if (mc.thePlayer.getDistanceToEntity(p) > aimDistance.getValue()) return false;
        float yaw = getYawTo(mc, p);
        float diff = Math.abs(MathHelper.wrapAngleTo180_float(yaw - mc.thePlayer.rotationYaw));
        if (diff > aimFov.getValue()) return false;
        if (requireVisibility.getValue() && !mc.thePlayer.canEntityBeSeen(p)) return false;
        if (ignoreNaked.getValue() && p.getHeldItem() == null) return false;
        return true;
    }

    private void setAngles(Minecraft mc, EntityPlayer target) {
        AxisAlignedBB box = target.getEntityBoundingBox();
        double tx = (box.minX + box.maxX) * 0.5D;
        double ty = box.minY + (box.maxY - box.minY) * 0.62D;
        double tz = (box.minZ + box.maxZ) * 0.5D;
        double dx = tx - mc.thePlayer.posX;
        double dy = ty - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double dz = tz - mc.thePlayer.posZ;
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float)(Math.atan2(dz, dx) * 180D / Math.PI) - 90F;
        float pitch = (float)-(Math.atan2(dy, horizontal) * 180D / Math.PI);
        float yawDiff = MathHelper.wrapAngleTo180_float(yaw - mc.thePlayer.rotationYaw);
        float pitchDiff = MathHelper.wrapAngleTo180_float(pitch - mc.thePlayer.rotationPitch);

        double speed = maxSpeed.getValue() * speedFactor.getValue();
        if (randomDecrease.getValue()) {
            float fov = Math.abs(yawDiff);
            if (fov <= randomDecreaseFov.getValue()) speed *= 0.5D + random.nextDouble() * 0.5D;
        }
        if (randomSpeed.getValue() > 0 && random.nextBoolean()) speed *= 0.85D + random.nextDouble() * 0.3D;
        speed = Math.min(maxSpeed.getValue(), Math.max(0D, speed));

        mc.thePlayer.rotationYaw += clampStep(yawDiff, (float)(speed * 0.1D));
        mc.thePlayer.rotationPitch = MathHelper.clamp_float(mc.thePlayer.rotationPitch + clampStep(pitchDiff, (float)(speed * 0.1D)), -90F, 90F);
    }

    private float clampStep(float difference, float step) {
        if (difference > step) return step;
        if (difference < -step) return -step;
        return difference;
    }

    private boolean isHoldingWeapon(Minecraft mc) {
        if (mc.thePlayer.getHeldItem() == null) return false;
        return mc.thePlayer.getHeldItem().getItem() instanceof ItemSword
                || mc.thePlayer.getHeldItem().getItem() instanceof ItemAxe;
    }

    private float getYawTo(Minecraft mc, EntityPlayer p) {
        return (float)(Math.atan2(p.posZ - mc.thePlayer.posZ, p.posX - mc.thePlayer.posX) * 180D / Math.PI) - 90F;
    }
}
