package br.vapevlite.modules;

import br.vapevlite.BooleanSetting;
import br.vapevlite.Category;
import br.vapevlite.Module;
import br.vapevlite.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Mouse;

/**
 * Independent recreation of the target-selection/rotation style observed in CrewX.
 * It does not copy the original bytecode.
 */
public class AimAssist extends Module {
    private final NumberSetting horizontalSpeed = new NumberSetting("Horizontal Speed", 3.0, 0.0, 10.0, 0.5);
    private final NumberSetting verticalSpeed = new NumberSetting("Vertical Speed", 0.0, 0.0, 10.0, 0.5);
    private final NumberSetting smoothing = new NumberSetting("Smoothing", 50.0, 0.0, 100.0, 5.0);
    private final NumberSetting range = new NumberSetting("Range", 4.5, 3.0, 8.0, 0.1);
    private final NumberSetting fov = new NumberSetting("FOV", 90.0, 30.0, 360.0, 5.0);
    private final BooleanSetting weaponsOnly = new BooleanSetting("Weapons Only", true);
    private final BooleanSetting allowTools = new BooleanSetting("Allow Tools", false);
    private final BooleanSetting teams = new BooleanSetting("Teams", true);

    private long lastAttackInput;
    private EntityPlayer target;

    public AimAssist() {
        super("Aim Assist", Category.COMBAT);
        addSetting(horizontalSpeed);
        addSetting(verticalSpeed);
        addSetting(smoothing);
        addSetting(range);
        addSetting(fov);
        addSetting(weaponsOnly);
        addSetting(allowTools);
        addSetting(teams);
    }

    @Override
    protected void onDisable() {
        target = null;
        lastAttackInput = 0L;
    }

    @Override
    public void onClientTick() {
        if (!isEnabled()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null) {
            target = null;
            return;
        }

        if (Mouse.isButtonDown(0)) lastAttackInput = System.currentTimeMillis();
        if (weaponsOnly.getValue() && !isHoldingAllowedWeapon(mc)) {
            target = null;
            return;
        }

        // CrewX-style short assist window after attack input.
        if (!Mouse.isButtonDown(0) && System.currentTimeMillis() - lastAttackInput > 350L) {
            target = null;
            return;
        }

        if (!isValidTarget(mc, target)) target = findTarget(mc);
        if (target == null) return;

        AxisAlignedBB box = target.getEntityBoundingBox();
        double expand = smoothing.getValue() / 100.0D * target.getCollisionBorderSize();
        box = box.expand(expand, expand, expand);

        double tx = (box.minX + box.maxX) * 0.5D;
        double ty = box.minY + (box.maxY - box.minY) * 0.62D;
        double tz = (box.minZ + box.maxZ) * 0.5D;

        EntityPlayerSP p = mc.thePlayer;
        double dx = tx - p.posX;
        double dz = tz - p.posZ;
        double dy = ty - (p.posY + p.getEyeHeight());
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        float wantedYaw = (float)(Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
        float wantedPitch = (float)-(Math.atan2(dy, horizontal) * 180.0D / Math.PI);

        float yawDiff = MathHelper.wrapAngleTo180_float(wantedYaw - p.rotationYaw);
        float pitchDiff = MathHelper.wrapAngleTo180_float(wantedPitch - p.rotationPitch);

        float hs = (float)Math.min(10.0D, Math.abs(horizontalSpeed.getValue()));
        float vs = (float)Math.min(10.0D, Math.abs(verticalSpeed.getValue()));

        p.rotationYaw += yawDiff * (0.1F * hs);
        p.rotationPitch += pitchDiff * (0.1F * vs);
        p.rotationPitch = MathHelper.clamp_float(p.rotationPitch, -90.0F, 90.0F);
    }

    private EntityPlayer findTarget(Minecraft mc) {
        EntityPlayer best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Object o : mc.theWorld.playerEntities) {
            if (!(o instanceof EntityPlayer)) continue;
            EntityPlayer e = (EntityPlayer)o;
            if (!isValidTarget(mc, e)) continue;
            double d = mc.thePlayer.getDistanceToEntity(e);
            if (d < bestDistance) {
                bestDistance = d;
                best = e;
            }
        }
        return best;
    }

    private boolean isValidTarget(Minecraft mc, EntityPlayer e) {
        if (e == null || e == mc.thePlayer || e.isDead) return false;
        if (e.ridingEntity == mc.thePlayer || mc.thePlayer.ridingEntity == e) return false;
        if (mc.thePlayer.getDistanceToEntity(e) > range.getValue()) return false;

        float yaw = getYawTo(mc, e);
        float yawDiff = Math.abs(MathHelper.wrapAngleTo180_float(yaw - mc.thePlayer.rotationYaw));
        if (yawDiff > fov.getValue() * 0.5F) return false;

        if (teams.getValue() && mc.thePlayer.isOnSameTeam(e)) return false;
        return mc.thePlayer.canEntityBeSeen(e);
    }

    private boolean isHoldingAllowedWeapon(Minecraft mc) {
        if (mc.thePlayer.getHeldItem() == null) return false;
        Item item = mc.thePlayer.getHeldItem().getItem();
        if (item instanceof ItemSword) return true;
        return allowTools.getValue() && item instanceof ItemTool;
    }

    private float getYawTo(Minecraft mc, EntityPlayer e) {
        return (float)(Math.atan2(e.posZ - mc.thePlayer.posZ, e.posX - mc.thePlayer.posX) * 180.0D / Math.PI) - 90.0F;
    }
}
