/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.wurstclient.WurstClient;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.PacketOutputListener.PacketOutputEvent;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin
	implements ClientPlayPacketListener
{
	@Shadow
	@Final
	private MinecraftClient client;
	
	@Inject(at = {@At("HEAD")},
		method = {"sendPacket(Lnet/minecraft/network/Packet;)V"},
		cancellable = true)
	private void onSendPacket(Packet<?> packet, CallbackInfo ci)
	{
		PacketOutputEvent event = new PacketOutputEvent(packet);
		EventManager.fire(event);
		
		if(event.isCancelled())
			ci.cancel();
	}
	
	@Inject(at = @At("TAIL"),
		method = "onChunkData(Lnet/minecraft/network/packet/s2c/play/ChunkDataS2CPacket;)V")
	private void onLoadChunk(ChunkDataS2CPacket packet, CallbackInfo ci)
	{
		WurstClient.INSTANCE.getHax().newChunksHack
			.afterLoadChunk(packet.getX(), packet.getZ());
	}
	
	@Inject(at = @At("TAIL"),
		method = "onBlockUpdate(Lnet/minecraft/network/packet/s2c/play/BlockUpdateS2CPacket;)V")
	private void onOnBlockUpdate(BlockUpdateS2CPacket packet, CallbackInfo ci)
	{
		WurstClient.INSTANCE.getHax().newChunksHack
			.afterUpdateBlock(packet.getPos());
	}
	
	@Inject(at = @At("TAIL"),
		method = "onChunkDeltaUpdate(Lnet/minecraft/network/packet/s2c/play/ChunkDeltaUpdateS2CPacket;)V")
	private void onOnChunkDeltaUpdate(ChunkDeltaUpdateS2CPacket packet,
		CallbackInfo ci)
	{
		packet.visitUpdates(
			(pos, state) -> WurstClient.INSTANCE.getHax().newChunksHack
				.afterUpdateBlock(pos));
	}
}
