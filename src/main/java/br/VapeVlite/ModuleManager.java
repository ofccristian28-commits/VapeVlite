package br.vapevlite;

import java.util.ArrayList;
import java.util.List;
import br.vapevlite.modules.*;

public class ModuleManager {
    private final List<Module> modules = new ArrayList<Module>();

    public ModuleManager() {
        modules.add(new ClickGUI());
        modules.add(new FpsOptimizer());
        modules.add(new AutoClicker());
        modules.add(new AimAssist());
        modules.add(new JumpReset());
        modules.add(new Backtrack());
        modules.add(new Reach());
        modules.add(new NoJumpDelay());
        modules.add(new NoHitDelay());
        modules.add(new FastPlace());
        modules.add(new AutoTool());
    }

    public List<Module> getModules() { return modules; }

    public Module get(String name) {
        for (Module m : modules) if (m.getName().equalsIgnoreCase(name)) return m;
        return null;
    }
}
