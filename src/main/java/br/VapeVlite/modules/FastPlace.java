package br.vapevlite.modules;

import br.vapevlite.BooleanSetting;
import br.vapevlite.Category;
import br.vapevlite.Module;
import br.vapevlite.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MovingObjectPosition;

import java.lang.reflect.Field;
import java.util.Random;

/**
 * FastPlace reimplemented from the observed CrewX behavior.
 * CPS controls the vanilla right-click delay: delay = 20 / random(CPS).
 */
public class FastPlace extends Module {
    private final NumberSetting cpsMin = new NumberSetting("CPS Min", 6.0, 0.0, 20.0, 1.0);
    private final NumberSetting cpsMax = new NumberSetting("CPS Max", 10.0, 0.0, 20.0, 1.0);
    private final BooleanSetting holdingBlock = new BooleanSetting("Holding Block", false);

    private final Random random = new Random();
    private Field rightClickDelayField;

    public FastPlace() {
        super("FastPlace", Category.COMBAT);
        addSetting(cpsMin);
        addSetting(cpsMax);
        addSetting(holdingBlock);
        rightClickDelayField = findField(Minecraft.class,
                "rightClickDelayTimer", "field_71467_ac");
    }

    @Override
    public void onClientTick() {
        if (!isEnabled()) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null) return;

        if (holdingBlock.getValue() && !isHoldingBlock(mc)) return;

        if (rightClickDelayField == null) {
            rightClickDelayField = findField(Minecraft.class,
                    "rightClickDelayTimer", "field_71467_ac");
        }
        if (rightClickDelayField == null) return;

        int min = clamp(cpsMin.getValue().intValue(), 0, 20);
        int max = clamp(cpsMax.getValue().intValue(), 0, 20);
        if (min > max) {
            int temp = min;
            min = max;
            max = temp;
        }

        // CrewX uses 20 / CPS. At 0 CPS it leaves vanilla delay untouched.
        if (max <= 0) return;

        int cps = min + (max > min ? random.nextInt(max - min + 1) : 0);
        if (cps <= 0) return;

        int wantedDelay = 20 / cps;

        try {
            rightClickDelayField.setAccessible(true);
            rightClickDelayField.setInt(mc, wantedDelay);
        } catch (Throwable ignored) {
        }
    }

    private boolean isHoldingBlock(Minecraft mc) {
        if (mc.thePlayer.getHeldItem() == null) return false;
        return mc.thePlayer.getHeldItem().getItem() instanceof net.minecraft.item.ItemBlock;
    }

    private static Field findField(Class<?> type, String... names) {
        for (String name : names) {
            try {
                return type.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
