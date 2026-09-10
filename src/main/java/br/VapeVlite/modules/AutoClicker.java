package br.vapevlite.modules;

import br.vapevlite.BooleanSetting;
import br.vapevlite.Category;
import br.vapevlite.Module;
import br.vapevlite.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemSword;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.input.Mouse;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Random;

/**
 * Elixe-8-style AutoClicker, independently implemented for the VapeVlite
 * module system. It keeps the same settings and timing/conditions while
 * avoiding a dependency on Elixe's mixins.
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
        if (!isEnabled() || mc == null || mc.thePlayer == null || mc.theWorld == null) return;

        if (mc.currentScreen != null && !workOnGui.getValue()) return;

        if (requireHold.getValue() && !isAttackButtonDown(mc)) {
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

        click(mc);
        reportSyntheticClick(mc);
        scheduleNextClick(now);
    }

    private boolean isAttackButtonDown(Minecraft mc) {
        try {
            int code = mc.gameSettings.keyBindAttack.getKeyCode();
            if (code < 0) {
                int mouseButton = code + 100;
                return mouseButton >= 0 && Mouse.isButtonDown(mouseButton);
            }
            return mc.gameSettings.keyBindAttack.isKeyDown();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean isHoldingSwordOrAxe(Minecraft mc) {
        if (mc.thePlayer.getHeldItem() == null) return false;
        return mc.thePlayer.getHeldItem().getItem() instanceof ItemSword
                || mc.thePlayer.getHeldItem().getItem() instanceof ItemAxe;
    }

    /** Performs the actual Minecraft left-click, not merely a key press. */
    private void click(Minecraft mc) {
        try {
            if (leftClickCounterField == null) {
                leftClickCounterField = findField(Minecraft.class,
                        "leftClickCounter", "field_71429_W");
            }
            if (clickMouseMethod == null) {
                clickMouseMethod = findMethod(Minecraft.class,
                        "clickMouse", "func_147116_af");
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
            // Use the direct controller fallback below.
        }

        // Fallback for environments where reflection cannot invoke clickMouse.
        try {
            MovingObjectPosition hit = mc.objectMouseOver;
            if (hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
                Entity entity = hit.entityHit;
                if (entity != null) {
                    mc.playerController.attackEntity(mc.thePlayer, entity);
                    mc.thePlayer.swingItem();
                    return;
                }
            }

            if (hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                    && breakBlocks.getValue()) {
                mc.playerController.clickBlock(hit.getBlockPos(), hit.sideHit);
                mc.thePlayer.swingItem();
                return;
            }

            int key = mc.gameSettings.keyBindAttack.getKeyCode();
            KeyBinding.setKeyBindState(key, true);
            KeyBinding.onTick(key);
            KeyBinding.setKeyBindState(key, false);
        } catch (Throwable ignored) {
            // Never crash Minecraft because of the AutoClicker.
        }
    }

    /** Keeps CPS counters/listeners in sync with the synthetic click. */
    private void reportSyntheticClick(Minecraft mc) {
        try {
            KeyBinding attack = mc.gameSettings.keyBindAttack;
            int key = attack.getKeyCode();
            KeyBinding.setKeyBindState(key, false);
            KeyBinding.onTick(key);
            KeyBinding.setKeyBindState(key, false);
        } catch (Throwable ignored) {
        }
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
        long jitter = jitterUnit == 0L ? 0L
                : (long) (random.nextDouble() * (jitterUnit * 2L + 1L)) - jitterUnit;

        nextClickAt = now + Math.max(25000000L, base + jitter);
    }

    private void normalizeCps() {
        double min = clamp(cpsMin.getValue(), 1.0, 20.0);
        double max = clamp(cpsMax.getValue(), 1.0, 20.0);
        if (min > max) {
            double t = min;
            min = max;
            max = t;
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
