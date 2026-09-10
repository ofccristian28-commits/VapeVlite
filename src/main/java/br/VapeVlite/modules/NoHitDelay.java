package br.vapevlite.modules;

import br.vapevlite.BooleanSetting;
import br.vapevlite.Category;
import br.vapevlite.Module;
import net.minecraft.client.Minecraft;

import java.lang.reflect.Field;

/** Removes the vanilla client attack cooldown. */
public class NoHitDelay extends Module {
    private final BooleanSetting onlyWhileAttacking = new BooleanSetting("Only While Attacking", false);
    private Field leftClickCounter;

    public NoHitDelay() {
        super("No Hit Delay", Category.COMBAT);
        addSetting(onlyWhileAttacking);
        leftClickCounter = findField(Minecraft.class, "leftClickCounter", "field_71429_W");
    }

    @Override
    public void onClientTick() {
        if (!isEnabled()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || leftClickCounter == null) return;
        if (onlyWhileAttacking.getValue() && !org.lwjgl.input.Mouse.isButtonDown(0)) return;
        try {
            leftClickCounter.setAccessible(true);
            leftClickCounter.setInt(mc, 0);
        } catch (Throwable ignored) {
        }
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
}
