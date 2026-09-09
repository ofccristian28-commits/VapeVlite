package br.vapevlite.modules;

import br.vapevlite.BooleanSetting;
import br.vapevlite.Category;
import br.vapevlite.Module;
import br.vapevlite.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Independent recreation of the timing/position-history style observed in CrewX.
 * It does not copy the original bytecode.
 */
public class Backtrack extends Module {
    private final NumberSetting minDelay = new NumberSetting("Min Delay", 100, 0, 1000, 25);
    private final NumberSetting maxDelay = new NumberSetting("Max Delay", 150, 0, 1000, 25);
    private final NumberSetting range = new NumberSetting("Range", 3.0, 0.0, 10.0, 0.1);
    private final NumberSetting chance = new NumberSetting("Chance", 100, 0, 100, 5);
    private final NumberSetting nextBacktrackDelay = new NumberSetting("Next Backtrack Delay", 0, 0, 2000, 25);
    private final BooleanSetting pauseOnHurtTime = new BooleanSetting("Pause On HurtTime", false);
    private final NumberSetting hurtTimeThreshold = new NumberSetting("HurtTime Threshold", 3, 0, 10, 1);
    private final BooleanSetting showPosition = new BooleanSetting("Show Position", true);
    private final NumberSetting manualWindow = new NumberSetting("Manual Hit Window", 500, 50, 2000, 25);

    private final Map<Integer, Deque<Snapshot>> history = new HashMap<Integer, Deque<Snapshot>>();
    private final Random random = new Random();
    private EntityLivingBase manualTarget;
    private long manualTargetTime;
    private long nextAllowedTime;
    private int currentChance = -1;
    private boolean active;
    private Vec3 lastPosition;
    private Vec3 currentPosition;

    public Backtrack() {
        super("Backtrack", Category.COMBAT);
        addSetting(minDelay);
        addSetting(maxDelay);
        addSetting(range);
        addSetting(chance);
        addSetting(nextBacktrackDelay);
        addSetting(pauseOnHurtTime);
        addSetting(hurtTimeThreshold);
        addSetting(showPosition);
        addSetting(manualWindow);
    }

    @Override
    protected void onDisable() {
        history.clear();
        manualTarget = null;
        active = false;
        currentChance = -1;
        nextAllowedTime = 0L;
    }

    @Override
    public void onClientTick() {
        Minecraft mc = Minecraft.getMinecraft();
        if (!isEnabled() || mc.theWorld == null || mc.thePlayer == null) return;

        long now = System.currentTimeMillis();
        for (Object o : mc.theWorld.loadedEntityList) {
            if (!(o instanceof EntityLivingBase)) continue;
            EntityLivingBase e = (EntityLivingBase)o;
            if (e == mc.thePlayer || e.isDead) continue;

            Deque<Snapshot> q = history.get(e.getEntityId());
            if (q == null) {
                q = new ArrayDeque<Snapshot>();
                history.put(e.getEntityId(), q);
            }
            q.addLast(new Snapshot(now, e.posX, e.posY, e.posZ));
            while (!q.isEmpty() && now - q.peekFirst().time > 2000L) q.removeFirst();
        }

        if (Mouse.isButtonDown(0) && mc.objectMouseOver != null &&
                mc.objectMouseOver.entityHit instanceof EntityLivingBase) {
            manualTarget = (EntityLivingBase)mc.objectMouseOver.entityHit;
            manualTargetTime = now;
        } else if (manualTarget != null && now - manualTargetTime > manualWindow.getValue()) {
            manualTarget = null;
        }

        EntityLivingBase target = resolveTarget(mc);
        active = false;

        if (target != null && now >= nextAllowedTime) {
            if (currentChance < 0) currentChance = random.nextInt(100);
            boolean chanceOk = currentChance < chance.getValue();
            boolean rangeOk = mc.thePlayer.getDistanceToEntity(target) <= range.getValue();
            boolean hurtPaused = pauseOnHurtTime.getValue() &&
                    target.hurtTime >= hurtTimeThreshold.getValue();

            if (chanceOk && rangeOk && !hurtPaused) {
                active = true;
                lastPosition = currentPosition;
                currentPosition = new Vec3(target.posX, target.posY, target.posZ);
            }
        } else if (active) {
            active = false;
        }

        if (!active) {
            int delay = (int)Math.round(nextBacktrackDelay.getValue());
            if (delay > 0) nextAllowedTime = now + delay;
            currentChance = -1;
        }
    }

    private EntityLivingBase resolveTarget(Minecraft mc) {
        if (manualTarget != null && !manualTarget.isDead &&
                mc.thePlayer.getDistanceToEntity(manualTarget) <= range.getValue()) {
            return manualTarget;
        }
        if (mc.objectMouseOver != null && mc.objectMouseOver.entityHit instanceof EntityLivingBase) {
            return (EntityLivingBase)mc.objectMouseOver.entityHit;
        }
        return null;
    }

    public Snapshot getSnapshot(EntityLivingBase entity) {
        Deque<Snapshot> q = history.get(entity.getEntityId());
        if (q == null || q.isEmpty()) return null;

        long wanted = System.currentTimeMillis() - getDelay();
        Snapshot best = q.peekFirst();
        for (Snapshot s : q) {
            if (s.time <= wanted) best = s;
            else break;
        }
        return best;
    }

    private long getDelay() {
        long low = Math.round(Math.min(minDelay.getValue(), maxDelay.getValue()));
        long high = Math.round(Math.max(minDelay.getValue(), maxDelay.getValue()));
        if (high <= low) return low;
        return low + (long)random.nextInt((int)(high - low + 1L));
    }

    @SubscribeEvent
    public void onPacket(Object ignored) {
        // Packet hooks are intentionally not copied from the source client.
        // The active historical-position attack is performed in the client tick below.
    }

    public void attackAtBacktrack(EntityLivingBase target) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!isEnabled() || target == null || target.isDead || mc.thePlayer == null) return;
        Snapshot snap = getSnapshot(target);
        if (snap == null) return;

        double ox = target.posX, oy = target.posY, oz = target.posZ;
        try {
            target.setPosition(snap.x, snap.y, snap.z);
            mc.playerController.attackEntity(mc.thePlayer, target);
            mc.thePlayer.swingItem();
        } finally {
            target.setPosition(ox, oy, oz);
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!isEnabled() || !showPosition.getValue()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) return;
        if (!Mouse.isButtonDown(0)) return;

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
            AxisAlignedBB box = new AxisAlignedBB(
                    x - e.width / 2.0D, y, z - e.width / 2.0D,
                    x + e.width / 2.0D, y + e.height, z + e.width / 2.0D);
            net.minecraft.client.renderer.RenderGlobal.drawOutlinedBoundingBox(box, 255, 255, 255, 255);
        }

        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    public static class Snapshot {
        public final long time;
        public final double x, y, z;
        public Snapshot(long time, double x, double y, double z) {
            this.time = time;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
