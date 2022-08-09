/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketCallbacks;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.ConnectionPacketOutputListener.ConnectionPacketOutputEvent;
import net.wurstclient.events.PacketInputListener.PacketInputEvent;

@Mixin(ClientConnection.class)
public abstract class ClientConnectionMixin
	extends SimpleChannelInboundHandler<Packet<?>>
{
	private ConcurrentLinkedQueue<ConnectionPacketOutputEvent> events =
		new ConcurrentLinkedQueue<>();
	
	@Inject(at = {@At(value = "INVOKE",
		target = "Lnet/minecraft/network/ClientConnection;handlePacket(Lnet/minecraft/network/Packet;Lnet/minecraft/network/listener/PacketListener;)V",
		ordinal = 0)},
		method = {
			"channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/Packet;)V"},
		cancellable = true)
	private void onChannelRead0(ChannelHandlerContext channelHandlerContext,
		Packet<?> packet, CallbackInfo ci)
	{
		PacketInputEvent event = new PacketInputEvent(packet);
		EventManager.fire(event);
		
		if(event.isCancelled())
			ci.cancel();
	}
	
	@ModifyVariable(
		method = "send(Lnet/minecraft/network/Packet;Lnet/minecraft/network/PacketCallbacks;)V",
		at = @At("HEAD"))
	public Packet<?> onSendPacket(Packet<?> packet)
	{
		ConnectionPacketOutputEvent event =
			new ConnectionPacketOutputEvent(packet);
		events.add(event);
		EventManager.fire(event);
		return event.getPacket();
	}
	
	@Inject(at = {@At(value = "HEAD")},
		method = {
			"send(Lnet/minecraft/network/Packet;Lnet/minecraft/network/PacketCallbacks;)V"},
		cancellable = true)
	private void onSendPacket(Packet<?> packet,
		@Nullable PacketCallbacks callback, CallbackInfo ci)
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
