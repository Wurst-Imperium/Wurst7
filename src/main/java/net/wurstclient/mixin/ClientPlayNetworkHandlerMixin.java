/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.ScreenHandlerType;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.AutoCraftHack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.PacketOutputListener.PacketOutputEvent;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin
	implements ClientPlayPacketListener
{
	private final AutoCraftHack autoCraft = WurstClient.INSTANCE.getHax().autoCraftHack;

	@Inject(at = {@At("HEAD")},
		method = {"sendPacket(Lnet/minecraft/network/Packet;)V"},
		cancellable = true)
	private void onSendPacket(Packet<?> packet, CallbackInfo ci)
	{
		if (packet instanceof PlayerInteractBlockC2SPacket) {
			PlayerInteractBlockC2SPacket blockPacket = (PlayerInteractBlockC2SPacket)packet;
			if (autoCraft.isEnabled())
				autoCraft.blockPositionClicked(blockPacket.getBlockHitResult().getBlockPos());
		}

		PacketOutputEvent event = new PacketOutputEvent(packet);
		EventManager.fire(event);
		
		if(event.isCancelled())
			ci.cancel();
	}

	@Inject(at = {@At("RETURN")},
			method = {"onScreenHandlerSlotUpdate(Lnet/minecraft/network/packet/s2c/play/ScreenHandlerSlotUpdateS2CPacket;)V"},
			cancellable = true)
	private void onScreenHandlerSlotUpdate(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo ci)
	{
		if (autoCraft.isEnabled())
			autoCraft.notifySlotUpdate(packet);
	}

	@Inject(at = {@At("RETURN")},
			method = {"onOpenScreen(Lnet/minecraft/network/packet/s2c/play/OpenScreenS2CPacket;)V"},
			cancellable = true)
	private void onOpenScreen(OpenScreenS2CPacket packet, CallbackInfo ci)
	{
		if (autoCraft.isEnabled() && packet.getScreenHandlerType() == ScreenHandlerType.SHULKER_BOX)
			autoCraft.storageContainerAccessed(packet.getSyncId());
	}

	@Inject(at = {@At("RETURN")},
			method = {"onInventory(Lnet/minecraft/network/packet/s2c/play/InventoryS2CPacket;)V"},
			cancellable = true)
	private void onInventory(InventoryS2CPacket packet, CallbackInfo ci)
	{
		if (autoCraft.isEnabled())
			autoCraft.storageContainerContent(packet.getSyncId(), packet.getContents());
	}
}
