package br.vapevlite.modules;

import br.vapevlite.BooleanSetting;
import br.vapevlite.Category;
import br.vapevlite.Module;
import br.vapevlite.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Mouse;
import java.util.concurrent.ThreadLocalRandom;

/** AutoClicker: hold the real left mouse button to click continuously. */
public class AutoClicker extends Module {
    private final NumberSetting minCps = new NumberSetting("Min CPS", 15, 1, 20, 1);
    private final NumberSetting maxCps = new NumberSetting("Max CPS", 20, 1, 20, 1);
    private final BooleanSetting randomize = new BooleanSetting("Randomize", true);

    private long nextClickAt;

    public AutoClicker() {
        super("AutoClicker", Category.COMBAT);
        addSetting(minCps);
        addSetting(maxCps);
        addSetting(randomize);
    }

    @Override
    protected void onDisable() {
        nextClickAt = 0L;
    }

    @Override
    public void onClientTick() {
        Minecraft mc = Minecraft.getMinecraft();

        if (!isEnabled() || mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null) {
            return;
        }

        // Keep clicking for as long as the physical left button is held.
        if (!Mouse.isButtonDown(0)) {
            nextClickAt = 0L;
            return;
        }

        long now = System.currentTimeMillis();

        if (nextClickAt == 0L) {
            nextClickAt = now;
        }

        if (now >= nextClickAt) {
            // Feed a real attack key press into Minecraft's normal input path.
            KeyBinding.onTick(mc.gameSettings.keyBindAttack.getKeyCode());
            nextClickAt = now + getNextDelay();
        }
    }

    private long getNextDelay() {
        int min = (int) Math.round(Math.min(minCps.getValue(), maxCps.getValue()));
        int max = (int) Math.round(Math.max(minCps.getValue(), maxCps.getValue()));

        min = Math.max(1, min);
        max = Math.max(min, max);

        double cps;
        if (!randomize.getValue() || min == max) {
            cps = min;
        } else {
            cps = ThreadLocalRandom.current().nextDouble(min, max + 1.0D);
        }

        return Math.max(1L, Math.round(1000.0D / cps));
    }
}
