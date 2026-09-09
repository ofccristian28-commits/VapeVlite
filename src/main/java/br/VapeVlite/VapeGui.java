package br.vapevlite;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import java.io.IOException;
import org.lwjgl.input.Keyboard;

public class VapeGui extends GuiScreen {
    private Module selected;
    private boolean listening;

    @Override
    public void initGui() {
        selected = VapeVlite.MODULES.getModules().isEmpty() ? null : VapeVlite.MODULES.getModules().get(0);
        rebuild();
    }

    private void rebuild() {
        buttonList.clear();
        int left = width / 2 - 150;
        int top = 40;
        int i = 0;
        for (Module m : VapeVlite.MODULES.getModules()) {
            buttonList.add(new GuiButton(1000 + i, left, top + i * 28, 110, 22, m.getName() + (m.isEnabled() ? "  ON" : "  OFF")));
            i++;
        }
        if (selected != null) {
            int x = left + 125;
            buttonList.add(new GuiButton(2000, x, 40, 120, 22, "Toggle"));
            buttonList.add(new GuiButton(2001, x, 68, 120, 22, "Key: " + keyName(selected.getKeybind())));
            int y = 98;
            for (int s = 0; s < selected.getSettings().size(); s++) {
                Setting<?> setting = selected.getSettings().get(s);
                buttonList.add(new GuiButton(3000 + s * 2, x, y, 55, 20, "-"));
                buttonList.add(new GuiButton(3001 + s * 2, x + 65, y, 55, 20, "+"));
                y += 48;
            }
            buttonList.add(new GuiButton(4000, x, height - 38, 120, 22, "Save"));
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id >= 1000 && button.id < 2000) {
            int idx = button.id - 1000;
            if (idx < VapeVlite.MODULES.getModules().size()) selected = VapeVlite.MODULES.getModules().get(idx);
            rebuild();
            return;
        }
        if (selected == null) return;
        if (button.id == 2000) { selected.toggle(); rebuild(); return; }
        if (button.id == 2001) { listening = true; return; }
        if (button.id == 4000) { VapeVlite.save(); return; }
        if (button.id >= 3000 && button.id < 4000) {
            int index = (button.id - 3000) / 2;
            if (index >= 0 && index < selected.getSettings().size()) {
                Setting<?> s = selected.getSettings().get(index);
                if (s instanceof NumberSetting) {
                    NumberSetting n = (NumberSetting)s;
                    if ((button.id & 1) == 0) n.decrement(); else n.increment();
                }
                rebuild();
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (listening && selected != null) {
            selected.setKeybind(keyCode == Keyboard.KEY_ESCAPE ? Keyboard.KEY_NONE : keyCode);
            listening = false;
            VapeVlite.save();
            rebuild();
            return;
        }
        if (keyCode == Keyboard.KEY_ESCAPE) { VapeVlite.save(); mc.displayGuiScreen(null); return; }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void onGuiClosed() { VapeVlite.save(); }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawRect(0, 0, width, height, 0xB0101010);
        int left = width / 2 - 150;
        drawRect(left - 8, 18, left + 245, height - 15, 0xD91B1B1B);
        drawString(fontRendererObj, "VapeVlite", left, 24, 0xFFFFFF);
        drawString(fontRendererObj, "Combat", left + 10, 31, 0x888888);
        if (selected != null) {
            int x = left + 125;
            drawString(fontRendererObj, selected.getName(), x, 28, 0xFFFFFF);
            int y = 126;
            for (Setting<?> setting : selected.getSettings()) {
                String value = value(setting);
                drawString(fontRendererObj, setting.getId() + ": " + value, x, y, 0xDDDDDD);
                y += 48;
            }
        }
        if (listening) drawString(fontRendererObj, "Press a key...", width / 2 - 40, height - 60, 0xFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private String value(Setting<?> s) {
        if (s instanceof NumberSetting) {
            double d = ((NumberSetting)s).getValue();
            return d == Math.rint(d) ? Integer.toString((int)d) : String.format(java.util.Locale.US, "%.2f", d);
        }
        return String.valueOf(s.getValue());
    }

    private String keyName(int key) {
        return key <= 0 ? "NONE" : Keyboard.getKeyName(key);
    }
}
