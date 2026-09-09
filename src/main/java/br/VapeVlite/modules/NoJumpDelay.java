package br.vapevlite.modules;

import br.vapevlite.Category;
import br.vapevlite.Module;
import net.minecraft.client.Minecraft;

import java.lang.reflect.Field;

/** Removes the vanilla jump cooldown by keeping the player's jumpTicks at zero. */
public class NoJumpDelay extends Module {
    private static Field jumpTicks;

    static {
        try {
            jumpTicks = net.minecraft.entity.EntityLivingBase.class.getDeclaredField("jumpTicks");
            jumpTicks.setAccessible(true);
        } catch (Exception ignored) {}
    }

    public NoJumpDelay() {
        super("No Jump Delay", Category.COMBAT);
    }

    @Override
    public void onClientTick() {
        if (!isEnabled() || jumpTicks == null) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;
        try { jumpTicks.setInt(mc.thePlayer, 0); } catch (Exception ignored) {}
    }
}
