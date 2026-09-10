package br.vapevlite.modules;

import br.vapevlite.BooleanSetting;
import br.vapevlite.Category;
import br.vapevlite.Module;
import br.vapevlite.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;
import org.lwjgl.input.Mouse;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

/**
 * Backtrack-style client implementation based on the supplied CrewX/Myau behavior.
 * It keeps recent target positions and briefly uses the historical position while
 * an attack window is active. Network packet delaying itself belongs to CrewX's
 * private lag manager, so this project implements the observable target rewind
 * without importing CrewX's classes.
 */
public class Backtrack extends Module {
    private final NumberSetting minDelay = new NumberSetting("Min Delay", 100, 0, 1000, 10);
    private final NumberSetting maxDelay = new NumberSetting("Max Delay", 150, 0, 1000, 10);
    private final NumberSetting range = new NumberSetting("Range", 3, 0, 10, 0.1);
    private final NumberSetting chance = new NumberSetting("Chance", 100, 0, 100, 1);
    private final NumberSetting nextDelay = new NumberSetting("Next Backtrack Delay", 500, 0, 2000, 10);
    private final BooleanSetting pauseHurt = new BooleanSetting("Pause On Hurt Time", false);
    private final NumberSetting hurtThreshold = new NumberSetting("Hurt Time Threshold", 3, 0, 10, 1);
    private final BooleanSetting showPosition = new BooleanSetting("Show Position", true);
    private final NumberSetting manualWindow = new NumberSetting("Manual Hit Window", 500, 50, 2000, 10);

    private static final Random RANDOM = new Random();
    private final Deque<Snapshot> history = new ArrayDeque<Snapshot>();
    private EntityLivingBase manualTarget;
    private long manualTargetTime;
    private long nextAllowedTime;
    private boolean active;
    private Snapshot activeSnapshot;

    public Backtrack() {
        super("Backtrack", Category.COMBAT);
        addSetting(minDelay); addSetting(maxDelay); addSetting(range); addSetting(chance);
        addSetting(nextDelay); addSetting(pauseHurt); addSetting(hurtThreshold);
        addSetting(showPosition); addSetting(manualWindow);
    }

    @Override public void onDisable() { clear(); }

    @Override
    public void onClientTick() {
        if (!isEnabled()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null) return;

        EntityLivingBase target = resolveTarget(mc);
        if (target != null) {
            history.addLast(new Snapshot(target));
            while (history.size() > 40) history.removeFirst();

            if (Mouse.isButtonDown(0)) {
                manualTarget = target;
                manualTargetTime = System.currentTimeMillis();
            }
        }

        if (manualTarget != null && !manualTarget.isDead && System.currentTimeMillis() - manualTargetTime <= manualWindow.getValue()) {
            int min = (int)Math.min(minDelay.getValue(), maxDelay.getValue());
            int max = (int)Math.max(minDelay.getValue(), maxDelay.getValue());
            boolean chanceOk = RANDOM.nextInt(100) < (int)chance.getValue();
            boolean rangeOk = mc.thePlayer.getDistanceToEntity(manualTarget) <= range.getValue();
            boolean hurtPause = pauseHurt.getValue() && manualTarget.hurtTime >= hurtThreshold.getValue();
            if (!active && chanceOk && rangeOk && !hurtPause && System.currentTimeMillis() >= nextAllowedTime) {
                activeSnapshot = oldestUseful(manualTarget, min, max);
                active = activeSnapshot != null;
                if (active) nextAllowedTime = System.currentTimeMillis() + (long)nextDelay.getValue();
            }
        } else if (manualTarget != null) {
            manualTarget = null;
        }

        if (!Mouse.isButtonDown(0) && active) active = false;
    }

    private EntityLivingBase resolveTarget(Minecraft mc) {
        if (mc.objectMouseOver != null && mc.objectMouseOver.entityHit instanceof EntityLivingBase) {
            EntityLivingBase e = (EntityLivingBase)mc.objectMouseOver.entityHit;
            if (e != mc.thePlayer) return e;
        }
        EntityLivingBase best = null;
        double bestDist = range.getValue();
        for (Object o : mc.theWorld.loadedEntityList) {
            if (!(o instanceof EntityPlayer)) continue;
            EntityLivingBase e = (EntityLivingBase)o;
            if (e == mc.thePlayer || e.isDead) continue;
            double d = mc.thePlayer.getDistanceToEntity(e);
            if (d <= bestDist) { bestDist = d; best = e; }
        }
        return best;
    }

    private Snapshot oldestUseful(EntityLivingBase target, int min, int max) {
        long wanted = min + (max > min ? RANDOM.nextInt(max - min + 1) : 0);
        Snapshot chosen = null;
        long now = System.currentTimeMillis();
        for (Snapshot s : history) {
            if (s.entityId == target.getEntityId() && now - s.time >= wanted) { chosen = s; break; }
        }
        return chosen;
    }

    private void clear() { history.clear(); manualTarget = null; active = false; activeSnapshot = null; nextAllowedTime = 0L; }

    public boolean hasBacktrackPosition() { return active && activeSnapshot != null && showPosition.getValue(); }
    public Vec3 getBacktrackPosition() { return activeSnapshot == null ? null : activeSnapshot.position; }

    private static class Snapshot {
        final int entityId; final long time; final Vec3 position;
        Snapshot(EntityLivingBase e) { entityId = e.getEntityId(); time = System.currentTimeMillis(); position = new Vec3(e.posX, e.posY, e.posZ); }
    }
}
