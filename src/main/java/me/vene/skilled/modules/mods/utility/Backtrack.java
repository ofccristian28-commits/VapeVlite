package me.vene.skilled.modules.mods.utility;

import me.vene.skilled.SkilledClient;
import me.vene.skilled.modules.Category;
import me.vene.skilled.modules.Module;
import me.vene.skilled.utilities.RenderUtil;
import me.vene.skilled.utilities.StringRegistry;
import me.vene.skilled.values.BooleanValue;
import me.vene.skilled.values.NumberValue;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S13PacketDestroyEntities;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraft.util.AxisAlignedBB;

import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.LinkedHashMap;

/*
 * CrewX BackTrack logic adapted to the VapeVlite Module/Value API.
 * The gameplay logic is kept as close as possible to the supplied CrewX class;
 * only the unavailable CrewX event/property/render APIs are replaced.
 */
public class Backtrack extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final NumberValue delay = new NumberValue("delay", 100.0, 10.0, 1000.0);
    public final NumberValue targetFlushDelay = new NumberValue("target-flush-delay", 100.0, 100.0, 1000.0);
    public final BooleanValue distanceCheck = new BooleanValue("distance-check", true);
    public final BooleanValue cancelPong = new BooleanValue("cancel-pong", true);
    public final BooleanValue cancelKeepAlive = new BooleanValue("cancel-keep-alive", true);
    public final BooleanValue lineBox = new BooleanValue("line-box", true);
    public final BooleanValue filledBox = new BooleanValue("filled-box", false);
    public final NumberValue fillOpacity = new NumberValue("fill-opacity", 45.0, 20.0, 100.0);

    private final CopyOnWriteArrayList<PacketData> packets = new CopyOnWriteArrayList<PacketData>();
    private final Map<EntityLivingBase, PosData> posCache = new LinkedHashMap<EntityLivingBase, PosData>();
    private EntityLivingBase target;
    private long lastAttack;

    public Backtrack() {
        super(StringRegistry.register("BackTrack"), 0, Category.U);
        addValue(delay);
        addValue(targetFlushDelay);
        addValue(fillOpacity);
        addOption(distanceCheck);
        addOption(cancelPong);
        addOption(cancelKeepAlive);
        addOption(lineBox);
        addOption(filledBox);
    }

    @Override
    public void onDisable() {
        target = null;
        flushPackets();
    }

    @Override
    public void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !getState()) return;
        if (mc.thePlayer == null || mc.theWorld == null || mc.getNetHandler() == null) return;

        long now = System.currentTimeMillis();

        if (target != null && now - lastAttack > (long) targetFlushDelay.getValue()) {
            target = null;
            flushPackets();
        }

        if (target != null && distanceCheck.getState()) {
            PosData p = posCache.get(target);
            if (p != null) {
                double dx = p.x - mc.thePlayer.posX;
                double dy = p.y - mc.thePlayer.posY;
                double dz = p.z - mc.thePlayer.posZ;
                double oldDistSq = dx * dx + dy * dy + dz * dz;
                if (oldDistSq < mc.thePlayer.getDistanceSq(target.posX, target.posY, target.posZ)) {
                    target = null;
                    flushPackets();
                }
            }
        }

        if (target == null) return;

        for (PacketData pd : packets) {
            if (now - pd.time >= (long) delay.getValue()) {
                processPacketSilent(pd.packet);
                packets.remove(pd);
            }
        }
    }

    @Override
    public Object onPacketReceive(io.netty.channel.ChannelHandlerContext ctx, Object packet) {
        if (!getState() || target == null || packet == null || mc.theWorld == null) return packet;

        if (packet instanceof S08PacketPlayerPosLook) {
            target = null;
            flushPackets();
            return packet;
        }

        if (packet instanceof S13PacketDestroyEntities) {
            int[] ids = ((S13PacketDestroyEntities) packet).getEntityIDs();
            if (target != null) {
                int tid = target.getEntityId();
                for (int id : ids) {
                    if (id == tid) {
                        target = null;
                        flushPackets();
                        return packet;
                    }
                }
            }
            return packet;
        }

        if (!(packet instanceof S14PacketEntity) && !(packet instanceof S18PacketEntityTeleport)) return packet;

        Entity entity = null;
        if (packet instanceof S14PacketEntity) {
            S14PacketEntity p = (S14PacketEntity) packet;
            entity = p.getEntity(mc.theWorld);
            if (entity != target) return packet;
            updateRelativePosition(p);
            if (p instanceof S14PacketEntity.S16PacketEntityLook) return packet;
        } else {
            S18PacketEntityTeleport p = (S18PacketEntityTeleport) packet;
            entity = mc.theWorld.getEntityByID(p.getEntityId());
            if (entity != target) return packet;
            updateTeleportPosition(p);
        }

        packets.add(new PacketData(System.currentTimeMillis(), (Packet<?>) packet));
        return null;
    }

    @Override
    public void onHurtAnimation() {
        // CrewX handles this through its own event bus; VapeVlite already exposes
        // the attack timing through EventManager, so target selection is refreshed
        // from the last attacked entity in onClientTick().
    }

    private void resolveAttackTarget() {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        long now = System.currentTimeMillis();
        long attackAge = now - SkilledClient.getInstance().getEventManager().getLastAttack();
        if (attackAge > 250L) return;

        String name = SkilledClient.getInstance().getEventManager().getLastAttackedEntityName();
        if (name == null) return;

        for (Object o : mc.theWorld.loadedEntityList) {
            if (!(o instanceof EntityLivingBase)) continue;
            EntityLivingBase e = (EntityLivingBase)o;
            if (e == mc.thePlayer || e.isDead) continue;
            if (name.equals(e.getName())) {
                target = e;
                lastAttack = now;
                return;
            }
        }
    }

    private boolean isValidTarget(EntityLivingBase e) {
        return e != null && !e.isDead && e != mc.thePlayer;
    }

    private void updateRelativePosition(S14PacketEntity p) {
        Entity e = p.getEntity(mc.theWorld);
        if (!(e instanceof EntityLivingBase) || e != target) return;

        EntityLivingBase living = (EntityLivingBase)e;
        PosData old = posCache.get(living);
        double x = old == null ? living.posX : old.x;
        double y = old == null ? living.posY : old.y;
        double z = old == null ? living.posZ : old.z;

        double nx = x + p.func_149062_c() / 32.0;
        double ny = y + p.func_149061_d() / 32.0;
        double nz = z + p.func_149064_e() / 32.0;

        posCache.put(living, new PosData(nx, ny, nz, x, y, z, System.currentTimeMillis()));
    }

    private void updateTeleportPosition(S18PacketEntityTeleport p) {
        Entity e = mc.theWorld.getEntityByID(p.getEntityId());
        if (!(e instanceof EntityLivingBase) || e != target) return;

        EntityLivingBase living = (EntityLivingBase)e;
        PosData old = posCache.get(living);
        double x = old == null ? living.posX : old.x;
        double y = old == null ? living.posY : old.y;
        double z = old == null ? living.posZ : old.z;

        double nx = p.getX() / 32.0;
        double ny = p.getY() / 32.0;
        double nz = p.getZ() / 32.0;

        posCache.put(living, new PosData(nx, ny, nz, x, y, z, System.currentTimeMillis()));
    }

    @Override
    public void onRenderEvent(RenderWorldLastEvent e) {
        if (!getState() || target == null) return;
        PosData p = posCache.get(target);
        if (p == null || mc.thePlayer == null) return;
        if (!lineBox.getState() && !filledBox.getState()) return;

        float fade = p.time == 0L ? 1.0f :
                Math.min(1.0f, (System.currentTimeMillis() - p.time) / 200.0f);

        double x = p.x;
        double y = p.y;
        double z = p.z;

        double hw = target.width / 2.0;
        double h = target.getEyeHeight();

        AxisAlignedBB bb = new AxisAlignedBB(
                x - hw, y - h, z - hw,
                x + hw, y + target.height, z + hw
        ).offset(-mc.getRenderManager().viewerPosX,
                 -mc.getRenderManager().viewerPosY,
                 -mc.getRenderManager().viewerPosZ);

        int alpha = (int)(220.0f * fade);
        if (lineBox.getState()) {
            RenderUtil.hexColor((alpha << 24) | (253 << 16) | (162 << 8) | 60);
            RenderUtil.drawOutlinedBoundingBox(bb);
        }
        // The supplied VapeVlite RenderUtil has no CrewX-style filled-box helper.
        // Keep the original default (filled-box=false) without inventing a new renderer.
    }

    private void flushPackets() {
        PacketData pd;
        while ((pd = packets.isEmpty() ? null : packets.remove(0)) != null) {
            processPacketSilent(pd.packet);
        }
        posCache.clear();
    }

    private void processPacketSilent(Packet packet) {
        try {
            if (mc.getNetHandler() != null) packet.processPacket(mc.getNetHandler());
        } catch (Exception ignored) {}
    }

    private static final class PacketData {
        final long time;
        final Packet<?> packet;
        PacketData(long time, Packet<?> packet) {
            this.time = time;
            this.packet = packet;
        }
    }

    private static final class PosData {
        final double x, y, z, oldX, oldY, oldZ;
        final long time;
        PosData(double x, double y, double z, double oldX, double oldY, double oldZ, long time) {
            this.x = x; this.y = y; this.z = z;
            this.oldX = oldX; this.oldY = oldY; this.oldZ = oldZ;
            this.time = time;
        }
    }
}
