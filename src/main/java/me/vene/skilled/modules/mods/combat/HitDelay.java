package me.vene.skilled.modules.mods.combat;

import me.vene.skilled.modules.Category;
import me.vene.skilled.modules.Module;
import me.vene.skilled.utilities.StringRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

import java.lang.reflect.Field;

/** CrewX-style 1.8.9 hit-delay fix: after a miss, remove the vanilla 10-tick click lock. */
public class HitDelay extends Module {
    private final Minecraft mc = Minecraft.getMinecraft();
    private Field leftClickCounter;

    public HitDelay() {
        super(StringRegistry.register("Hit Delay"), 0, Category.C);
        leftClickCounter = findField(Minecraft.class, "leftClickCounter", "field_71429_W");
    }

    @Override
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || mc.thePlayer == null || mc.theWorld == null || !Mouse.isButtonDown(0)) return;
        try {
            MovingObjectPosition hit = mc.objectMouseOver;
            if (hit == null || hit.typeOfHit == MovingObjectPosition.MovingObjectType.MISS) {
                if (leftClickCounter != null) leftClickCounter.setInt(mc, 0);
            }
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
