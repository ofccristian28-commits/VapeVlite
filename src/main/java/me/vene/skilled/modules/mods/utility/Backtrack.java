package me.vene.skilled.modules.mods.utility;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import me.vene.skilled.modules.Category;
import me.vene.skilled.modules.Module;
import me.vene.skilled.utilities.StringRegistry;
import me.vene.skilled.values.NumberValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/** Client-side backtrack: delays movement packets for nearby player entities. */
public class Backtrack extends Module {
    private final Minecraft mc = Minecraft.getMinecraft();
    private final NumberValue delay = new NumberValue("Delay", 120.0, 0.0, 500.0);
    private final NumberValue range = new NumberValue("Range", 6.0, 2.0, 12.0);
    private final Map<ScheduledFuture<?>, Boolean> scheduled = new ConcurrentHashMap<ScheduledFuture<?>, Boolean>();

    public Backtrack() {
        super(StringRegistry.register("Backtrack"), 0, Category.U);
        addValue(delay);
        addValue(range);
    }

    @Override
    public Object onPacketReceive(final ChannelHandlerContext ctx, final Object packet) {
        if (packet == null || mc.thePlayer == null || mc.theWorld == null) return packet;
        if (!isMovementPacket(packet)) return packet;
        Integer entityId = getEntityId(packet);
        if (entityId == null) return packet;
        Entity entity = mc.theWorld.getEntityByID(entityId);
        if (!(entity instanceof EntityOtherPlayerMP) || entity == mc.thePlayer) return packet;
        if (mc.thePlayer.getDistanceToEntity(entity) > (float) range.getValue()) return packet;

        final long delayMs = Math.max(0L, (long) delay.getValue());
        if (delayMs <= 0L) return packet;
        final Object retained = ReferenceCountUtil.retain(packet);
        ScheduledFuture<?> future = ctx.executor().schedule(new Runnable() {
            @Override public void run() {
                try { ctx.fireChannelRead(retained); }
                finally { scheduled.keySet().removeIf(k -> k.isDone() || k.isCancelled()); }
            }
        }, delayMs, TimeUnit.MILLISECONDS);
        scheduled.put(future, Boolean.TRUE);
        return null;
    }

    @Override
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) scheduled.keySet().removeIf(k -> k.isDone() || k.isCancelled());
    }

    @Override
    public void onDisable() {
        // Do not cancel queued packets; they will be delivered after their scheduled delay.
        scheduled.keySet().removeIf(k -> k.isDone() || k.isCancelled());
    }

    private static boolean isMovementPacket(Object packet) {
        if (packet instanceof S18PacketEntityTeleport) return true;
        String n = packet.getClass().getName();
        return n.endsWith("S14PacketEntity") || n.endsWith("S15PacketEntityRelMove") || n.endsWith("S16PacketEntityLook") || n.endsWith("S17PacketEntityLookMove");
    }

    private static Integer getEntityId(Object packet) {
        try {
            if (packet instanceof S18PacketEntityTeleport) return ((S18PacketEntityTeleport) packet).getEntityId();
            Method m = packet.getClass().getMethod("getEntityId");
            return ((Number) m.invoke(packet)).intValue();
        } catch (Throwable ignored) {}
        return null;
    }
}
