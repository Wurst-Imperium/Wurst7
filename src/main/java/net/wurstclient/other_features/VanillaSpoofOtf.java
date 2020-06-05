/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.other_features;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.impl.networking.CustomPayloadC2SPacketAccessor;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.minecraft.util.PacketByteBuf;
import net.wurstclient.DontBlock;
import net.wurstclient.events.PacketOutputListener.PacketOutputEvent;
import net.wurstclient.other_feature.OtherFeature;
import net.wurstclient.settings.CheckboxSetting;

@DontBlock
public final class VanillaSpoofOtf extends OtherFeature
{
	private final CheckboxSetting spoof = new CheckboxSetting("Spoof Vanilla", false);
	
	public VanillaSpoofOtf()
	{
		super("VanillaSpoof",
			"Is your server blocking Fabric? This feature will help\n"
			+ "you get around the block by pretending to be a vanilla client.");
		addSetting(spoof);
	}

	public void onSentPacket(PacketOutputEvent event)
	{
		if(!spoof.isChecked())
			return;
		
		if(!(event.getPacket() instanceof CustomPayloadC2SPacket))
			return;
		
		CustomPayloadC2SPacketAccessor packet = (CustomPayloadC2SPacketAccessor)event.getPacket();
		if(packet.getChannel().getNamespace().equals("minecraft") && packet.getChannel().getPath().equals("register"))
			event.cancel();
		
		if(packet.getChannel().getNamespace().equals("minecraft") && packet.getChannel().getPath().equals("brand"))
			event.setPacket(new CustomPayloadC2SPacket(CustomPayloadC2SPacket.BRAND,
				new PacketByteBuf(Unpooled.buffer()).writeString("vanilla")));
		
		if(packet.getChannel().getNamespace().equals("fabric"))
			event.cancel();
	}
}
