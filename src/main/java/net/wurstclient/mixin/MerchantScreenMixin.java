/**
 * 
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.packet.c2s.play.SelectMerchantTradeC2SPacket;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.TradeFillHack;

/**
 * villager
 * 
 * @author yuanlu
 *
 */
@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin 
extends HandledScreen<MerchantScreenHandler>
implements ScreenHandlerProvider<MerchantScreenHandler>{

	public MerchantScreenMixin(WurstClient wurst,
			MerchantScreenHandler container,
		PlayerInventory playerInventory, Text name)
	{
		super(container, playerInventory, name);
	}
	
	@Shadow
	private int selectedIndex;

	private final TradeFillHack tradeFill =
		WurstClient.INSTANCE.getHax().tradeFillHack;
	
	@Override
	protected void onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType) {
		super.onMouseClick(slot, slotId, button, actionType);
		
		if (tradeFill.isEnabled())tradeFill.nextTick(this::fillTrade);
		
	}
	
	private void fillTrade() {
		var handler = ((MerchantScreenHandler) super.handler);
		handler.switchTo(this.selectedIndex);
        this.client.getNetworkHandler().sendPacket(new SelectMerchantTradeC2SPacket(this.selectedIndex));
	}
}
