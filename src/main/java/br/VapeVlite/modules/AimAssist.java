package br.vapevlite.modules;

import br.vapevlite.BooleanSetting;
import br.vapevlite.Category;
import br.vapevlite.Module;
import br.vapevlite.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.input.Mouse;
import java.util.Random;

/** Independent recreation of the supplied reference AimAssist's option model. */
public class AimAssist extends Module {
    private final BooleanSetting players = new BooleanSetting("Players", true);
    private final BooleanSetting animals = new BooleanSetting("Animals", false);
    private final BooleanSetting monsters = new BooleanSetting("Monsters", false);
    private final BooleanSetting villagers = new BooleanSetting("Villagers", false);

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
    private final BooleanSetting requireAttackButton = new BooleanSetting("Require Attack Button", false);
    private final BooleanSetting requireWeapon = new BooleanSetting("Require Weapon", false);
    private final BooleanSetting ignoreNaked = new BooleanSetting("Ignore Naked", false);
    private final BooleanSetting ignoreOnRightClick = new BooleanSetting("Ignore On Right Click", false);
    private final BooleanSetting stopOnHitbox = new BooleanSetting("Stop On Hitbox", false);

    private final Random random = new Random();
    private long lastAttack;

    public AimAssist() {
        super("Aim Assist", Category.COMBAT);
        addSetting(players); addSetting(animals); addSetting(monsters); addSetting(villagers);
        addSetting(yaw); addSetting(pitch);
        addSetting(speedFactor); addSetting(maxSpeed); addSetting(aimFov); addSetting(aimDistance);
        addSetting(randomYaw); addSetting(randomPitch); addSetting(randomSpeed);
        addSetting(randomDecrease); addSetting(randomDecreaseFov);
        addSetting(afterAttack); addSetting(requireVisibility); addSetting(requireSprint);
        addSetting(requireAttackButton); addSetting(requireWeapon); addSetting(ignoreNaked);
        addSetting(ignoreOnRightClick); addSetting(stopOnHitbox);
    }

    @Override
    protected void onDisable() { lastAttack = 0L; }

    @Override
    public void onClientTick() {
        if (!isEnabled()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null) return;

        boolean attackDown = Mouse.isButtonDown(0);
        if (attackDown) lastAttack = System.currentTimeMillis();
        if (requireAttackButton.getValue() && !attackDown) return;
        if (afterAttack.getValue() && System.currentTimeMillis() - lastAttack > 300L) return;
        if (requireSprint.getValue() && !mc.thePlayer.isSprinting()) return;
        if (requireWeapon.getValue() && !isHoldingWeapon(mc)) return;
        if (ignoreOnRightClick.getValue() && Mouse.isButtonDown(1)) return;
        if (stopOnHitbox.getValue() && mc.objectMouseOver != null
                && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) return;

        EntityLivingBase target = findTarget(mc);
        if (target == null) return;

        double dx = target.posX - mc.thePlayer.posX;
        double dz = target.posZ - mc.thePlayer.posZ;
        double dy = (target.posY + target.getEyeHeight() * 0.55D)
                - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        float wantedYaw = (float)(Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
        float wantedPitch = (float)-(Math.atan2(dy, horizontal) * 180.0D / Math.PI);
        float yawDiff = MathHelper.wrapAngleTo180_float(wantedYaw - mc.thePlayer.rotationYaw);
        float pitchDiff = MathHelper.wrapAngleTo180_float(wantedPitch - mc.thePlayer.rotationPitch);

        double fov = aimFov.getValue();
        if (randomDecrease.getValue() && Math.abs(yawDiff) < randomDecreaseFov.getValue()
                && random.nextBoolean()) return;

        float base = (float)(speedFactor.getValue() * maxSpeed.getValue());
        base = MathHelper.clamp_float(base, 0.05F, 40.0F);
        float randomFactor = 1.0F;
        if (randomYaw.getValue() || randomPitch.getValue()) {
            randomFactor += (random.nextFloat() - 0.5F) * (float)(randomSpeed.getValue() / 40.0D);
        }
        float step = MathHelper.clamp_float(base * randomFactor * 0.10F, 0.01F, 1.0F);

        if (Math.abs(yawDiff) <= fov * 0.5D && yaw.getValue())
            mc.thePlayer.rotationYaw += yawDiff * step;
        if (Math.abs(yawDiff) <= fov * 0.5D && pitch.getValue())
            mc.thePlayer.rotationPitch = MathHelper.clamp_float(mc.thePlayer.rotationPitch + pitchDiff * step, -90F, 90F);
    }

    private EntityLivingBase findTarget(Minecraft mc) {
        EntityLivingBase best = null;
        double bestScore = Double.MAX_VALUE;
        for (Object obj : mc.theWorld.loadedEntityList) {
            if (!(obj instanceof EntityLivingBase)) continue;
            EntityLivingBase e = (EntityLivingBase)obj;
            if (!valid(mc, e)) continue;
            double dx = e.posX - mc.thePlayer.posX;
            double dz = e.posZ - mc.thePlayer.posZ;
            float yawTo = (float)(Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
            double angle = Math.abs(MathHelper.wrapAngleTo180_float(yawTo - mc.thePlayer.rotationYaw));
            double distance = mc.thePlayer.getDistanceToEntity(e);
            double score = angle * 3.0D + distance;
            if (score < bestScore) { bestScore = score; best = e; }
        }
        return best;
    }

    private boolean valid(Minecraft mc, EntityLivingBase e) {
        if (e == mc.thePlayer || e.isDead || e.getHealth() <= 0) return false;
        if (mc.thePlayer.getDistanceToEntity(e) > aimDistance.getValue()) return false;
        if (!allowedType(e)) return false;
        if (requireVisibility.getValue() && !mc.thePlayer.canEntityBeSeen(e)) return false;
        if (ignoreNaked.getValue() && e instanceof EntityPlayer && isNaked((EntityPlayer)e)) return false;
        return true;
    }

    private boolean allowedType(EntityLivingBase e) {
        if (e instanceof EntityPlayer) return players.getValue();
        if (e instanceof EntityAnimal) return animals.getValue();
        if (e instanceof EntityVillager) return villagers.getValue();
        if (e instanceof IMob) return monsters.getValue();
        return false;
    }

    private boolean isNaked(EntityPlayer p) {
        for (int i = 0; i < 4; i++) if (p.inventory.armorInventory[i] != null) return false;
        return true;
    }

    private boolean isHoldingWeapon(Minecraft mc) {
        ItemStack s = mc.thePlayer.getHeldItem();
        return s != null && (s.getItem() instanceof ItemSword || s.getItem() instanceof ItemAxe);
    }
}
