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

    private final NumberSetting cpsMin =
            new NumberSetting("Min CPS", 15.0, 1.0, 20.0, 1.0);

    private final NumberSetting cpsMax =
            new NumberSetting("Max CPS", 20.0, 1.0, 20.0, 1.0);

    private final BooleanSetting randomize =
            new BooleanSetting("Randomize", true);

    private long nextClickAt = 0L;

    public AutoClicker() {
        super("AutoClicker", Category.COMBAT);

        addSetting(cpsMin);
        addSetting(cpsMax);
        addSetting(randomize);
    }

    @Override
    protected void onDisable() {
        nextClickAt = 0L;
    }

    @Override
    public void onClientTick() {
        Minecraft mc = Minecraft.getMinecraft();

        if (!isEnabled()) {
            return;
        }

        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        if (mc.currentScreen != null) {
            return;
        }

        // Só funciona enquanto o botão esquerdo real estiver pressionado.
        if (!Mouse.isButtonDown(0)) {
            nextClickAt = 0L;
            return;
        }

        long now = System.currentTimeMillis();

        // Começa a clicar imediatamente.
        if (nextClickAt == 0L) {
            nextClickAt = now;
        }

        if (now >= nextClickAt) {
            int keyCode = mc.gameSettings.keyBindAttack.getKeyCode();

            KeyBinding.onTick(keyCode);

            nextClickAt = now + getNextDelay();
        }
    }

    private long getNextDelay() {
        int min = (int) Math.round(
                Math.min(cpsMin.getValue(), cpsMax.getValue())
        );

        int max = (int) Math.round(
                Math.max(cpsMin.getValue(), cpsMax.getValue())
        );

        min = clamp(min, 1, 20);
        max = clamp(max, min, 20);

        double cps;

        if (!randomize.getValue() || min == max) {
            cps = min;
        } else {
            cps = ThreadLocalRandom.current()
                    .nextDouble(min, max + 1.0D);
        }

        return Math.max(
                1L,
                Math.round(1000.0D / cps)
        );
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
