package br.vapevlite.modules;

import br.vapevlite.Category;
import br.vapevlite.Module;
import br.vapevlite.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class Backtrack extends Module {
    private final NumberSetting delay = new NumberSetting("Delay", 100, 0, 500, 25);
    private final Map<Integer, Deque<Snapshot>> history = new HashMap<Integer, Deque<Snapshot>>();

    public Backtrack() {
        super("Backtrack", Category.COMBAT);
        addSetting(delay);
    }

    @Override
    public void onClientTick() {
        Minecraft mc = Minecraft.getMinecraft();
        if (!isEnabled() || mc.theWorld == null) return;
        long now = System.currentTimeMillis();
        for (Object o : mc.theWorld.loadedEntityList) {
            if (!(o instanceof EntityLivingBase)) continue;
            EntityLivingBase e = (EntityLivingBase)o;
            if (e == mc.thePlayer) continue;
            Deque<Snapshot> q = history.get(e.getEntityId());
            if (q == null) { q = new ArrayDeque<Snapshot>(); history.put(e.getEntityId(), q); }
            q.addLast(new Snapshot(now, e.posX, e.posY, e.posZ));
            while (!q.isEmpty() && now - q.peekFirst().time > 1200) q.removeFirst();
        }
    }

    public Snapshot getSnapshot(EntityLivingBase entity) {
        Deque<Snapshot> q = history.get(entity.getEntityId());
        if (q == null || q.isEmpty()) return null;
        long wanted = System.currentTimeMillis() - (long)delay.getValue();
        Snapshot best = q.peekFirst();
        for (Snapshot s : q) {
            if (s.time <= wanted) best = s; else break;
        }
        return best;
    }

    public static class Snapshot {
        public final long time;
        public final double x, y, z;
        public Snapshot(long time, double x, double y, double z) {
            this.time = time; this.x = x; this.y = y; this.z = z;
        }
    }
}
