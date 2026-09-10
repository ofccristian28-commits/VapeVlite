package br.vapevlite;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

@Mod(modid = "vapevlite", name = "VapeVlite", version = "1.0.0", clientSideOnly = true)
public class VapeVlite {
    public static final ModuleManager MODULES = new ModuleManager();
    public static ConfigManager CONFIG;
    private static boolean lastGuiKey;
    private static long lastSave;
    private static boolean notificationsReady;
    private static final List<Notification> NOTIFICATIONS = new ArrayList<Notification>();

    private static class Notification {
        final String module;
        final boolean enabled;
        final long created;
        Notification(String module, boolean enabled) {
            this.module = module;
            this.enabled = enabled;
            this.created = System.currentTimeMillis();
        }
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        CONFIG = new ConfigManager(MODULES);
        MinecraftForge.EVENT_BUS.register(this);
        for (Module m : MODULES.getModules()) MinecraftForge.EVENT_BUS.register(m);
        CONFIG.load();
        notificationsReady = true;
    }

    public static void notifyModule(String module, boolean enabled) {
        if (!notificationsReady) return;
        synchronized (NOTIFICATIONS) {
            NOTIFICATIONS.add(new Notification(module, enabled));
            while (NOTIFICATIONS.size() > 4) NOTIFICATIONS.remove(0);
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;

        Module guiBind = MODULES.get("ClickGUI");
        int guiKey = guiBind == null ? Keyboard.KEY_R : guiBind.getKeybind();
        boolean guiPressed = guiKey != Keyboard.KEY_NONE && Keyboard.isKeyDown(guiKey);
        if (guiPressed && !lastGuiKey) {
            if (mc.currentScreen instanceof VapeGui) mc.displayGuiScreen(null);
            else mc.displayGuiScreen(new VapeGui());
        }
        lastGuiKey = guiPressed;

        for (Module m : MODULES.getModules()) {
            if (!"ClickGUI".equalsIgnoreCase(m.getName()) && KeyState.pressed(m.getKeybind())) {
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
            // modules use their own input checks.
        }
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.fontRendererObj == null) return;
        long now = System.currentTimeMillis();
        synchronized (NOTIFICATIONS) {
            Iterator<Notification> it = NOTIFICATIONS.iterator();
            while (it.hasNext()) {
                if (now - it.next().created > 2600L) it.remove();
            }
            int index = 0;
            for (Notification n : NOTIFICATIONS) {
                long age = now - n.created;
                int alpha = 255;
                if (age > 2000L) alpha = (int)(255L * (2600L - age) / 600L);
                if (alpha < 0) alpha = 0;
                int w = 150;
                int h = 24;
                int x = event.resolution.getScaledWidth() - w - 8;
                int y = event.resolution.getScaledHeight() - 8 - h * (index + 1) - 4 * index;
                int bg = (alpha << 24) | 0x15151A;
                int accent = (alpha << 24) | (n.enabled ? 0xDAA520 : 0x8A8A8A);
                mc.ingameGUI.drawRect(x, y, x + w, y + h, bg);
                mc.ingameGUI.drawRect(x, y, x + 3, y + h, accent);
                String text = n.module + (n.enabled ? " enabled" : " disabled");
                mc.fontRendererObj.drawStringWithShadow(text, x + 9, y + 8, (alpha << 24) | 0xE0E0E0);
                index++;
            }
        }
    }

    public static void save() { if (CONFIG != null) CONFIG.save(); }
}
