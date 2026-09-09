package br.vapevlite;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@Mod(modid="vapevlite", name="VapeVlite", version="1.0")
public class VapeVlite {
    public static final ModuleManager MODULES = new ModuleManager();

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(MODULES);
        for (Module m : MODULES.getModules()) MinecraftForge.EVENT_BUS.register(m);
    }
}
