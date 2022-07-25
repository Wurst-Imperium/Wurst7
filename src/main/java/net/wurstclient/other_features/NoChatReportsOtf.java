/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
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
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.client.gui.hud.MessageIndicator.Icon;
import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import net.wurstclient.DontBlock;
import net.wurstclient.SearchTags;
import net.wurstclient.WurstClient;
import net.wurstclient.nochatreports.NoChatReportsChannelHandler;
import net.wurstclient.other_feature.OtherFeature;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.util.ChatUtils;

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
	
	public MessageIndicator modifyIndicator(Text message,
		MessageSignatureData signature, MessageIndicator indicator)
	{
		if(indicator != null || signature == null || signature.isEmpty())
			return indicator;
		
		return new MessageIndicator(0xE84F58, Icon.CHAT_NOT_SECURE,
			Text.literal(ChatUtils.WURST_PREFIX + "\u00a7cReportable\u00a7r - ")
				.append(Text.translatable(
					"description.wurst.nochatreports.message_is_reportable")),
			"Reportable");
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
	
	// See ChatHudMixin, ClientPlayerEntityMixin, MessageHandlerMixin,
	// ProfileKeysMixin
}
