/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.network.Connection;
import net.minecraft.network.TickablePacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.wurstclient.WurstClient;
import net.wurstclient.util.ChatUtils;

@Mixin(ClientPacketListener.class)
public abstract class ClientPlayNetworkHandlerMixin
	extends ClientCommonPacketListenerImpl
	implements TickablePacketListener, ClientGamePacketListener
{
	private ClientPlayNetworkHandlerMixin(WurstClient wurst, Minecraft client,
		Connection connection, CommonListenerCookie connectionState)
	{
		super(client, connection, connectionState);
	}
	
	@Inject(
		method = "handleLogin(Lnet/minecraft/network/protocol/game/ClientboundLoginPacket;)V",
		at = @At("TAIL"))
	public void onOnGameJoin(ClientboundLoginPacket packet, CallbackInfo ci)
	{
		WurstClient wurst = WurstClient.INSTANCE;
		if(!wurst.isEnabled())
			return;
		
		// Remove Mojang's dishonest warning toast on safe servers
		if(!packet.enforcesSecureChat())
		{
			minecraft.getToastManager().queued.removeIf(toast -> toast
				.getToken() == SystemToast.SystemToastId.UNSECURE_SERVER_WARNING);
			return;
		}
		
		// Add an honest warning toast on unsafe servers
		MutableComponent title = Component.literal(ChatUtils.WURST_PREFIX
			+ wurst.translate("toast.wurst.nochatreports.unsafe_server.title"));
		MutableComponent message = Component.literal(
			wurst.translate("toast.wurst.nochatreports.unsafe_server.message"));
		
		SystemToast systemToast = SystemToast.multiline(minecraft,
			SystemToast.SystemToastId.UNSECURE_SERVER_WARNING, title, message);
		minecraft.getToastManager().addToast(systemToast);
	}
	
	@Inject(
		method = "updateLevelChunk(IILnet/minecraft/network/protocol/game/ClientboundLevelChunkPacketData;)V",
		at = @At("TAIL"))
	private void onLoadChunk(int x, int z,
		ClientboundLevelChunkPacketData chunkData, CallbackInfo ci)
	{
		WurstClient.INSTANCE.getHax().newChunksHack.afterLoadChunk(x, z);
	}
	
	@Inject(
		method = "handleBlockUpdate(Lnet/minecraft/network/protocol/game/ClientboundBlockUpdatePacket;)V",
		at = @At("TAIL"))
	private void onOnBlockUpdate(ClientboundBlockUpdatePacket packet,
		CallbackInfo ci)
	{
		WurstClient.INSTANCE.getHax().newChunksHack
			.afterUpdateBlock(packet.getPos());
	}
	
	@Inject(
		method = "handleChunkBlocksUpdate(Lnet/minecraft/network/protocol/game/ClientboundSectionBlocksUpdatePacket;)V",
		at = @At("TAIL"))
	private void onOnChunkDeltaUpdate(
		ClientboundSectionBlocksUpdatePacket packet, CallbackInfo ci)
	{
		packet.runUpdates(
			(pos, state) -> WurstClient.INSTANCE.getHax().newChunksHack
				.afterUpdateBlock(pos));
	}
}
