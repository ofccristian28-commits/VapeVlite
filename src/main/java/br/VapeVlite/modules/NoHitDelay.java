package br.vapevlite.modules;

import br.vapevlite.Category;
import br.vapevlite.Module;
import net.minecraft.client.Minecraft;

import java.lang.reflect.Field;

/** Removes the client-side attack click delay. */
public class NoHitDelay extends Module {
    private static Field leftClickCounter;

    static {
        try {
            leftClickCounter = Minecraft.class.getDeclaredField("leftClickCounter");
            leftClickCounter.setAccessible(true);
        } catch (Exception ignored) {}
    }

    public NoHitDelay() {
        super("No Hit Delay", Category.COMBAT);
    }

    @Override
    public void onClientTick() {
        if (!isEnabled() || leftClickCounter == null) return;
        try {
            leftClickCounter.setInt(Minecraft.getMinecraft(), 0);
        } catch (Exception ignored) {}
    }
}
