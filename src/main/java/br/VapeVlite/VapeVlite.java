package br.vapevlite;

import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

@Mod(modid = "vapevlite", name = "VapeVlite", version = "1.0.0", clientSideOnly = true)
public class VapeVlite {
    public static final ModuleManager MODULES = new ModuleManager();
    public static ConfigManager CONFIG;
    private static boolean lastRShift;
    private static long lastSave;
    private static boolean notificationsReady;
    private static final List<Notification> notifications = new ArrayList<Notification>();

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        CONFIG = new ConfigManager(MODULES);
        MinecraftForge.EVENT_BUS.register(this);
        for (Module m : MODULES.getModules()) MinecraftForge.EVENT_BUS.register(m);
        CONFIG.load();
        notificationsReady = true;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;
        Module guiBind = MODULES.get("GUI Bind");
        int guiKey = guiBind == null ? Keyboard.KEY_RSHIFT : guiBind.getKeybind();
        boolean rshift = guiKey != Keyboard.KEY_NONE && Keyboard.isKeyDown(guiKey);
        if (rshift && !lastRShift) {
            if (mc.currentScreen instanceof VapeGui) mc.displayGuiScreen(null);
            else mc.displayGuiScreen(new VapeGui());
        }
        lastRShift = rshift;
        for (Module m : MODULES.getModules()) {
            if (!(m instanceof br.vapevlite.modules.ClickGUI) && KeyState.pressed(m.getKeybind())) {
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

    /** Shows a small Elixe-style notification at the bottom-right of the screen. */
    public static void notifyModuleDisabled(String moduleName) {
        if (!notificationsReady) return;
        notifications.add(new Notification(moduleName + " disabled", System.currentTimeMillis()));
        while (notifications.size() > 4) notifications.remove(0);
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.ingameGUI == null) return;

        long now = System.currentTimeMillis();
        Iterator<Notification> it = notifications.iterator();
        while (it.hasNext()) {
            if (now - it.next().createdAt > 2200L) it.remove();
        }
        if (notifications.isEmpty()) return;

        int screenW = event.resolution.getScaledWidth();
        int screenH = event.resolution.getScaledHeight();
        int y = screenH - 8;

        for (int i = notifications.size() - 1; i >= 0; i--) {
            Notification n = notifications.get(i);
            int age = (int)(now - n.createdAt);
            String text = n.text;
            int textWidth = mc.fontRendererObj.getStringWidth(text);
            int boxW = textWidth + 28;
            int boxH = 28;
            int x = screenW - boxW - 10;
            y -= boxH;

            int alpha = 255;
            if (age > 1700) alpha = Math.max(0, 255 - (age - 1700) * 5);
            int bg = (alpha << 24) | 0x101014;
            int accent = (alpha << 24) | 0xDAA520;
            int white = (alpha << 24) | 0xE0E0E0;

            net.minecraft.client.gui.Gui.drawRect(x, y, x + boxW, y + boxH, bg);
            net.minecraft.client.gui.Gui.drawRect(x, y, x + 3, y + boxH, accent);
            mc.fontRendererObj.drawStringWithShadow(text, x + 11, y + 9, white);
            y -= 6;
        }
    }

    private static final class Notification {
        private final String text;
        private final long createdAt;

        private Notification(String text, long createdAt) {
            this.text = text;
            this.createdAt = createdAt;
        }
    }
}
