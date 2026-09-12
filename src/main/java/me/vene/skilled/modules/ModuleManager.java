package me.vene.skilled.modules;

import java.util.ArrayList;
import me.vene.skilled.modules.mods.combat.AimAssist;
import me.vene.skilled.modules.mods.combat.AutoClicker;
import me.vene.skilled.modules.mods.combat.Reach;
import me.vene.skilled.modules.mods.combat.Sprint;
import me.vene.skilled.modules.mods.main.CombatGUI;
import me.vene.skilled.modules.mods.main.OtherGUI;
import me.vene.skilled.modules.mods.main.PlayerGUI;
import me.vene.skilled.modules.mods.main.RenderGUI;
import me.vene.skilled.modules.mods.main.UtilityGUI;
import me.vene.skilled.modules.mods.other.ClickGUI;
import me.vene.skilled.modules.mods.player.TimerModule;
import me.vene.skilled.modules.mods.player.JumpDelay;
import me.vene.skilled.modules.mods.combat.HitDelay;
import me.vene.skilled.modules.mods.player.WTap;
import me.vene.skilled.modules.mods.render.Chams;
import me.vene.skilled.modules.mods.render.Fullbright;
import me.vene.skilled.modules.mods.render.PlayerESP;
import me.vene.skilled.modules.mods.render.SkinChanger;
import me.vene.skilled.modules.mods.utility.Array;
import me.vene.skilled.modules.mods.utility.InfoTab;
import me.vene.skilled.modules.mods.utility.Refill;
import me.vene.skilled.modules.mods.utility.AutoTool;
import me.vene.skilled.modules.mods.utility.FastPlace;
import me.vene.skilled.modules.mods.utility.Backtrack;

public class ModuleManager {
    private static ArrayList<Module> modules;

    public static ArrayList<Module> getModules() { return ModuleManager.modules; }
    public static void clearShit() { ModuleManager.modules.clear(); }

    static {
        ModuleManager.modules = new ArrayList<Module>();
        ModuleManager.modules.add(new ClickGUI());
        ModuleManager.modules.add(new CombatGUI());
        ModuleManager.modules.add(new AimAssist());
        ModuleManager.modules.add(new AutoClicker());
        ModuleManager.modules.add(new Array());
        ModuleManager.modules.add(new InfoTab());
        ModuleManager.modules.add(new OtherGUI());
        ModuleManager.modules.add(new PlayerGUI());
        ModuleManager.modules.add(new Reach());
        ModuleManager.modules.add(new HitDelay());
        ModuleManager.modules.add(new RenderGUI());
        ModuleManager.modules.add(new UtilityGUI());
        ModuleManager.modules.add(new WTap());
        ModuleManager.modules.add(new Fullbright());
        ModuleManager.modules.add(new Sprint());
        ModuleManager.modules.add(new TimerModule());
        ModuleManager.modules.add(new JumpDelay());
        ModuleManager.modules.add(new Chams());
        ModuleManager.modules.add(new PlayerESP());
        ModuleManager.modules.add(new SkinChanger());
        ModuleManager.modules.add(new Refill());
        ModuleManager.modules.add(new AutoTool());
        ModuleManager.modules.add(new FastPlace());
        ModuleManager.modules.add(new Backtrack());
    }
}
