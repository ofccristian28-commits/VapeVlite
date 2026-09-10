package br.vapevlite.modules;

import br.vapevlite.BooleanSetting;
import br.vapevlite.Category;
import br.vapevlite.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;

import java.lang.reflect.Field;

/** Removes the vanilla jump cooldown. */
public class NoJumpDelay extends Module {
    private final BooleanSetting onlyOnGround = new BooleanSetting("Only On Ground", false);
    private Field jumpTicks;

    public NoJumpDelay() {
        super("No Jump Delay", Category.MOVEMENT);
        addSetting(onlyOnGround);
        jumpTicks = findField(EntityLivingBase.class, "jumpTicks", "field_70773_bE");
    }

    @Override
    public void onClientTick() {
        if (!isEnabled()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || jumpTicks == null) return;
        if (onlyOnGround.getValue() && !mc.thePlayer.onGround) return;
        try {
            jumpTicks.setAccessible(true);
            jumpTicks.setInt(mc.thePlayer, 0);
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
