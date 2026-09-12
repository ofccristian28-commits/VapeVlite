package me.vene.skilled.modules.mods.utility;

import me.vene.skilled.modules.Category;
import me.vene.skilled.modules.Module;
import me.vene.skilled.utilities.StringRegistry;
import me.vene.skilled.values.BooleanValue;
import me.vene.skilled.values.NumberValue;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.lang.reflect.Field;
import java.util.Random;

/** FastPlace with the same basic idea as Elixe: randomize the target use rate instead of a fixed rhythm. */
public class FastPlace extends Module {
    private final Minecraft mc = Minecraft.getMinecraft();
    private final NumberValue minCps = new NumberValue("CPS Min", 6.0, 1.0, 20.0);
    private final NumberValue maxCps = new NumberValue("CPS Max", 10.0, 1.0, 20.0);
    private final BooleanValue blocksOnly = new BooleanValue("Blocks Only", true);
    private final Random random = new Random();
    private Field rightClickDelayTimer;

    public FastPlace() {
        super(StringRegistry.register("FastPlace"), 0, Category.U);
        addValue(minCps);
        addValue(maxCps);
        addOption(blocksOnly);
        rightClickDelayTimer = findField(Minecraft.class, "rightClickDelayTimer", "field_71467_ac");
    }

    @Override
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null) return;
        ItemStack held = mc.thePlayer.getHeldItem();
        if (blocksOnly.getState() && (held == null || !(held.getItem() instanceof ItemBlock))) return;
        if (rightClickDelayTimer == null) return;
        try {
            int min = (int) minCps.getValue();
            int max = Math.max(min, (int) maxCps.getValue());
            int cps = min + random.nextInt(max - min + 1);
            int delay = Math.max(0, 20 / Math.max(1, cps));
            rightClickDelayTimer.setInt(mc, delay);
        } catch (Throwable ignored) {}
    }

    private static Field findField(Class<?> owner, String... names) {
        for (String name : names) {
            try { Field f = owner.getDeclaredField(name); f.setAccessible(true); return f; }
            catch (NoSuchFieldException ignored) {}
        }
        return null;
    }
}
