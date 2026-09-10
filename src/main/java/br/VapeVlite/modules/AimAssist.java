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

/**
 * Client-side aim assist, independently reimplemented for Minecraft 1.8.9.
 * Defaults follow the Elixe 8.0-style settings: yaw only, 20 FOV and 5 blocks.
 */
public class AimAssist extends Module {
    private final BooleanSetting player = new BooleanSetting("Player", true);
    private final BooleanSetting animal = new BooleanSetting("Animal", false);
    private final BooleanSetting monster = new BooleanSetting("Monster", false);
    private final BooleanSetting villager = new BooleanSetting("Villager", false);

    private final BooleanSetting yawRotation = new BooleanSetting("Yaw", true);
    private final BooleanSetting pitchRotation = new BooleanSetting("Pitch", false);
    private final NumberSetting speedFactor = new NumberSetting("Speed Factor", 0.8, 0, 1, 0.05);
    private final NumberSetting maxSpeed = new NumberSetting("Max Speed", 5, 1, 40, 0.5);
    private final NumberSetting aimFov = new NumberSetting("Aim FOV", 20, 1, 90, 1);
    private final NumberSetting aimDistance = new NumberSetting("Aim Distance", 5, 0, 10, 0.1);

    private final BooleanSetting randomYaw = new BooleanSetting("Random Yaw", true);
    private final BooleanSetting randomPitch = new BooleanSetting("Random Pitch", false);
    private final NumberSetting randomSpeed = new NumberSetting("Random Speed", 5, 1, 40, 0.5);
    private final BooleanSetting randomDecrease = new BooleanSetting("Random Decrease", false);
    private final NumberSetting randomDecreaseFov = new NumberSetting("Random Decrease FOV", 5, 1, 90, 1);

    private final BooleanSetting afterAttack = new BooleanSetting("After Attack", false);
    private final BooleanSetting requireVisibility = new BooleanSetting("Require Visibility", false);
    private final BooleanSetting requireSprint = new BooleanSetting("Require Sprint", false);
    private final BooleanSetting requireAttack = new BooleanSetting("Require Attack Button", false);
    private final BooleanSetting requireWeapon = new BooleanSetting("Require Weapon", false);
    private final BooleanSetting ignoreNaked = new BooleanSetting("Ignore Naked", false);
    private final BooleanSetting ignoreRightClick = new BooleanSetting("Ignore On Right Click", false);
    private final BooleanSetting stopOnHitbox = new BooleanSetting("Stop On Hitbox", false);

    private final Random random = new Random();
    private long lastAttackTime;

    public AimAssist() {
        super("Aim Assist", Category.COMBAT);
        addSetting(player); addSetting(animal); addSetting(monster); addSetting(villager);
        addSetting(yawRotation); addSetting(pitchRotation);
        addSetting(speedFactor); addSetting(maxSpeed); addSetting(aimFov); addSetting(aimDistance);
        addSetting(randomYaw); addSetting(randomPitch); addSetting(randomSpeed);
        addSetting(randomDecrease); addSetting(randomDecreaseFov);
        addSetting(afterAttack); addSetting(requireVisibility); addSetting(requireSprint);
        addSetting(requireAttack); addSetting(requireWeapon); addSetting(ignoreNaked);
        addSetting(ignoreRightClick); addSetting(stopOnHitbox);
    }

    @Override
    protected void onDisable() {
        lastAttackTime = 0L;
    }

    @Override
    public void onClientTick() {
        if (!isEnabled()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null) return;

        long now = System.currentTimeMillis();
        boolean attackDown = Mouse.isButtonDown(0) || mc.gameSettings.keyBindAttack.isKeyDown();
        if (attackDown) lastAttackTime = now;
        boolean recentlyAttacked = now - lastAttackTime <= 350L;

        if (requireAttack.getValue() && !attackDown && !recentlyAttacked) return;
        if (afterAttack.getValue() && !recentlyAttacked) return;
        if (requireSprint.getValue() && !mc.thePlayer.isSprinting()) return;
        if (requireWeapon.getValue() && !isHoldingWeapon(mc)) return;
        if (ignoreRightClick.getValue() && Mouse.isButtonDown(1)) return;
        if (stopOnHitbox.getValue() && mc.objectMouseOver != null
                && mc.objectMouseOver.entityHit instanceof EntityPlayer) return;

        EntityPlayer target = findTarget(mc);
        if (target == null) return;

        aimAt(mc, target);
    }

    private EntityPlayer findTarget(Minecraft mc) {
        EntityPlayer best = null;
        double bestScore = Double.MAX_VALUE;
        double maxDistance = Math.max(0D, aimDistance.getValue());
        double maxDistanceSq = maxDistance * maxDistance;

        for (Object object : mc.theWorld.playerEntities) {
            if (!(object instanceof EntityPlayer)) continue;
            EntityPlayer target = (EntityPlayer) object;
            if (!isValidTarget(mc, target, maxDistanceSq)) continue;

            float yaw = getYawTo(mc, target);
            float yawDiff = Math.abs(MathHelper.wrapAngleTo180_float(yaw - mc.thePlayer.rotationYaw));
            double distanceSq = mc.thePlayer.getDistanceSqToEntity(target);
            // Prefer targets near the crosshair, then by distance.
            double score = yawDiff * 3.0D + Math.sqrt(distanceSq);
            if (score < bestScore) {
                bestScore = score;
                best = target;
            }
        }
        return best;
    }

    private boolean isValidTarget(Minecraft mc, EntityPlayer target, double maxDistanceSq) {
        if (target == mc.thePlayer || target.isDead) return false;
        if (!player.getValue()) return false;
        if (mc.thePlayer.getDistanceSqToEntity(target) > maxDistanceSq) return false;
        if (requireVisibility.getValue() && !mc.thePlayer.canEntityBeSeen(target)) return false;
        if (ignoreNaked.getValue() && isNaked(target)) return false;

        float yaw = getYawTo(mc, target);
        float yawDiff = Math.abs(MathHelper.wrapAngleTo180_float(yaw - mc.thePlayer.rotationYaw));
        return yawDiff <= aimFov.getValue();
    }

    private void aimAt(Minecraft mc, EntityPlayer target) {
        AxisAlignedBB box = target.getEntityBoundingBox();
        double tx = (box.minX + box.maxX) * 0.5D;
        double ty = box.minY + (box.maxY - box.minY) * 0.62D;
        double tz = (box.minZ + box.maxZ) * 0.5D;

        double dx = tx - mc.thePlayer.posX;
        double dy = ty - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double dz = tz - mc.thePlayer.posZ;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal < 0.0001D) return;

        float targetYaw = (float) (Math.atan2(dz, dx) * 180D / Math.PI) - 90F;
        float targetPitch = (float) -(Math.atan2(dy, horizontal) * 180D / Math.PI);
        float yawDiff = MathHelper.wrapAngleTo180_float(targetYaw - mc.thePlayer.rotationYaw);
        float pitchDiff = targetPitch - mc.thePlayer.rotationPitch;

        double speed = Math.max(0D, Math.min(maxSpeed.getValue(), maxSpeed.getValue() * speedFactor.getValue()));

        if (randomDecrease.getValue() && Math.abs(yawDiff) <= randomDecreaseFov.getValue()) {
            speed *= 0.55D + random.nextDouble() * 0.45D;
        }

        if (randomSpeed.getValue() > 0) {
            double amount = Math.min(1D, randomSpeed.getValue() / 40D);
            speed *= 1D - amount * 0.20D + random.nextDouble() * amount * 0.40D;
        }

        // Elixe-style speed values are degrees per client tick, not tenths of a degree.
        float step = (float) Math.max(0.05D, speed);

        if (yawRotation.getValue()) {
            float wantedYaw = yawDiff;
            if (randomYaw.getValue()) wantedYaw += (random.nextFloat() - 0.5F) * 0.20F;
            mc.thePlayer.rotationYaw += clampStep(wantedYaw, step);
        }

        if (pitchRotation.getValue()) {
            float wantedPitch = pitchDiff;
            if (randomPitch.getValue()) wantedPitch += (random.nextFloat() - 0.5F) * 0.15F;
            mc.thePlayer.rotationPitch = MathHelper.clamp_float(
                    mc.thePlayer.rotationPitch + clampStep(wantedPitch, step), -90F, 90F);
        }
    }

    private static float clampStep(float difference, float step) {
        if (difference > step) return step;
        if (difference < -step) return -step;
        return difference;
    }

    private static boolean isHoldingWeapon(Minecraft mc) {
        if (mc.thePlayer.getHeldItem() == null) return false;
        return mc.thePlayer.getHeldItem().getItem() instanceof ItemSword
                || mc.thePlayer.getHeldItem().getItem() instanceof ItemAxe;
    }

    private static boolean isNaked(EntityPlayer player) {
        for (int i = 0; i < 4; i++) {
            if (player.inventory.armorInventory[i] != null) return false;
        }
        return true;
    }

    private static float getYawTo(Minecraft mc, EntityPlayer target) {
        return (float) (Math.atan2(target.posZ - mc.thePlayer.posZ,
                target.posX - mc.thePlayer.posX) * 180D / Math.PI) - 90F;
    }
}
