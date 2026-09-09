package br.vapevlite;

import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

@Mod(modid = "vapevlite", name = "VapeVlite", version = "1.0.0", clientSideOnly = true)
public class VapeVlite {
    public static final ModuleManager MODULES = new ModuleManager();
    public static ConfigManager CONFIG;
    private static boolean lastRShift;
    private static long lastSave;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        CONFIG = new ConfigManager(MODULES);
        MinecraftForge.EVENT_BUS.register(this);
        for (Module m : MODULES.getModules()) MinecraftForge.EVENT_BUS.register(m);
        CONFIG.load();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;
        boolean rshift = Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
        if (rshift && !lastRShift) {
            if (mc.currentScreen instanceof VapeGui) mc.displayGuiScreen(null);
            else mc.displayGuiScreen(new VapeGui());
        }
        lastRShift = rshift;
        for (Module m : MODULES.getModules()) {
            if (KeyState.pressed(m.getKeybind())) {
                m.toggle();
                save();
            }
            m.onClientTick();
        }
        if (System.currentTimeMillis() - lastSave > 2000L) {
            save();
            lastSave = System.currentTimeMillis();
        }
        if (Mouse.isButtonDown(0)) {
            // modules use their own input checks; this keeps the central loop lightweight.
        }
    }

    public static void save() { if (CONFIG != null) CONFIG.save(); }
}
