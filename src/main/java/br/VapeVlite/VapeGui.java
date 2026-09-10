package br.vapevlite;

import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.Locale;

/**
 * Compact Elixe-8-inspired ClickGUI, independently recreated from the
 * observed 400x220 layout and interaction model.
 */
public class VapeGui extends GuiScreen {
    private static final int WIDTH = 400;
    private static final int HEIGHT = 220;
    private static final int SIDEBAR = 92;
    private static final int MODULE_PANE = 120;
    private static final int CONTENT_TOP = 28;
    private static final int ACCENT = 0xFFDAA520;
    private static final int BG = 0xFF0E0E12;
    private static final int PANEL = 0xFF16161B;
    private static final int BAR = 0xFF1B1B21;
    private static final int HOVER = 0xFF26262E;
    private static final int TEXT = 0xFFDBDBDB;
    private static final int DIM = 0xFF73737A;

    private Module selected;
    private boolean listening;
    private int moduleScroll;
    private int optionsScroll;

    private int guiX;
    private int guiY;

    @Override
    public void initGui() {
        guiX = (width - WIDTH) / 2;
        guiY = (height - HEIGHT) / 2;
        selected = VapeVlite.MODULES.getModules().isEmpty()
                ? null : VapeVlite.MODULES.getModules().get(0);
        moduleScroll = 0;
        optionsScroll = 0;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        // Main 400x220 Elixe-style frame.
        drawRect(guiX, guiY, guiX + WIDTH, guiY + HEIGHT, BG);
        drawRect(guiX, guiY, guiX + SIDEBAR, guiY + HEIGHT, PANEL);
        drawRect(guiX + SIDEBAR, guiY, guiX + WIDTH, guiY + CONTENT_TOP, BAR);

        // Sidebar.
        drawString(fontRendererObj, "VapeVlite", guiX + 8, guiY + 8, ACCENT);
        drawString(fontRendererObj, "COMBAT", guiX + 8, guiY + 39, TEXT);
        drawRect(guiX + 8, guiY + 52, guiX + SIDEBAR - 8, guiY + 72,
                isMouseOver(mouseX, mouseY, guiX + 8, guiY + 52,
                        guiX + SIDEBAR - 8, guiY + 72) ? HOVER : BAR);
        drawString(fontRendererObj, "combat", guiX + 16, guiY + 58, ACCENT);

        // Module column.
        int mx = guiX + SIDEBAR + 10;
        int my = guiY + CONTENT_TOP + 8 - moduleScroll;
        int moduleIndex = 0;
        for (Module module : VapeVlite.MODULES.getModules()) {
            if (my + 18 >= guiY + CONTENT_TOP && my <= guiY + HEIGHT - 6) {
                boolean hover = isMouseOver(mouseX, mouseY, mx, my, mx + 100, my + 18);
                int base = module == selected ? HOVER : (hover ? BAR : BG);
                drawRect(mx, my, mx + 100, my + 18, base);
                if (module == selected) {
                    drawRect(mx, my, mx + 2, my + 18, ACCENT);
                }
                drawString(fontRendererObj, module.getName(), mx + 7, my + 5,
                        module == selected ? TEXT : DIM);
                drawString(fontRendererObj, module.isEnabled() ? "●" : "○",
                        mx + 88, my + 5, module.isEnabled() ? ACCENT : DIM);
            }
            moduleIndex++;
            my += 21;
        }

        // Options column.
        int ox = guiX + 220;
        int oy = guiY + CONTENT_TOP + 8 - optionsScroll;
        if (selected != null) {
            drawString(fontRendererObj, selected.getName(), ox, guiY + 8, TEXT);
            drawString(fontRendererObj, "SETTINGS", guiX + 326, guiY + 8, DIM);

            for (int i = 0; i < selected.getSettings().size(); i++) {
                Setting<?> setting = selected.getSettings().get(i);
                if (oy + 18 >= guiY + CONTENT_TOP && oy <= guiY + HEIGHT - 5) {
                    drawSetting(mouseX, mouseY, setting, ox, oy);
                }
                oy += 22;
            }
        }

        if (listening) {
            drawRect(guiX + 8, guiY + HEIGHT - 24, guiX + SIDEBAR - 8,
                    guiY + HEIGHT - 8, HOVER);
            drawString(fontRendererObj, "Press key...", guiX + 16,
                    guiY + HEIGHT - 19, ACCENT);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawSetting(int mouseX, int mouseY, Setting<?> setting, int x, int y) {
        boolean hover = isMouseOver(mouseX, mouseY, x, y, x + 170, y + 18);
        if (hover) drawRect(x, y, x + 170, y + 18, BAR);

        drawString(fontRendererObj, setting.getId(), x + 5, y + 5, TEXT);

        if (setting instanceof BooleanSetting) {
            boolean enabled = ((BooleanSetting) setting).getValue();
            drawRect(x + 145, y + 6, x + 164, y + 12, enabled ? ACCENT : DIM);
            drawString(fontRendererObj, enabled ? "ON" : "OFF", x + 121, y + 5,
                    enabled ? ACCENT : DIM);
        } else if (setting instanceof NumberSetting) {
            NumberSetting number = (NumberSetting) setting;
            String value = format(number.getValue());
            drawString(fontRendererObj, value, x + 138, y + 5, ACCENT);
            drawString(fontRendererObj, "‹", x + 122, y + 5, DIM);
            drawString(fontRendererObj, "›", x + 163, y + 5, DIM);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton != 0 || selected == null) return;

        int mx = guiX + SIDEBAR + 10;
        int my = guiY + CONTENT_TOP + 8 - moduleScroll;
        for (Module module : VapeVlite.MODULES.getModules()) {
            if (isMouseOver(mouseX, mouseY, mx, my, mx + 100, my + 18)) {
                selected = module;
                optionsScroll = 0;
                return;
            }
            my += 21;
        }

        int ox = guiX + 220;
        int oy = guiY + CONTENT_TOP + 8 - optionsScroll;
        for (Setting<?> setting : selected.getSettings()) {
            if (isMouseOver(mouseX, mouseY, ox, oy, ox + 170, oy + 18)) {
                if (setting instanceof BooleanSetting) {
                    ((BooleanSetting) setting).toggle();
                    VapeVlite.save();
                } else if (setting instanceof NumberSetting) {
                    NumberSetting number = (NumberSetting) setting;
                    if (mouseX < ox + 145) number.decrement();
                    else number.increment();
                    VapeVlite.save();
                }
                return;
            }
            oy += 22;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (listening && selected != null) {
            selected.setKeybind(keyCode == Keyboard.KEY_ESCAPE ? Keyboard.KEY_NONE : keyCode);
            listening = false;
            VapeVlite.save();
            return;
        }

        if (keyCode == Keyboard.KEY_ESCAPE) {
            VapeVlite.save();
            mc.displayGuiScreen(null);
            return;
        }

        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getDWheel();
        if (wheel == 0) return;

        if (Mouse.getX() >= guiX * 2 && Mouse.getX() <= (guiX + WIDTH) * 2) {
            if (wheel > 0) {
                moduleScroll = Math.max(0, moduleScroll - 21);
                optionsScroll = Math.max(0, optionsScroll - 22);
            } else {
                moduleScroll = Math.min(maxModuleScroll(), moduleScroll + 21);
                optionsScroll = Math.min(maxOptionsScroll(), optionsScroll + 22);
            }
        }
    }

    @Override
    public void onGuiClosed() {
        VapeVlite.save();
    }

    private int maxModuleScroll() {
        int count = VapeVlite.MODULES.getModules().size();
        return Math.max(0, count * 21 - (HEIGHT - CONTENT_TOP - 10));
    }

    private int maxOptionsScroll() {
        if (selected == null) return 0;
        return Math.max(0, selected.getSettings().size() * 22 - (HEIGHT - CONTENT_TOP - 10));
    }

    private static boolean isMouseOver(int mouseX, int mouseY,
                                       int left, int top, int right, int bottom) {
        return mouseX > left && mouseX < right && mouseY > top && mouseY < bottom;
    }

    private static String format(double value) {
        if (value == Math.rint(value)) return Integer.toString((int) value);
        return String.format(Locale.US, "%.2f", value);
    }
}
