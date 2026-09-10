package br.vapevlite.modules;

import br.vapevlite.BooleanSetting;
import br.vapevlite.Category;
import br.vapevlite.Module;
import br.vapevlite.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import org.lwjgl.input.Mouse;

import java.util.Random;

/** Elixe-style Aim Assist behavior, independently reimplemented for Forge 1.8.9. */
public class AimAssist extends Module {
    private final BooleanSetting player = new BooleanSetting("Player", true);
    private final BooleanSetting animal = new BooleanSetting("Animal", false);
    private final BooleanSetting monster = new BooleanSetting("Monster", false);
    private final BooleanSetting villager = new BooleanSetting("Villager", false);
    private final BooleanSetting yaw = new BooleanSetting("Yaw", true);
    private final BooleanSetting pitch = new BooleanSetting("Pitch", false);
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
    private EntityLivingBase lastTarget;

    public AimAssist() {
        super("Aim Assist", Category.COMBAT);
        addSetting(player); addSetting(animal); addSetting(monster); addSetting(villager);
        addSetting(yaw); addSetting(pitch); addSetting(speedFactor); addSetting(maxSpeed);
        addSetting(aimFov); addSetting(aimDistance); addSetting(randomYaw); addSetting(randomPitch);
        addSetting(randomSpeed); addSetting(randomDecrease); addSetting(randomDecreaseFov);
        addSetting(afterAttack); addSetting(requireVisibility); addSetting(requireSprint);
        addSetting(requireAttack); addSetting(requireWeapon); addSetting(ignoreNaked);
        addSetting(ignoreRightClick); addSetting(stopOnHitbox);
    }

    @Override public void onDisable() { lastAttackTime = 0L; lastTarget = null; }

    @Override
    public void onClientTick() {
        if (!isEnabled()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null) return;

        boolean attack = Mouse.isButtonDown(0) || mc.gameSettings.keyBindAttack.isKeyDown();
        if (attack) lastAttackTime = System.currentTimeMillis();
        long sinceAttack = System.currentTimeMillis() - lastAttackTime;

        if (requireAttack.getValue() && !attack) return;
        if (afterAttack.getValue() && sinceAttack > 500L) return;
        if (requireSprint.getValue() && !mc.thePlayer.isSprinting()) return;
        if (requireWeapon.getValue() && !isWeapon(mc)) return;
        if (ignoreRightClick.getValue() && Mouse.isButtonDown(1)) return;
        if (stopOnHitbox.getValue() && mc.objectMouseOver != null && mc.objectMouseOver.entityHit != null) return;

        EntityLivingBase target = findTarget(mc);
        if (target == null) return;
        lastTarget = target;

        float[] rotations = rotationsToTarget(mc.thePlayer, target);
        float yawDiff = MathHelper.wrapAngleTo180_float(rotations[0] - mc.thePlayer.rotationYaw);
        float pitchDiff = MathHelper.wrapAngleTo180_float(rotations[1] - mc.thePlayer.rotationPitch);
        if (Math.abs(yawDiff) > aimFov.getValue()) return;

        float randomYawOffset = randomYaw.getValue() ? signedRandom(randomSpeed.getValue().floatValue()) : 0F;
        float randomPitchOffset = randomPitch.getValue() ? signedRandom(randomSpeed.getValue().floatValue()) : 0F;
        if (randomDecrease.getValue() && Math.abs(yawDiff) >= randomDecreaseFov.getValue()) {
            randomYawOffset *= Math.abs(yawDiff) / randomDecreaseFov.getValue().floatValue();
            randomPitchOffset *= Math.abs(yawDiff) / randomDecreaseFov.getValue().floatValue();
        }

        float yawStep = yaw.getValue() ? clamp(yawDiff) : 0F;
        float pitchStep = pitch.getValue() ? clamp(pitchDiff) : 0F;
        float factor = speedFactor.getValue().floatValue();
        yawStep = (yawStep + randomYawOffset) * factor;
        pitchStep = (pitchStep + randomPitchOffset) * factor;

        // The real module changes the player's angles on its angle event; this is
        // the same calculation applied during the 1.8.9 client tick.
        mc.thePlayer.rotationYaw += yawStep;
        mc.thePlayer.rotationYawHead = mc.thePlayer.rotationYaw;
        mc.thePlayer.renderYawOffset = mc.thePlayer.rotationYaw;
        mc.thePlayer.rotationPitch = MathHelper.clamp_float(mc.thePlayer.rotationPitch + pitchStep, -90F, 90F);
    }

    private EntityLivingBase findTarget(Minecraft mc) {
        EntityLivingBase best = null;
        float bestDistance = aimDistance.getValue().floatValue();
        float bestAngle = Float.MAX_VALUE;
        for (Object o : mc.theWorld.loadedEntityList) {
            if (!(o instanceof EntityLivingBase)) continue;
            EntityLivingBase e = (EntityLivingBase)o;
            if (e == mc.thePlayer || e.isDead || e.getHealth() <= 0) continue;
            if (!allowed(e)) continue;
            float d = mc.thePlayer.getDistanceToEntity(e);
            if (d > bestDistance) continue;
            if (requireVisibility.getValue() && !mc.thePlayer.canEntityBeSeen(e)) continue;
            if (ignoreNaked.getValue() && e instanceof EntityPlayer && ((EntityPlayer)e).inventory.armorInventory[0] == null
                    && ((EntityPlayer)e).inventory.armorInventory[1] == null
                    && ((EntityPlayer)e).inventory.armorInventory[2] == null
                    && ((EntityPlayer)e).inventory.armorInventory[3] == null) continue;
            float angle = Math.abs(MathHelper.wrapAngleTo180_float(getYaw(mc.thePlayer, e) - mc.thePlayer.rotationYaw));
            if (angle <= aimFov.getValue() && angle < bestAngle) {
                bestAngle = angle;
                bestDistance = d;
                best = e;
            }
        }
        return best;
    }

    private boolean allowed(EntityLivingBase e) {
        if (e instanceof EntityPlayer) return player.getValue();
        if (e instanceof EntityVillager) return villager.getValue();
        if (e instanceof EntityAnimal) return animal.getValue();
        if (e instanceof EntityMob || e instanceof EntitySlime) return monster.getValue();
        return false;
    }

    private float[] rotationsToTarget(EntityLivingBase from, EntityLivingBase target) {
        double x = target.posX - from.posX;
        double y = target.posY + target.getEyeHeight() * 0.85D - (from.posY + from.getEyeHeight());
        double z = target.posZ - from.posZ;
        double horizontal = Math.sqrt(x * x + z * z);
        float yaw = (float)(Math.toDegrees(Math.atan2(z, x)) - 90.0D);
        float pitch = (float)(-Math.toDegrees(Math.atan2(y, horizontal)));
        return new float[] { yaw, pitch };
    }

    private float getYaw(EntityLivingBase from, EntityLivingBase to) { return rotationsToTarget(from, to)[0]; }
    private float clamp(float value) {
        float max = (float)Math.max(1D, maxSpeed.getValue());
        float a = Math.min(Math.abs(value), max);
        return value < 0F ? -a : a;
    }
    private float signedRandom(float speed) { return (random.nextBoolean() ? 1F : -1F) * random.nextFloat() * speed; }
    private boolean isWeapon(Minecraft mc) {
        if (mc.thePlayer.getHeldItem() == null) return false;
        return mc.thePlayer.getHeldItem().getItem() instanceof net.minecraft.item.ItemSword
                || mc.thePlayer.getHeldItem().getItem() instanceof net.minecraft.item.ItemAxe;
    }
}
