package br.vapevlite;

import java.util.ArrayList;
import java.util.List;
import br.vapevlite.modules.*;

public class ModuleManager {
    private final List<Module> modules = new ArrayList<Module>();
    public ModuleManager(){
        modules.add(new AutoClicker());
        modules.add(new AimAssist());
        modules.add(new JumpReset());
        modules.add(new Backtrack());
        modules.add(new Reach());
    }
    public List<Module> getModules(){return modules;}
}
