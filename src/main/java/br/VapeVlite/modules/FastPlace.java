package br.vapevlite.modules;

import br.vapevlite.Category;
import br.vapevlite.Module;
import br.vapevlite.NumberSetting;
import net.minecraft.client.Minecraft;

/** Reduces the vanilla right-click placement delay (in ticks). */
public class FastPlace extends Module {
    private final NumberSetting delay = new NumberSetting("Delay", 1.0, 1.0, 5.0, 1.0);

    public FastPlace() {
        super("FastPlace", Category.COMBAT);
        addSetting(delay);
    }

    @Override
    public void onClientTick() {
        if (!isEnabled()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.currentScreen != null) return;
        int wanted = Math.max(1, Math.min(5, delay.getValue().intValue()));
        if (mc.rightClickDelayTimer > wanted) {
            mc.rightClickDelayTimer = wanted;
        }
    }
}
