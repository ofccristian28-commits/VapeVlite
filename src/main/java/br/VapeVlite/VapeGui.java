package br.vapevlite;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import java.io.IOException;
import java.util.Locale;

/** Compact dark/gold click GUI inspired by the Elixe 8 layout. */
public class VapeGui extends GuiScreen {
    private Module selected;
    private boolean listening;
    private int scroll;
    private int moduleTop;
    private int panelLeft;
    private int panelTop;

    private static final int GOLD = 0xFFD9A520;
    private static final int BG = 0xF20F1012;
    private static final int PANEL = 0xF2191A1E;
    private static final int CARD = 0xFF24262B;
    private static final int CARD_ON = 0xFF3A2E16;

    @Override
    public void initGui() {
        selected = VapeVlite.MODULES.getModules().isEmpty() ? null : VapeVlite.MODULES.getModules().get(0);
        scroll = 0;
        rebuild();
    }

    private void rebuild() {
        buttonList.clear();
        int w = 720, h = 430;
        panelLeft = width / 2 - w / 2;
        panelTop = height / 2 - h / 2;
        moduleTop = panelTop + 72 - scroll;

        int sideW = 145;
        int listX = panelLeft + sideW + 14;
        int listW = 210;
        int optX = listX + listW + 14;
        int optW = 300;

        int i = 0;
        for (Module m : VapeVlite.MODULES.getModules()) {
            int y = moduleTop + i * 42;
            if (y >= panelTop + 62 && y <= panelTop + h - 42) {
                buttonList.add(new GuiButton(1000 + i, listX, y, listW, 34, m.getName()));
            }
            i++;
        }

        if (selected != null) {
            buttonList.add(new GuiButton(2000, optX, panelTop + 58, 86, 24, selected.isEnabled() ? "ON" : "OFF"));
            buttonList.add(new GuiButton(2001, optX + 92, panelTop + 58, 112, 24, listening ? "PRESS KEY" : keyName(selected.getKeybind())));
            buttonList.add(new GuiButton(2002, optX + 210, panelTop + 58, 90, 24, "SAVE"));

            int y = panelTop + 104;
            for (int s = 0; s < selected.getSettings().size(); s++) {
                Setting<?> setting = selected.getSettings().get(s);
                if (y > panelTop + h - 34) break;
                if (setting instanceof BooleanSetting) {
                    buttonList.add(new GuiButton(3000 + s, optX, y, optW, 26,
                            setting.getId() + "  " + (((BooleanSetting)setting).getValue() ? "ON" : "OFF")));
                } else {
                    buttonList.add(new GuiButton(4000 + s * 2, optX, y, 28, 26, "-"));
                    buttonList.add(new GuiButton(4001 + s * 2, optX + optW - 28, y, 28, 26, "+"));
                }
                y += 34;
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton b) throws IOException {
        if (b.id >= 1000 && b.id < 2000) {
            int idx = b.id - 1000;
            if (idx < VapeVlite.MODULES.getModules().size()) selected = VapeVlite.MODULES.getModules().get(idx);
            rebuild(); return;
        }
        if (selected == null) return;
        if (b.id == 2000) { selected.toggle(); VapeVlite.save(); rebuild(); return; }
        if (b.id == 2001) { listening = true; return; }
        if (b.id == 2002) { VapeVlite.save(); return; }
        if (b.id >= 3000 && b.id < 4000) {
            int idx = b.id - 3000;
            if (idx < selected.getSettings().size()) {
                Setting<?> s = selected.getSettings().get(idx);
                if (s instanceof BooleanSetting) ((BooleanSetting)s).toggle();
                VapeVlite.save(); rebuild();
            }
            return;
        }
        if (b.id >= 4000 && b.id < 5000) {
            int raw = b.id - 4000;
            int idx = raw / 2;
            if (idx < selected.getSettings().size() && selected.getSettings().get(idx) instanceof NumberSetting) {
                NumberSetting n = (NumberSetting)selected.getSettings().get(idx);
                if ((raw & 1) == 0) n.decrement(); else n.increment();
                VapeVlite.save(); rebuild();
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (listening && selected != null) {
            selected.setKeybind(keyCode == Keyboard.KEY_ESCAPE ? Keyboard.KEY_NONE : keyCode);
            listening = false;
            VapeVlite.save(); rebuild(); return;
        }
        if (keyCode == Keyboard.KEY_ESCAPE) { VapeVlite.save(); mc.displayGuiScreen(null); return; }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = org.lwjgl.input.Mouse.getDWheel();
        if (wheel != 0) {
            scroll -= wheel > 0 ? 24 : -24;
            int max = Math.max(0, VapeVlite.MODULES.getModules().size() * 42 - 300);
            scroll = Math.max(0, Math.min(max, scroll));
            rebuild();
        }
    }

    @Override public void onGuiClosed() { VapeVlite.save(); }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int w = 720, h = 430;
        int x = width / 2 - w / 2, y = height / 2 - h / 2;
        drawRect(x, y, x + w, y + h, BG);
        drawRect(x, y, x + 145, y + h, PANEL);
        drawRect(x + 145, y, x + w, y + 48, 0xF216171A);

        drawString(fontRendererObj, "VapeVlite", x + 18, y + 18, GOLD);
        drawString(fontRendererObj, "COMBAT", x + 18, y + 48, 0xFF777777);
        drawString(fontRendererObj, selected == null ? "Modules" : selected.getName(), x + 160, y + 18, 0xFFFFFFFF);
        drawString(fontRendererObj, "SETTINGS", x + 400, y + 18, 0xFF888888);

        int i = 0;
        for (Module m : VapeVlite.MODULES.getModules()) {
            int my = y + 72 + i * 42 - scroll;
            if (my >= y + 55 && my < y + h - 10) {
                drawRect(x + 159, my, x + 369, my + 34, m == selected ? CARD_ON : CARD);
                drawString(fontRendererObj, m.getName(), x + 171, my + 12, m == selected ? GOLD : 0xFFD0D0D0);
                drawString(fontRendererObj, m.isEnabled() ? "ON" : "OFF", x + 335, my + 12, m.isEnabled() ? GOLD : 0xFF777777);
            }
            i++;
        }

        if (selected != null) {
            int optX = x + 383;
            int optY = y + 104;
            for (Setting<?> s : selected.getSettings()) {
                if (optY > y + h - 30) break;
                drawString(fontRendererObj, s.getId(), optX + 6, optY + 9, 0xFFD0D0D0);
                if (s instanceof BooleanSetting) {
                    boolean on = ((BooleanSetting)s).getValue();
                    drawString(fontRendererObj, on ? "ON" : "OFF", optX + 268, optY + 9, on ? GOLD : 0xFF777777);
                } else {
                    drawString(fontRendererObj, value(s), optX + 235, optY + 9, GOLD);
                }
                optY += 34;
            }
        }
        if (listening) drawString(fontRendererObj, "Press a key...", x + 500, y + h - 25, GOLD);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private String value(Setting<?> s) {
        if (s instanceof NumberSetting) {
            double d = ((NumberSetting)s).getValue();
            return d == Math.rint(d) ? Integer.toString((int)d) : String.format(Locale.US, "%.2f", d);
        }
        return String.valueOf(s.getValue());
    }

    private String keyName(int key) { return key <= 0 ? "NONE" : Keyboard.getKeyName(key); }
}
