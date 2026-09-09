package br.vapevlite.modules;

import br.vapevlite.BooleanSetting;
import br.vapevlite.Category;
import br.vapevlite.Module;
import br.vapevlite.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Mouse;
import java.util.concurrent.ThreadLocalRandom;

public class AutoClicker extends Module {
    private final NumberSetting minCps = new NumberSetting("Min CPS", 15, 1, 20, 1);
    private final NumberSetting maxCps = new NumberSetting("Max CPS", 20, 1, 20, 1);
    private final BooleanSetting randomize = new BooleanSetting("Randomize", true);
    private long lastClick;
    private long nextDelay = 55L;

    public AutoClicker() {
        super("AutoClicker", Category.COMBAT);
        addSetting(minCps);
        addSetting(maxCps);
        addSetting(randomize);
    }

    @Override
    public void onClientTick() {
        if (!isEnabled()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.currentScreen != null || !Mouse.isButtonDown(0)) return;

        long now = System.currentTimeMillis();
        if (now - lastClick >= nextDelay) {
            KeyBinding.onTick(mc.gameSettings.keyBindAttack.getKeyCode());
            lastClick = now;
            double cps = getNextCps();
            nextDelay = Math.max(1L, Math.round(1000D / cps));
        }
    }

    private double getNextCps() {
        int min = (int)Math.round(Math.min(minCps.getValue(), maxCps.getValue()));
        int max = (int)Math.round(Math.max(minCps.getValue(), maxCps.getValue()));
        if (!randomize.getValue() || min == max) return min;
        return ThreadLocalRandom.current().nextDouble(min, max + 1.0D);
    }
}
