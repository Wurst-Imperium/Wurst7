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
import net.minecraft.client.toast.SystemToast;
import net.minecraft.network.Packet;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.s2c.play.ServerMetadataS2CPacket;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.wurstclient.WurstClient;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.PacketOutputListener.PacketOutputEvent;
import net.wurstclient.util.ChatUtils;

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
		method = "onServerMetadata(Lnet/minecraft/network/packet/s2c/play/ServerMetadataS2CPacket;)V")
	public void onOnServerMetadata(ServerMetadataS2CPacket packet,
		CallbackInfo ci)
	{
		if(!WurstClient.INSTANCE.isEnabled())
			return;
		
		// Remove Mojang's dishonest warning toast on safe servers
		if(!packet.isSecureChatEnforced())
		{
			client.getToastManager().toastQueue.removeIf(toast -> toast
				.getType() == SystemToast.Type.UNSECURE_SERVER_WARNING);
			return;
		}
		
		// Add an honest warning toast on unsafe servers
		MutableText title = Text.literal(ChatUtils.WURST_PREFIX).append(
			Text.translatable("toast.wurst.nochatreports.unsafe_server.title"));
		MutableText message = Text
			.translatable("toast.wurst.nochatreports.unsafe_server.message");
		
		SystemToast systemToast = SystemToast.create(client,
			SystemToast.Type.UNSECURE_SERVER_WARNING, title, message);
		client.getToastManager().add(systemToast);
	}
}
