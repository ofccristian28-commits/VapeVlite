package br.vapevlite.modules;

import br.vapevlite.BooleanSetting;
import br.vapevlite.Category;
import br.vapevlite.Module;
import br.vapevlite.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.EntityLivingBase;

import java.lang.reflect.Field;

/** Low-end renderer preset. Values are restored when the module is disabled. */
public class FpsOptimizer extends Module {
    private final BooleanSetting noShadows = new BooleanSetting("Remove Shadows", true);
    private final BooleanSetting noPortals = new BooleanSetting("Remove Portals", true);
    private final BooleanSetting noParticles = new BooleanSetting("Reduce Particles", true);
    private final BooleanSetting noClouds = new BooleanSetting("Remove Clouds", true);
    private final BooleanSetting lowGraphics = new BooleanSetting("Low Graphics", true);
    private final BooleanSetting lowViewDistance = new BooleanSetting("Low Render Distance", true);
    private final NumberSetting renderDistance = new NumberSetting("Render Distance", 4, 2, 12, 1);

    private boolean saved;
    private boolean oldFancy, oldAo, oldShadows;
    private int oldParticles, oldDistance;
    private Object oldClouds;
    private Object oldPortal;

    public FpsOptimizer() {
        super("FPS Optimizer", Category.RENDER);
        addSetting(noShadows); addSetting(noPortals); addSetting(noParticles); addSetting(noClouds);
        addSetting(lowGraphics); addSetting(lowViewDistance); addSetting(renderDistance);
    }

    @Override public void onEnable() { saved = false; apply(); }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getMinecraft();
        if (!saved || mc.gameSettings == null) return;
        GameSettings g = mc.gameSettings;
        g.fancyGraphics = oldFancy;
        g.ambientOcclusion = oldAo;
        g.particleSetting = oldParticles;
        g.renderDistanceChunks = oldDistance;
        setField(g, "entityShadows", "field_74347_j", oldShadows);
        restoreOptional(g, "clouds", "field_74335_Z", oldClouds);
        if (mc.thePlayer != null) setField(mc.thePlayer, "timeInPortal", "field_71086_bY", oldPortal);
        saved = false;
    }

    @Override public void onClientTick() { if (isEnabled()) apply(); }

    private void apply() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings == null) return;
        GameSettings g = mc.gameSettings;
        if (!saved) {
            oldFancy = g.fancyGraphics;
            oldAo = g.ambientOcclusion;
            oldParticles = g.particleSetting;
            oldDistance = g.renderDistanceChunks;
            oldShadows = getBoolean(g, "entityShadows", "field_74347_j", true);
            oldClouds = getField(g, "clouds", "field_74335_Z");
            if (mc.thePlayer != null) oldPortal = getField(mc.thePlayer, "timeInPortal", "field_71086_bY");
            saved = true;
        }
        if (lowGraphics.getValue()) {
            g.fancyGraphics = false;
            g.ambientOcclusion = false;
        }
        if (noParticles.getValue()) g.particleSetting = 2;
        if (lowViewDistance.getValue()) g.renderDistanceChunks = (int)renderDistance.getValue();
        if (noShadows.getValue()) setField(g, "entityShadows", "field_74347_j", false);
        if (noClouds.getValue()) setField(g, "clouds", "field_74335_Z", 0);
        if (noPortals.getValue() && mc.thePlayer != null) setField(mc.thePlayer, "timeInPortal", "field_71086_bY", 0F);
    }

    private static Field findField(Class<?> c, String... names) {
        for (String n : names) try { return c.getDeclaredField(n); } catch (Throwable ignored) {}
        return null;
    }
    private static Object getField(Object o, String... names) {
        try { Field f = findField(o.getClass(), names); if (f == null) return null; f.setAccessible(true); return f.get(o); } catch (Throwable ignored) { return null; }
    }
    private static boolean getBoolean(Object o, String a, String b, boolean d) {
        Object v = getField(o, a, b); return v instanceof Boolean ? (Boolean)v : d;
    }
    private static void setField(Object o, String a, String b, Object v) {
        try { Field f = findField(o.getClass(), a, b); if (f == null) return; f.setAccessible(true); f.set(o, v); } catch (Throwable ignored) {}
    }
    private static void restoreOptional(Object o, String a, String b, Object v) { if (v != null) setField(o, a, b, v); }
}
