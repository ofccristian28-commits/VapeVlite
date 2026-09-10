package br.vapevlite.modules;

import br.vapevlite.BooleanSetting;
import br.vapevlite.Category;
import br.vapevlite.Module;
import br.vapevlite.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemSword;
import org.lwjgl.input.Mouse;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Random;

/** Elixe-style autoclicker reimplemented for the VapeVlite module system. */
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
        normalizeCps();
        resetTimer();
    }

    @Override
    protected void onEnable() { resetTimer(); }

    @Override
    protected void onDisable() { nextClickAt = 0L; }

    @Override
    public void onClientTick() {
        Minecraft mc = Minecraft.getMinecraft();
        if (!isEnabled() || mc.thePlayer == null || mc.theWorld == null) return;

        boolean gui = mc.currentScreen != null;
        if (gui && !workOnGui.getValue()) return;
        if (requireHold.getValue() && !Mouse.isButtonDown(0)) {
            resetTimer();
            return;
        }
        if (requireWeapon.getValue() && !isHoldingSwordOrAxe(mc)) return;

        if (!breakBlocks.getValue() && mc.objectMouseOver != null
                && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            resetTimer();
            return;
        }

        long now = System.nanoTime();
        if (now < nextClickAt) return;

        performClick(mc);
        scheduleNextClick(now);
    }

    private boolean isHoldingSwordOrAxe(Minecraft mc) {
        if (mc.thePlayer.getHeldItem() == null) return false;
        return mc.thePlayer.getHeldItem().getItem() instanceof ItemSword
                || mc.thePlayer.getHeldItem().getItem() instanceof ItemAxe;
    }

    private void performClick(Minecraft mc) {
        try {
            Field counter = findField(Minecraft.class, "leftClickCounter", "field_71429_W");
            if (counter != null) { counter.setAccessible(true); counter.setInt(mc, 0); }

            Method click = findMethod(Minecraft.class, "clickMouse", "func_147116_af");
            if (click != null) {
                click.setAccessible(true);
                click.invoke(mc);
                return;
            }
        } catch (Throwable ignored) { }

        // Fallback for mappings/runtime variants.
        KeyBinding.onTick(mc.gameSettings.keyBindAttack.getKeyCode());
    }

    private void resetTimer() { nextClickAt = System.nanoTime(); }

    private void scheduleNextClick(long now) {
        normalizeCps();
        int cps = cpsMin.getValue().intValue();
        if (cpsMax.getValue().intValue() > cps) {
            cps += random.nextInt(cpsMax.getValue().intValue() - cps + 1);
        }
        long base = 1000000000L / Math.max(1, cps);
        long jitterUnit = base / 12L;
        long jitter = jitterUnit == 0 ? 0L : (long)(random.nextDouble() * (jitterUnit * 2L + 1L)) - jitterUnit;
        nextClickAt = now + Math.max(25000000L, base + jitter);
    }

    private void normalizeCps() {
        cpsMin.setValue(clamp((int)Math.round(cpsMin.getValue()), 1, 20));
        cpsMax.setValue(clamp((int)Math.round(cpsMax.getValue()), 1, 20));
        if (cpsMin.getValue() > cpsMax.getValue()) {
            double t = cpsMin.getValue();
            cpsMin.setValue(cpsMax.getValue());
            cpsMax.setValue(t);
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static Field findField(Class<?> type, String... names) {
        for (String name : names) {
            try { return type.getDeclaredField(name); } catch (NoSuchFieldException ignored) { }
        }
        return null;
    }

    private static Method findMethod(Class<?> type, String... names) {
        for (String name : names) {
            try { return type.getDeclaredMethod(name); } catch (NoSuchMethodException ignored) { }
        }
        return null;
    }
}
