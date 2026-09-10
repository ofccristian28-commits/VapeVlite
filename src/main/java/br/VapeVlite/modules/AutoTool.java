package br.vapevlite.modules;

import br.vapevlite.Category;
import br.vapevlite.Module;
import br.vapevlite.NumberSetting;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.input.Mouse;

/** Automatically selects the best hotbar tool for the block being mined. */
public class AutoTool extends Module {
    private final NumberSetting delay = new NumberSetting("Delay", 0.0, 0.0, 200.0, 10.0);
    private long lastSwitch;

    public AutoTool() {
        super("Auto Tool", Category.WORLD);
        addSetting(delay);
    }

    @Override
    public void onClientTick() {
        if (!isEnabled()) return;
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.thePlayer;
        if (player == null || mc.theWorld == null || mc.currentScreen != null || !Mouse.isButtonDown(0)) return;
        MovingObjectPosition mop = mc.objectMouseOver;
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;

        long now = System.currentTimeMillis();
        long wait = Math.max(0L, Math.min(200L, Math.round(delay.getValue())));
        if (now - lastSwitch < wait) return;

        BlockPos pos = mop.getBlockPos();
        Block block = mc.theWorld.getBlockState(pos).getBlock();
        int bestSlot = player.inventory.currentItem;
        float best = strength(player.getHeldItem(), block);

        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.inventory.getStackInSlot(slot);
            float value = strength(stack, block);
            if (value > best + 0.001F) {
                best = value;
                bestSlot = slot;
            }
        }

        if (bestSlot != player.inventory.currentItem) {
            player.inventory.currentItem = bestSlot;
            player.inventoryContainer.detectAndSendChanges();
            lastSwitch = now;
        }
    }

    private float strength(ItemStack stack, Block block) {
        if (stack == null) return 1.0F;
        try {
            return stack.getStrVsBlock(block);
        } catch (Throwable ignored) {
            return 1.0F;
        }
    }
}
