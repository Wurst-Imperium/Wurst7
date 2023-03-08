/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.other_features;

import net.fabricmc.fabric.api.client.networking.v1.ClientLoginConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.wurstclient.DontBlock;
import net.wurstclient.SearchTags;
import net.wurstclient.WurstClient;
import net.wurstclient.nochatreports.NoChatReportsChannelHandler;
import net.wurstclient.other_feature.OtherFeature;
import net.wurstclient.settings.CheckboxSetting;

@DontBlock
@SearchTags({"no chat reports", "NoEncryption", "no encryption",
	"NoChatSigning", "no chat signing"})
public final class NoChatReportsOtf extends OtherFeature
{
	private final CheckboxSetting disableSignatures =
		new CheckboxSetting("Disable signatures", true);
	
	public NoChatReportsOtf()
	{
		super("NoChatReports", "description.wurst.other_feature.nochatreports");
		addSetting(disableSignatures);
		
		ClientLoginConnectionEvents.INIT.register(this::onLoginStart);
		ClientPlayConnectionEvents.DISCONNECT.register(this::onPlayDisconnect);
	}
	
	private void onLoginStart(ClientLoginNetworkHandler handler,
		MinecraftClient client)
	{
		if(isActive() && !WURST.getOtfs().vanillaSpoofOtf.isEnabled())
			ClientPlayNetworking.registerGlobalReceiver(
				NoChatReportsChannelHandler.CHANNEL,
				NoChatReportsChannelHandler.INSTANCE);
		else
			ClientPlayNetworking
				.unregisterGlobalReceiver(NoChatReportsChannelHandler.CHANNEL);
	}
	
	private void onPlayDisconnect(ClientPlayNetworkHandler handler,
		MinecraftClient client)
	{
		ClientPlayNetworking
			.unregisterGlobalReceiver(NoChatReportsChannelHandler.CHANNEL);
	}
	
	@Override
	public boolean isEnabled()
	{
		return disableSignatures.isChecked();
	}
	
	public boolean isActive()
	{
		return isEnabled() && WurstClient.INSTANCE.isEnabled();
	}
	
	@Override
	public String getPrimaryAction()
	{
		return WURST.translate("button.wurst.nochatreports."
			+ (isEnabled() ? "re-enable_signatures" : "disable_signatures"));
	}
	
	@Override
	public void doPrimaryAction()
	{
		disableSignatures.setChecked(!disableSignatures.isChecked());
	}
	
	// See ClientPlayerEntityMixin, ProfileKeysMixin
}
