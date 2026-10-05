/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.ConnectionPacketOutputListener.ConnectionPacketOutputEvent;
import net.wurstclient.events.PacketInputListener.PacketInputEvent;

@Mixin(Connection.class)
public abstract class ConnectionMixin
	extends SimpleChannelInboundHandler<Packet<?>>
{
	private ConcurrentLinkedQueue<ConnectionPacketOutputEvent> events =
		new ConcurrentLinkedQueue<>();
	private boolean sentPositionThisTick;
	
	/**
	 * Since 26.3-snapshot-10, {@link ServerGamePacketListenerImpl} rejects
	 * multiple position packets in a single client tick. This mixin inserts
	 * {@link ServerboundClientTickEndPacket}s as needed to prevent that.
	 */
	@Inject(
		method = "doSendPacket(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V",
		at = @At("HEAD"))
	private void onDoSendPacket(Packet<?> packet,
		@Nullable ChannelFutureListener listener, boolean flush,
		CallbackInfo ci)
	{
		if(packet instanceof ServerboundClientTickEndPacket)
		{
			sentPositionThisTick = false;
			return;
		}
		
		if(!(packet instanceof ServerboundMovePlayerPacket move)
			|| !move.hasPosition())
			return;
		
		if(sentPositionThisTick)
			sendPacket(ServerboundClientTickEndPacket.INSTANCE, null, false);
		
		sentPositionThisTick = true;
	}
	
	@Shadow
	private void sendPacket(Packet<?> packet,
		@Nullable ChannelFutureListener listener, boolean flush)
	{
		
	}
	
	@Inject(
		method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/network/Connection;genericsFtw(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;)V",
			ordinal = 0),
		cancellable = true)
	private void onChannelRead0(ChannelHandlerContext context, Packet<?> packet,
		CallbackInfo ci)
	{
		PacketInputEvent event = new PacketInputEvent(packet);
		EventManager.fire(event);
		
		if(event.isCancelled())
			ci.cancel();
	}
	
	// These mixins target the second "send" method. The one with two arguments.
	
	@ModifyVariable(
		method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V",
		at = @At("HEAD"))
	public Packet<?> modifyPacket(Packet<?> packet)
	{
		ConnectionPacketOutputEvent event =
			new ConnectionPacketOutputEvent(packet);
		events.add(event);
		EventManager.fire(event);
		return event.getPacket();
	}
	
	@Inject(
		method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V",
		at = @At("HEAD"),
		cancellable = true)
	private void onSend(Packet<?> packet,
		@Nullable ChannelFutureListener callback, CallbackInfo ci)
	{
		ConnectionPacketOutputEvent event = getEvent(packet);
		if(event == null)
			return;
		
		if(event.isCancelled())
			ci.cancel();
		
		events.remove(event);
	}
	
	private ConnectionPacketOutputEvent getEvent(Packet<?> packet)
	{
		for(ConnectionPacketOutputEvent event : events)
			if(event.getPacket() == packet)
				return event;
			
		return null;
	}
}
