package br.vapevlite.modules;

import br.vapevlite.Category;
import br.vapevlite.Module;
import br.vapevlite.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Mouse;

public class AutoClicker extends Module {
    private final NumberSetting cps = new NumberSetting("CPS", 10, 1, 20, 1);
    private long lastClick;

    public AutoClicker() {
        super("AutoClicker", Category.COMBAT);
        addSetting(cps);
    }

    public double getCps() { return cps.getValue(); }

    @Override
    public void onClientTick() {
        if (!isEnabled()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.currentScreen != null || !Mouse.isButtonDown(0)) return;
        long delay = (long)(1000D / cps.getValue());
        long now = System.currentTimeMillis();
        if (now - lastClick >= delay) {
            KeyBinding.onTick(mc.gameSettings.keyBindAttack.getKeyCode());
            lastClick = now;
        }
    }
}
