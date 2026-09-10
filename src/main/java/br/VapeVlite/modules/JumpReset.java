package br.vapevlite.modules;

import br.vapevlite.Category;
import br.vapevlite.Module;
import br.vapevlite.NumberSetting;
import net.minecraft.client.Minecraft;
import java.util.concurrent.ThreadLocalRandom;

public class JumpReset extends Module {
    private final NumberSetting delay = new NumberSetting("Delay", 2, 0, 5, 1);
    private final NumberSetting chance = new NumberSetting("Chance", 100, 10, 100, 1);
    private int ticks;
    private int lastHurt;

    public JumpReset() {
        super("Jump Reset", Category.MOVEMENT);
        addSetting(delay);
        addSetting(chance);
    }

    @Override
    protected void onDisable() {
        ticks = 0;
        lastHurt = 0;
    }

    @Override
    public void onClientTick() {
        if (!isEnabled()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.currentScreen != null) return;

        int hurt = mc.thePlayer.hurtTime;
        if (hurt > 0 && lastHurt == 0) {
            double c = Math.max(10D, Math.min(100D, chance.getValue()));
            if (ThreadLocalRandom.current().nextDouble(0D, 100D) < c) {
                ticks = delay.getValue().intValue();
            } else {
                ticks = -1;
            }
        }
        lastHurt = hurt;

        if (ticks > 0) {
            ticks--;
            return;
        }
        if (ticks == 0 && hurt > 0 && mc.thePlayer.onGround && !mc.gameSettings.keyBindSneak.isKeyDown()) {
            mc.thePlayer.jump();
            ticks = -1;
        }
    }
}
