package br.vapevlite.modules;

import br.vapevlite.BooleanSetting;
import br.vapevlite.Category;
import br.vapevlite.Module;
import br.vapevlite.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemSword;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.input.Mouse;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Random;

/**
 * AutoClicker for the VapeVlite module system.
 * The timing/conditions are independently reimplemented from the behavior
 * observed in Elixe 8.0, using reflection for Minecraft 1.8.9 internals.
 */
public class AutoClicker extends Module {
    private final NumberSetting cpsMin = new NumberSetting("CPS Min", 8.0, 1.0, 20.0, 1.0);
    private final NumberSetting cpsMax = new NumberSetting("CPS Max", 12.0, 1.0, 20.0, 1.0);
    private final BooleanSetting requireHold = new BooleanSetting("Require Hold", true);
    private final BooleanSetting requireWeapon = new BooleanSetting("Require Weapon", false);
    private final BooleanSetting breakBlocks = new BooleanSetting("Break Blocks", false);
    private final BooleanSetting workOnGui = new BooleanSetting("Work On GUI", false);

    private final Random random = new Random();
    private long nextClickAt;

    private Field leftClickCounterField;
    private Method clickMouseMethod;

    public AutoClicker() {
        super("AutoClicker", Category.COMBAT);
        addSetting(cpsMin);
        addSetting(cpsMax);
        addSetting(requireHold);
        addSetting(requireWeapon);
        addSetting(breakBlocks);
        addSetting(workOnGui);
        resolveMinecraftMethods();
        normalizeCps();
        resetTimer();
    }

    @Override
    protected void onEnable() {
        normalizeCps();
        resetTimer();
    }

    @Override
    protected void onDisable() {
        nextClickAt = 0L;
    }

    @Override
    public void onClientTick() {
        Minecraft mc = Minecraft.getMinecraft();
        if (!isEnabled() || mc.thePlayer == null || mc.theWorld == null) return;

        boolean gui = mc.currentScreen != null;
        if (gui && !workOnGui.getValue()) return;

        if (requireHold.getValue() && !isAttackButtonPhysicallyDown(mc)) {
            resetTimer();
            return;
        }

        if (requireWeapon.getValue() && !isHoldingSwordOrAxe(mc)) return;

        if (!breakBlocks.getValue()
                && mc.objectMouseOver != null
                && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            resetTimer();
            return;
        }

        long now = System.nanoTime();
        if (now < nextClickAt) return;

        performClick(mc);
        scheduleNextClick(now);
    }

    private boolean isAttackButtonPhysicallyDown(Minecraft mc) {
        // Minecraft's default attack key is the left mouse button. Using the
        // actual mouse state avoids the KeyBinding state getting stuck.
        if (mc.gameSettings.keyBindAttack.getKeyCode() < 0) {
            return Mouse.isButtonDown(0);
        }
        return Mouse.isButtonDown(0) || mc.gameSettings.keyBindAttack.isKeyDown();
    }

    private boolean isHoldingSwordOrAxe(Minecraft mc) {
        if (mc.thePlayer.getHeldItem() == null) return false;
        return mc.thePlayer.getHeldItem().getItem() instanceof ItemSword
                || mc.thePlayer.getHeldItem().getItem() instanceof ItemAxe;
    }

    private void performClick(Minecraft mc) {
        try {
            if (leftClickCounterField == null || clickMouseMethod == null) {
                resolveMinecraftMethods();
            }

            if (leftClickCounterField != null) {
                leftClickCounterField.setAccessible(true);
                leftClickCounterField.setInt(mc, 0);
            }

            if (clickMouseMethod != null) {
                clickMouseMethod.setAccessible(true);
                clickMouseMethod.invoke(mc);
                return;
            }
        } catch (Throwable ignored) {
            // Fall through to the vanilla key press below.
        }

        KeyBinding.onTick(mc.gameSettings.keyBindAttack.getKeyCode());
    }

    private void resolveMinecraftMethods() {
        leftClickCounterField = findField(Minecraft.class,
                "leftClickCounter", "field_71429_W");
        clickMouseMethod = findMethod(Minecraft.class,
                "clickMouse", "func_147116_af");
    }

    private void resetTimer() {
        nextClickAt = System.nanoTime();
    }

    private void scheduleNextClick(long now) {
        normalizeCps();

        int min = cpsMin.getValue().intValue();
        int max = cpsMax.getValue().intValue();
        int cps = min + (max > min ? random.nextInt(max - min + 1) : 0);

        long base = 1000000000L / Math.max(1, cps);
        long jitterUnit = base / 12L;
        long jitter = jitterUnit == 0L
                ? 0L
                : (long) (random.nextDouble() * (jitterUnit * 2L + 1L)) - jitterUnit;

        nextClickAt = now + Math.max(25000000L, base + jitter);
    }

    private void normalizeCps() {
        double min = clamp(cpsMin.getValue(), 1.0, 20.0);
        double max = clamp(cpsMax.getValue(), 1.0, 20.0);

        if (min > max) {
            double temp = min;
            min = max;
            max = temp;
        }

        cpsMin.setValue(min);
        cpsMax.setValue(max);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static Field findField(Class<?> type, String... names) {
        for (String name : names) {
            try {
                return type.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }

    private static Method findMethod(Class<?> type, String... names) {
        for (String name : names) {
            try {
                return type.getDeclaredMethod(name);
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }
}
