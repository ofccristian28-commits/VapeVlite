package br.vapevlite.modules;

import br.vapevlite.BooleanSetting;
import br.vapevlite.Category;
import br.vapevlite.Module;
import br.vapevlite.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.input.Mouse;
import java.util.Random;

/**
 * Independent AutoClicker implementation inspired by the public behaviour/settings
 * observed in the supplied reference client. No reference bytecode is embedded.
 */
public class AutoClicker extends Module {
    private final NumberSetting cpsMin = new NumberSetting("CPS Min", 8, 1, 20, 1);
    private final NumberSetting cpsMax = new NumberSetting("CPS Max", 12, 1, 20, 1);
    private final BooleanSetting requireHold = new BooleanSetting("Require Hold", true);
    private final BooleanSetting requireWeapon = new BooleanSetting("Require Weapon", false);
    private final BooleanSetting breakBlocks = new BooleanSetting("Break Blocks", false);
    private final BooleanSetting workOnGui = new BooleanSetting("Work On GUI", false);

    private final Random random = new Random();
    private long nextClickAt;

    public AutoClicker() {
        super("AutoClicker", Category.COMBAT);
        addSetting(cpsMin);
        addSetting(cpsMax);
        addSetting(requireHold);
        addSetting(requireWeapon);
        addSetting(breakBlocks);
        addSetting(workOnGui);
        resetTimer();
    }

    @Override
    protected void onEnable() {
        resetTimer();
    }

    @Override
    protected void onDisable() {
        nextClickAt = 0L;
    }

    @Override
    public void onClientTick() {
        if (!isEnabled()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (mc.currentScreen != null && !workOnGui.getValue()) {
            resetTimer();
            return;
        }

        if (requireHold.getValue() && !isAttackButtonDown(mc)) {
            resetTimer();
            return;
        }

        if (requireWeapon.getValue() && !isHoldingSwordOrAxe(mc)) return;

        long now = System.currentTimeMillis();
        if (now < nextClickAt) return;

        MovingObjectPosition hit = mc.objectMouseOver;
        if (hit == null) return;

        if (hit.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY
                && hit.entityHit instanceof EntityLivingBase) {
            EntityLivingBase target = (EntityLivingBase) hit.entityHit;
            if (!target.isDead && target != mc.thePlayer) {
                mc.playerController.attackEntity(mc.thePlayer, target);
                mc.thePlayer.swingItem();
                scheduleNextClick(now);
            }
        } else if (hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && breakBlocks.getValue()) {
            // Respect vanilla block interaction rather than injecting packets.
            mc.playerController.onPlayerDamageBlock(hit.getBlockPos(), hit.sideHit);
            mc.thePlayer.swingItem();
            scheduleNextClick(now);
        }
    }

    private boolean isAttackButtonDown(Minecraft mc) {
        int key = mc.gameSettings.keyBindAttack.getKeyCode();
        if (key < 0) return Mouse.isButtonDown(key + 100);
        return org.lwjgl.input.Keyboard.isKeyDown(key);
    }

    private boolean isHoldingSwordOrAxe(Minecraft mc) {
        ItemStack stack = mc.thePlayer.getHeldItem();
        if (stack == null) return false;
        return stack.getItem() instanceof ItemSword || stack.getItem() instanceof ItemAxe;
    }

    private void resetTimer() {
        nextClickAt = System.currentTimeMillis();
    }

    private void scheduleNextClick(long now) {
        int min = (int) Math.round(Math.min(cpsMin.getValue(), cpsMax.getValue()));
        int max = (int) Math.round(Math.max(cpsMin.getValue(), cpsMax.getValue()));
        int cps = min == max ? min : min + random.nextInt(max - min + 1);
        cps = Math.max(1, Math.min(20, cps));
        long delay = Math.max(1L, Math.round(1000.0D / cps));
        nextClickAt = now + delay;
    }
}
