package br.vapevlite;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    private final String name;
    private final Category category;
    private boolean enabled;
    private int keybind;
    private final List<Setting<?>> settings = new ArrayList<Setting<?>>();

    protected Module(String name, Category category) {
        this.name = name;
        this.category = category;
    }

    public String getName() { return name; }
    public Category getCategory() { return category; }
    public boolean isEnabled() { return enabled; }
    public int getKeybind() { return keybind; }
    public void setKeybind(int keybind) { this.keybind = keybind; }
    public List<Setting<?>> getSettings() { return settings; }

    protected void addSetting(Setting<?> setting) { settings.add(setting); }

    public void toggle() { setEnabled(!enabled); }

    public void setEnabled(boolean value) {
        if (enabled == value) return;
        enabled = value;
        if (value) {
            onEnable();
        } else {
            onDisable();
            VapeVlite.notifyModuleDisabled(name);
        }
    }

    protected void onEnable() {}
    protected void onDisable() {}
    public void onClientTick() {}
    public void onRenderTick() {}
}
