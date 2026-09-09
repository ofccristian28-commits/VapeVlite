package br.vapevlite.modules;

import br.vapevlite.Category;
import br.vapevlite.Module;
import br.vapevlite.NumberSetting;
import net.minecraft.client.Minecraft;

public class JumpReset extends Module {
    private final NumberSetting delay = new NumberSetting("Delay", 2, 0, 5, 1);
    private int ticks;
    private int lastHurt;

    public JumpReset() {
        super("Jump Reset", Category.COMBAT);
        addSetting(delay);
    }

    @Override
    public void onClientTick() {
        if (!isEnabled()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.currentScreen != null) return;
        int hurt = mc.thePlayer.hurtTime;
        if (hurt > 0 && lastHurt == 0) ticks = delay.getValue().intValue();
        lastHurt = hurt;
        if (ticks > 0) { ticks--; return; }
        if (hurt > 0 && mc.thePlayer.onGround && !mc.gameSettings.keyBindSneak.isKeyDown()) {
            mc.thePlayer.jump();
        }
    }
}
