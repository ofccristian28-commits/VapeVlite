package me.vene.skilled.modules.mods.utility;

import me.vene.skilled.modules.Category;
import me.vene.skilled.modules.Module;
import me.vene.skilled.utilities.StringRegistry;
import me.vene.skilled.values.BooleanValue;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

/** Automatically selects the strongest hotbar tool for the block being mined. */
public class AutoTool extends Module {
    private final Minecraft mc = Minecraft.getMinecraft();
    private final BooleanValue onlyBreaking = new BooleanValue("Only Breaking", true);

    public AutoTool() {
        super(StringRegistry.register("Auto Tool"), 0, Category.U);
        addOption(onlyBreaking);
    }

    @Override
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null) return;
        if (onlyBreaking.getState() && !Mouse.isButtonDown(0)) return;
        if (mc.objectMouseOver == null || mc.objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;

        try {
            net.minecraft.util.BlockPos pos = mc.objectMouseOver.getBlockPos();
            if (pos == null) return;
            net.minecraft.block.state.IBlockState state = mc.theWorld.getBlockState(pos);
            ItemStack current = mc.thePlayer.getHeldItem();
            float bestSpeed = current == null ? 0.0f : current.getStrVsBlock(state);
            int bestSlot = mc.thePlayer.inventory.currentItem;

            for (int slot = 0; slot < 9; slot++) {
                ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
                if (stack == null) continue;
                float speed = stack.getStrVsBlock(state);
                if (speed > bestSpeed + 0.01f) {
                    bestSpeed = speed;
                    bestSlot = slot;
                }
            }
            if (bestSlot != mc.thePlayer.inventory.currentItem) mc.thePlayer.inventory.currentItem = bestSlot;
        } catch (Throwable ignored) {}
    }
}
