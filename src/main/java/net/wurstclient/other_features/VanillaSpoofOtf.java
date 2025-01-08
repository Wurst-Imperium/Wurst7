/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.other_features;

import net.minecraft.network.packet.BrandCustomPayload;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.wurstclient.DontBlock;
import net.wurstclient.SearchTags;
import net.wurstclient.events.ConnectionPacketOutputListener;
import net.wurstclient.other_feature.OtherFeature;
import net.wurstclient.settings.CheckboxSetting;

@DontBlock
@SearchTags({"vanilla spoof", "AntiFabric", "anti fabric", "LibHatesMods",
	"HackedServer"})
public final class VanillaSpoofOtf extends OtherFeature
	implements ConnectionPacketOutputListener
{
	private final CheckboxSetting spoof =
		new CheckboxSetting("Spoof Vanilla", false);
	
	public VanillaSpoofOtf()
	{
		super("VanillaSpoof",
			"Bypasses anti-Fabric plugins by pretending to be a vanilla client.");
		addSetting(spoof);
		
		EVENTS.add(ConnectionPacketOutputListener.class, this);
	}
	
	@Override
	public void onSentConnectionPacket(ConnectionPacketOutputEvent event)
	{
		if(!spoof.isChecked())
			return;
		
		if(!(event.getPacket() instanceof CustomPayloadC2SPacket packet))
			return;
		
		// change client brand "fabric" back to "vanilla"
		if(packet.payload() instanceof BrandCustomPayload)
			event.setPacket(
				new CustomPayloadC2SPacket(new BrandCustomPayload("vanilla")));
			
		// cancel Fabric's "c:version", "c:register" and
		// "fabric:custom_ingredient_sync" packets
		// TODO: Something else is needed to prevent the connection from
		// hanging when these packets are cancelled.
		
		// Identifier channel = packet.payload().getId().id();
		// if(channel.getNamespace().equals("fabric")
		// || channel.getNamespace().equals("c"))
		// event.cancel();
	}
	
	@Override
	public boolean isEnabled()
	{
		return spoof.isChecked();
	}
	
	@Override
	public String getPrimaryAction()
	{
		return isEnabled() ? "Disable" : "Enable";
	}
	
	@Override
	public void doPrimaryAction()
	{
		spoof.setChecked(!spoof.isChecked());
	}
}
