package br.vapevlite;

import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Compact Elixe-style ClickGUI, independently recreated from the supplied
 * Elixe 8.0 reference layout. It keeps the VapeVlite name and uses the same
 * compact sidebar/module/settings proportions rather than vanilla buttons.
 */
public class VapeGui extends GuiScreen {
    private static final int WIDTH = 400;
    private static final int HEIGHT = 220;
    private static final int SIDEBAR = 92;
    private static final int HEADER = 28;
    private static final int MODULE_X = 102;
    private static final int MODULE_W = 100;
    private static final int OPTION_X = 220;
    private static final int OPTION_W = 170;

    private static final int BG = 0xFF0E0E12;
    private static final int PANEL = 0xFF16161B;
    private static final int BAR = 0xFF1B1B21;
    private static final int HOVER = 0xFF26262E;
    private static final int ACCENT = 0xFFDAA520;
    private static final int ACCENT_DIM = 0xFF78600F;
    private static final int TEXT = 0xFFE0E0E0;
    private static final int DIM = 0xFF77777F;
    private static final int LINE = 0xFF303038;

    private Category category = Category.COMBAT;
    private Module selected;
    private int moduleScroll;
    private int optionScroll;
    private boolean listening;
    private boolean configTab;
    private int guiX;
    private int guiY;

    @Override
    public void initGui() {
        guiX = (width - WIDTH) / 2;
        guiY = (height - HEIGHT) / 2;
        selected = firstModule(category);
        moduleScroll = 0;
        optionScroll = 0;
        listening = false;
        configTab = false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        // Main frame: same compact 400x220 proportions as the reference.
        drawRect(guiX, guiY, guiX + WIDTH, guiY + HEIGHT, BG);
        drawRect(guiX, guiY, guiX + SIDEBAR, guiY + HEIGHT, PANEL);
        drawRect(guiX + SIDEBAR, guiY, guiX + WIDTH, guiY + HEADER, BAR);
        drawRect(guiX + SIDEBAR, guiY + HEADER, guiX + SIDEBAR + 1, guiY + HEIGHT, LINE);

        drawSidebar(mouseX, mouseY);
        drawHeader();

        if (configTab) {
            drawConfigPanel();
        } else {
            drawModules(mouseX, mouseY);
            drawOptions(mouseX, mouseY);
        }

        if (listening) {
            drawRect(guiX + 6, guiY + HEIGHT - 23, guiX + SIDEBAR - 6, guiY + HEIGHT - 6, HOVER);
            drawString(fontRendererObj, "press key...", guiX + 13, guiY + HEIGHT - 18, ACCENT);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawSidebar(int mouseX, int mouseY) {
        drawString(fontRendererObj, "VapeVlite", guiX + 8, guiY + 8, ACCENT);

        Category[] categories = Category.values();
        int y = guiY + 39;
        for (Category cat : categories) {
            boolean selectedCategory = cat == category && !configTab;
            boolean hover = inside(mouseX, mouseY, guiX + 6, y - 2, guiX + SIDEBAR - 6, y + 18);

            if (selectedCategory || hover) {
                drawRect(guiX + 6, y - 2, guiX + SIDEBAR - 6, y + 18,
                        selectedCategory ? HOVER : BAR);
            }
            if (selectedCategory) {
                drawRect(guiX + 6, y - 2, guiX + 8, y + 18, ACCENT);
            }
            drawString(fontRendererObj, cat.getDisplayName(), guiX + 13, y + 3,
                    selectedCategory ? TEXT : DIM);
            y += 23;
        }

        int configY = guiY + HEIGHT - 29;
        boolean hoverConfig = inside(mouseX, mouseY, guiX + 6, configY - 2,
                guiX + SIDEBAR - 6, configY + 18);
        if (configTab || hoverConfig) {
            drawRect(guiX + 6, configY - 2, guiX + SIDEBAR - 6, configY + 18,
                    configTab ? HOVER : BAR);
        }
        if (configTab) drawRect(guiX + 6, configY - 2, guiX + 8, configY + 18, ACCENT);
        drawString(fontRendererObj, "config", guiX + 13, configY + 3, configTab ? TEXT : DIM);
    }

    private void drawHeader() {
        String left = configTab ? "config" : category.getDisplayName();
        drawString(fontRendererObj, left, guiX + SIDEBAR + 10, guiY + 8, TEXT);
        if (!configTab && selected != null) {
            drawString(fontRendererObj, "/", guiX + SIDEBAR + 67, guiY + 8, DIM);
            drawString(fontRendererObj, selected.getName().toLowerCase(Locale.US),
                    guiX + SIDEBAR + 78, guiY + 8, DIM);
        }
        drawString(fontRendererObj, "SETTINGS", guiX + 326, guiY + 8, DIM);
    }

    private void drawModules(int mouseX, int mouseY) {
        List<Module> modules = modulesFor(category);
        int y = guiY + HEADER + 8 - moduleScroll;

        for (Module module : modules) {
            if (y + 18 >= guiY + HEADER && y <= guiY + HEIGHT - 6) {
                boolean hover = inside(mouseX, mouseY, guiX + MODULE_X, y,
                        guiX + MODULE_X + MODULE_W, y + 18);
                boolean active = module == selected;
                drawRect(guiX + MODULE_X, y, guiX + MODULE_X + MODULE_W, y + 18,
                        active ? HOVER : (hover ? BAR : BG));
                if (active) drawRect(guiX + MODULE_X, y, guiX + MODULE_X + 2, y + 18, ACCENT);

                drawString(fontRendererObj, module.getName(), guiX + MODULE_X + 7, y + 5,
                        active ? TEXT : DIM);

                // Compact Elixe-like switch indicator.
                int dot = module.isEnabled() ? ACCENT : DIM;
                drawRect(guiX + MODULE_X + 88, y + 6,
                        guiX + MODULE_X + 94, y + 12, dot);
            }
            y += 21;
        }

        if (modules.isEmpty()) {
            drawString(fontRendererObj, "no modules", guiX + MODULE_X + 7,
                    guiY + HEADER + 14, DIM);
        }
    }

    private void drawOptions(int mouseX, int mouseY) {
        if (selected == null) return;

        int x = guiX + OPTION_X;
        int y = guiY + HEADER + 8 - optionScroll;

        // Keybind row, matching the right-hand control area in the reference.
        drawString(fontRendererObj, "key", x, y + 5, TEXT);
        drawRect(x + 78, y + 2, x + OPTION_W, y + 18, HOVER);
        String key = listening ? "press..." : keyName(selected.getKeybind());
        drawString(fontRendererObj, key, x + 98, y + 5, listening ? ACCENT : TEXT);
        y += 24;

        List<Setting<?>> settings = selected.getSettings();
        if (settings.isEmpty()) {
            drawString(fontRendererObj, "no settings", x, y + 5, DIM);
            return;
        }

        for (Setting<?> setting : settings) {
            if (y + 40 >= guiY + HEADER && y <= guiY + HEIGHT - 5) {
                drawSetting(mouseX, mouseY, setting, x, y);
            }
            y += setting instanceof NumberSetting ? 27 : 22;
        }
    }

    private void drawSetting(int mouseX, int mouseY, Setting<?> setting, int x, int y) {
        boolean hover = inside(mouseX, mouseY, x, y, x + OPTION_W, y + 20);
        if (hover) drawRect(x - 3, y, x + OPTION_W, y + 20, BAR);

        drawString(fontRendererObj, setting.getId(), x + 4, y + 5, TEXT);

        if (setting instanceof BooleanSetting) {
            boolean enabled = ((BooleanSetting) setting).getValue();
            drawString(fontRendererObj, enabled ? "ON" : "OFF", x + 122, y + 5,
                    enabled ? ACCENT : DIM);
            drawRect(x + 145, y + 6, x + 164, y + 12, enabled ? ACCENT : DIM);
        } else if (setting instanceof NumberSetting) {
            NumberSetting n = (NumberSetting) setting;
            double min = n.getMin();
            double max = n.getMax();
            double value = n.getValue();
            double pct = max <= min ? 0D : (value - min) / (max - min);
            pct = Math.max(0D, Math.min(1D, pct));

            drawString(fontRendererObj, format(value), x + 122, y + 5, ACCENT);
            int barY = y + 20;
            drawRect(x + 4, barY, x + 166, barY + 2, DIM);
            drawRect(x + 4, barY, x + 4 + (int) (162D * pct), barY + 2, ACCENT);
            drawRect(x + 2 + (int) (162D * pct), barY - 2,
                    x + 6 + (int) (162D * pct), barY + 4, ACCENT);
        }
    }

    private void drawConfigPanel() {
        int x = guiX + SIDEBAR + 18;
        int y = guiY + HEADER + 20;
        drawString(fontRendererObj, "config", x, y, TEXT);
        drawString(fontRendererObj, "VapeVlite automatically saves module states", x, y + 24, DIM);
        drawString(fontRendererObj, "and settings when the GUI closes.", x, y + 38, DIM);
        drawRect(x, y + 58, x + 145, y + 78, HOVER);
        drawString(fontRendererObj, "saved automatically", x + 10, y + 64, ACCENT);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton != 0) {
            super.mouseClicked(mouseX, mouseY, mouseButton);
            return;
        }

        // Sidebar categories.
        int y = guiY + 39;
        for (Category cat : Category.values()) {
            if (inside(mouseX, mouseY, guiX + 6, y - 2, guiX + SIDEBAR - 6, y + 18)) {
                category = cat;
                configTab = false;
                selected = firstModule(cat);
                moduleScroll = 0;
                optionScroll = 0;
                listening = false;
                return;
            }
            y += 23;
        }

        int configY = guiY + HEIGHT - 29;
        if (inside(mouseX, mouseY, guiX + 6, configY - 2, guiX + SIDEBAR - 6, configY + 18)) {
            configTab = true;
            listening = false;
            return;
        }

        if (configTab || selected == null) return;

        // Module selection/toggle.
        int my = guiY + HEADER + 8 - moduleScroll;
        for (Module module : modulesFor(category)) {
            if (inside(mouseX, mouseY, guiX + MODULE_X, my,
                    guiX + MODULE_X + MODULE_W, my + 18)) {
                if (mouseX >= guiX + MODULE_X + 82) {
                    module.toggle();
                    VapeVlite.save();
                } else {
                    selected = module;
                    optionScroll = 0;
                }
                return;
            }
            my += 21;
        }

        // Keybind.
        int ox = guiX + OPTION_X;
        int oy = guiY + HEADER + 8 - optionScroll;
        if (inside(mouseX, mouseY, ox + 78, oy + 2, ox + OPTION_W, oy + 18)) {
            listening = true;
            return;
        }
        oy += 24;

        for (Setting<?> setting : selected.getSettings()) {
            int h = setting instanceof NumberSetting ? 27 : 22;
            if (inside(mouseX, mouseY, ox, oy, ox + OPTION_W, oy + h)) {
                if (setting instanceof BooleanSetting) {
                    ((BooleanSetting) setting).toggle();
                    VapeVlite.save();
                } else if (setting instanceof NumberSetting) {
                    NumberSetting n = (NumberSetting) setting;
                    if (mouseX < ox + 85) n.decrement();
                    else n.increment();
                    VapeVlite.save();
                }
                return;
            }
            oy += h;
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
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0 || configTab) return;

        if (wheel > 0) {
            moduleScroll = Math.max(0, moduleScroll - 21);
            optionScroll = Math.max(0, optionScroll - 22);
        } else {
            moduleScroll = Math.min(maxModuleScroll(), moduleScroll + 21);
            optionScroll = Math.min(maxOptionScroll(), optionScroll + 22);
        }
    }

    @Override
    public void onGuiClosed() {
        VapeVlite.save();
    }

    private Module firstModule(Category cat) {
        List<Module> modules = modulesFor(cat);
        return modules.isEmpty() ? null : modules.get(0);
    }

    private List<Module> modulesFor(Category cat) {
        List<Module> result = new ArrayList<Module>();
        for (Module module : VapeVlite.MODULES.getModules()) {
            if (module.getCategory() == cat) result.add(module);
        }
        return result;
    }

    private int maxModuleScroll() {
        int count = modulesFor(category).size();
        return Math.max(0, count * 21 - (HEIGHT - HEADER - 12));
    }

    private int maxOptionScroll() {
        if (selected == null) return 0;
        int total = 24;
        for (Setting<?> setting : selected.getSettings()) total += setting instanceof NumberSetting ? 27 : 22;
        return Math.max(0, total - (HEIGHT - HEADER - 8));
    }

    private static boolean inside(int mx, int my, int l, int t, int r, int b) {
        return mx >= l && mx <= r && my >= t && my <= b;
    }

    private static String keyName(int key) {
        if (key == Keyboard.KEY_NONE) return "none";
        String name = Keyboard.getKeyName(key);
        return name == null ? "none" : name.toLowerCase(Locale.US);
    }

    private static String format(double value) {
        if (value == Math.rint(value)) return Integer.toString((int) value);
        return String.format(Locale.US, "%.2f", value);
    }
}
