package net.wurstclient.hacks;

import com.google.common.eventbus.Subscribe;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;

@SearchTags({"AutoTotem", "Totem", "Totem of Undying", "Totem hack"})
public class AutoTotemHack extends Hack implements UpdateListener{
    public AutoTotemHack() {
        super("AutoTotem", "Automatically equips totems of undying for you");
        setCategory(Category.COMBAT);
    }

    private MinecraftClient mc = MinecraftClient.getInstance();

    @Override
    public void onEnable(){
        EVENTS.add(UpdateListener.class, this);
    }
    @Override
    public void onDisable(){
        EVENTS.remove(UpdateListener.class, this);
    }

    @Override
    public void onUpdate() {
        if (mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) return;

        for (int i = 9; i < 44; i++) {
            if (mc.player.inventory.getStack(i).getItem() == Items.TOTEM_OF_UNDYING) {
                mc.interactionManager.clickSlot(0, 0, i, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(1, 0, 45, SlotActionType.PICKUP, mc.player);
                return;
            }
        }

        for (int i = 0; i < 8; i++) {
            if (mc.player.inventory.getStack(i).getItem() == Items.TOTEM_OF_UNDYING) {
                mc.player.inventory.selectedSlot = i;
                mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(
                        PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
                return;
            }
        }
    }
}


