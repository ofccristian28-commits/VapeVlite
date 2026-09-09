package br.vapevlite.modules;

import br.vapevlite.BooleanSetting;
import br.vapevlite.Category;
import br.vapevlite.Module;
import br.vapevlite.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.input.Mouse;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Independent recreation of the CrewX AutoClicker configuration/logic style.
 */
public class CrewXAutoClicker extends Module {
    private final NumberSetting minCps = new NumberSetting("Min CPS", 8, 1, 20, 1);
    private final NumberSetting maxCps = new NumberSetting("Max CPS", 12, 1, 20, 1);
    private final BooleanSetting blockHit = new BooleanSetting("Block Hit", false);
    private final NumberSetting blockHitTicks = new NumberSetting("Block Hit Ticks", 1.5, 1, 20, 0.5);
    private final BooleanSetting weaponsOnly = new BooleanSetting("Weapons Only", true);
    private final BooleanSetting allowTools = new BooleanSetting("Allow Tools", false);
    private final BooleanSetting breakBlocks = new BooleanSetting("Break Blocks", true);

    private long clickDelay;
    private long blockHitDelay;

    public CrewXAutoClicker() {
        super("AutoClicker (CrewX)", Category.COMBAT);
        addSetting(minCps);
        addSetting(maxCps);
        addSetting(blockHit);
        addSetting(blockHitTicks);
        addSetting(weaponsOnly);
        addSetting(allowTools);
        addSetting(breakBlocks);
    }

    @Override
    public void onClientTick() {
        if (!isEnabled()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null) return;

        clickDelay = Math.max(0L, clickDelay - 50L);
        blockHitDelay = Math.max(0L, blockHitDelay - 50L);

        if (!Mouse.isButtonDown(0)) return;
        if (!canClickItem(mc)) return;

        MovingObjectPosition hit = mc.objectMouseOver;
        if (hit == null) return;

        if (hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && !breakBlocks.getValue()) return;
        if (hit.typeOfHit != MovingObjectPosition.MovingObjectType.ENTITY ||
                !(hit.entityHit instanceof EntityLivingBase)) return;

        EntityLivingBase target = (EntityLivingBase)hit.entityHit;
        if (target == mc.thePlayer || target.isDead) return;
        if (clickDelay > 0L) return;

        mc.playerController.attackEntity(mc.thePlayer, target);
        mc.thePlayer.swingItem();

        clickDelay = nextClickDelay();
        if (blockHit.getValue() && blockHitDelay <= 0L && isSword(mc)) {
            blockHitDelay = Math.max(50L, Math.round(blockHitTicks.getValue() * 50.0D));
        }
    }

    private long nextClickDelay() {
        int low = (int)Math.round(Math.min(minCps.getValue(), maxCps.getValue()));
        int high = (int)Math.round(Math.max(minCps.getValue(), maxCps.getValue()));
        double cps = low == high ? low : ThreadLocalRandom.current().nextDouble(low, high + 1.0D);
        return Math.max(1L, Math.round(1000.0D / cps));
    }

    private boolean canClickItem(Minecraft mc) {
        if (!weaponsOnly.getValue()) return true;
        if (mc.thePlayer.getHeldItem() == null) return false;
        Item item = mc.thePlayer.getHeldItem().getItem();
        if (item instanceof ItemSword) return true;
        return allowTools.getValue() && item instanceof ItemTool;
    }

    private boolean isSword(Minecraft mc) {
        return mc.thePlayer.getHeldItem() != null &&
                mc.thePlayer.getHeldItem().getItem() instanceof ItemSword;
    }
}
