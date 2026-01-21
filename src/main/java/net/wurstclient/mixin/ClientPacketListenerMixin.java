/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.inventory.MenuType;
import net.wurstclient.data.MerchantPacket;
import net.wurstclient.util.TradingUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 客户端数据包监听器混入类，用于处理交易相关的数据包
 */
@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin
{
	
	/**
	 * 处理商人交易报价数据包
	 *
	 * @param packet
	 *            商人交易报价数据包
	 */
	@Inject(at = @At("HEAD"), method = "handleMerchantOffers")
	private void onHandleMerchantOffers(ClientboundMerchantOffersPacket packet,
										CallbackInfo ci)
	{
		if(TradingUtils.isEnableGuiLessTradeDetection())
		{
			MerchantPacket.packet = packet;
		}
	}
	
	@Inject(at = @At("HEAD"),
		method = "handleOpenScreen",
		cancellable = true,
		order = 0)
	private void onHandleOpenScreen(ClientboundOpenScreenPacket packet,
		CallbackInfo ci)
	{
		if(!TradingUtils.isEnableGuiLessTradeDetection())
		{
			return;
		}
		
		if(!TradingUtils.isWindowOpen()
			&& packet.getType() == MenuType.MERCHANT)
		{
			ci.cancel();
			wurst$closeContainer(packet.getContainerId());
		}
	}
	
	@Unique
	private void wurst$closeContainer(int containerId)
	{
		Minecraft minecraft = Minecraft.getInstance();
		if(minecraft.player != null)
		{
			minecraft.player.connection
				.send(new ServerboundContainerClosePacket(containerId));
		}
	}
}
