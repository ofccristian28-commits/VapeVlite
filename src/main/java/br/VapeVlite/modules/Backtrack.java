package br.vapevlite.modules;

import br.vapevlite.BooleanSetting;
import br.vapevlite.Category;
import br.vapevlite.Module;
import br.vapevlite.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class Backtrack extends Module {
    private final NumberSetting minDelay = new NumberSetting("Min Delay", 200, 0, 1000, 25);
    private final NumberSetting maxDelay = new NumberSetting("Max Delay", 400, 0, 1000, 25);
    private final BooleanSetting randomize = new BooleanSetting("Randomize", true);
    private final BooleanSetting showPosition = new BooleanSetting("Show Position", false);
    private final BooleanSetting lagBased = new BooleanSetting("Lag Based", false);
    private final BooleanSetting onlyWhileAttacking = new BooleanSetting("Only While Attacking", true);
    private final Map<Integer, Deque<Snapshot>> history = new HashMap<Integer, Deque<Snapshot>>();

    public Backtrack() {
        super("Backtrack", Category.COMBAT);
        addSetting(minDelay);
        addSetting(maxDelay);
        addSetting(randomize);
        addSetting(showPosition);
        addSetting(lagBased);
        addSetting(onlyWhileAttacking);
    }

    @Override
    protected void onDisable() { history.clear(); }

    @Override
    public void onClientTick() {
        Minecraft mc = Minecraft.getMinecraft();
        if (!isEnabled() || mc.theWorld == null || mc.thePlayer == null) return;
        if (onlyWhileAttacking.getValue() && !Mouse.isButtonDown(0)) return;

        long now = System.currentTimeMillis();
        for (Object o : mc.theWorld.loadedEntityList) {
            if (!(o instanceof EntityLivingBase)) continue;
            EntityLivingBase e = (EntityLivingBase)o;
            if (e == mc.thePlayer || e.isDead) continue;
            Deque<Snapshot> q = history.get(e.getEntityId());
            if (q == null) { q = new ArrayDeque<Snapshot>(); history.put(e.getEntityId(), q); }
            q.addLast(new Snapshot(now, e.posX, e.posY, e.posZ));
            while (!q.isEmpty() && now - q.peekFirst().time > 1500L) q.removeFirst();
        }
    }

    public Snapshot getSnapshot(EntityLivingBase entity) {
        Deque<Snapshot> q = history.get(entity.getEntityId());
        if (q == null || q.isEmpty()) return null;
        long wanted = System.currentTimeMillis() - getDelay();
        Snapshot best = q.peekFirst();
        for (Snapshot s : q) {
            if (s.time <= wanted) best = s; else break;
        }
        return best;
    }

    private long getDelay() {
        long low = Math.round(Math.min(minDelay.getValue(), maxDelay.getValue()));
        long high = Math.round(Math.max(minDelay.getValue(), maxDelay.getValue()));
        long base = randomize.getValue() && high > low
                ? low + (long)(Math.random() * (high - low + 1L)) : low;
        if (!lagBased.getValue()) return base;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.getNetHandler() == null) return base;
        NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getGameProfile().getId());
        int ping = info == null ? 0 : info.getResponseTime();
        return Math.max(base, base + Math.min(200, Math.max(0, ping)) / 2L);
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!isEnabled() || !showPosition.getValue()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) return;
        if (onlyWhileAttacking.getValue() && !Mouse.isButtonDown(0)) return;

        RenderManager rm = mc.getRenderManager();
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GL11.glLineWidth(2.0F);
        for (Object o : mc.theWorld.loadedEntityList) {
            if (!(o instanceof EntityLivingBase)) continue;
            EntityLivingBase e = (EntityLivingBase)o;
            if (e == mc.thePlayer || e.isDead) continue;
            Snapshot s = getSnapshot(e);
            if (s == null) continue;
            double x = s.x - rm.viewerPosX;
            double y = s.y - rm.viewerPosY;
            double z = s.z - rm.viewerPosZ;
            AxisAlignedBB box = new AxisAlignedBB(x - e.width / 2.0D, y, z - e.width / 2.0D,
                    x + e.width / 2.0D, y + e.height, z + e.width / 2.0D);
            net.minecraft.client.renderer.RenderGlobal.drawOutlinedBoundingBox(box, 0xFFFFFFFF);
        }
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    public static class Snapshot {
        public final long time;
        public final double x, y, z;
        public Snapshot(long time, double x, double y, double z) {
            this.time = time; this.x = x; this.y = y; this.z = z;
        }
    }
}
