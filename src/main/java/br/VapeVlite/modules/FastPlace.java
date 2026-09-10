package br.vapevlite.modules;

import br.vapevlite.Category;
import br.vapevlite.Module;
import br.vapevlite.NumberSetting;
import net.minecraft.client.Minecraft;

import java.lang.reflect.Field;

public class FastPlace extends Module {

    private final Minecraft mc = Minecraft.getMinecraft();

    private final NumberSetting delay =
            new NumberSetting("Delay", 1.0, 1.0, 5.0, 1.0);

    private Field rightClickDelayTimer;

    public FastPlace() {
        super("FastPlace", Category.COMBAT);
        addSetting(delay);

        try {
            rightClickDelayTimer =
                    Minecraft.class.getDeclaredField("rightClickDelayTimer");
            rightClickDelayTimer.setAccessible(true);
        } catch (Exception e) {
            rightClickDelayTimer = null;
        }
    }

    @Override
    public void onClientTick() {
        if (!isEnabled() || mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        if (rightClickDelayTimer == null) {
            return;
        }

        try {
            int wanted = delay.getValue().intValue();
            int current = rightClickDelayTimer.getInt(mc);

            if (current > wanted) {
                rightClickDelayTimer.setInt(mc, wanted);
            }
        } catch (Exception ignored) {
        }
    }
}
