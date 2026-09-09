package br.vapevlite.modules;

import br.vapevlite.Category;
import br.vapevlite.Module;
import br.vapevlite.NumberSetting;
import net.minecraft.client.Minecraft;

import java.lang.reflect.Field;

/** Controls the vanilla right-click placement delay in ticks. */
public class FastPlace extends Module {
    private final NumberSetting delay = new NumberSetting("Delay", 1.0, 1.0, 5.0, 1.0);
    private static Field rightClickDelayTimer;

    static {
        try {
            rightClickDelayTimer = Minecraft.class.getDeclaredField("rightClickDelayTimer");
            rightClickDelayTimer.setAccessible(true);
        } catch (Exception ignored) {}
    }

    public FastPlace() {
        super("FastPlace", Category.COMBAT);
        addSetting(delay);
    }

    @Override
    public void onClientTick() {
        if (!isEnabled() || rightClickDelayTimer == null) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            int current = rightClickDelayTimer.getInt(mc);
            int wanted = Math.max(0, Math.min(5, (int)Math.round(delay.getValue())));
            if (current > wanted) rightClickDelayTimer.setInt(mc, wanted);
        } catch (Exception ignored) {}
    }
}
