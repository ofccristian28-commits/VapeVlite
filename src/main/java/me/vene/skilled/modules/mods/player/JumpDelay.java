package me.vene.skilled.modules.mods.player;

import me.vene.skilled.modules.Category;
import me.vene.skilled.modules.Module;
import me.vene.skilled.utilities.StringRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.lang.reflect.Field;

/** Removes the small vanilla jump cooldown, matching the behavior used by 1.8.9 clients. */
public class JumpDelay extends Module {
    private final Minecraft mc = Minecraft.getMinecraft();
    private final Field jumpTicks = findJumpTicks();

    public JumpDelay() {
        super(StringRegistry.register("Jump Delay"), 0, Category.P);
    }

    @Override
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || mc.thePlayer == null) return;
        try {
            if (jumpTicks != null) jumpTicks.setInt(mc.thePlayer, 0);
        } catch (Throwable ignored) {}
    }

    private static Field findJumpTicks() {
        for (String name : new String[] { "jumpTicks", "field_70773_bE" }) {
            try {
                Field f = EntityPlayerSP.class.getSuperclass().getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {}
        }
        try {
            Field f = net.minecraft.entity.EntityLivingBase.class.getDeclaredField("jumpTicks");
            f.setAccessible(true);
            return f;
        } catch (NoSuchFieldException ignored) {}
        return null;
    }
}
