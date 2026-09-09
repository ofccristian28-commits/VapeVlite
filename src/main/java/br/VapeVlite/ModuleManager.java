package br.vapevlite;

import java.util.ArrayList;
import java.util.List;
import org.lwjgl.input.Keyboard;
import br.vapevlite.modules.*;

public class ModuleManager {
    private final List<Module> modules = new ArrayList<Module>();

    public ModuleManager() {
        modules.add(new AutoClicker());
        modules.add(new AimAssist());
        modules.add(new JumpReset());
        modules.add(new Backtrack());
        modules.add(new Reach());
        modules.get(0).setKeybind(Keyboard.KEY_NONE);
        modules.get(1).setKeybind(Keyboard.KEY_NONE);
        modules.get(2).setKeybind(Keyboard.KEY_NONE);
        modules.get(3).setKeybind(Keyboard.KEY_NONE);
        modules.get(4).setKeybind(Keyboard.KEY_NONE);
    }

    public List<Module> getModules() { return modules; }

    public Module get(String name) {
        for (Module m : modules) if (m.getName().equalsIgnoreCase(name)) return m;
        return null;
    }
}
