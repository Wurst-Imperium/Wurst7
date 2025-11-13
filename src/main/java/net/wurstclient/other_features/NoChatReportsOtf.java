/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.other_features;

import java.net.URI;

import net.fabricmc.fabric.api.client.networking.v1.ClientLoginConnectionEvents;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.GuiMessageTag.Icon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.LocalChatSession;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.SignedMessageChain;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.wurstclient.Category;
import net.wurstclient.DontBlock;
import net.wurstclient.SearchTags;
import net.wurstclient.events.ChatInputListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.other_feature.OtherFeature;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.util.ChatUtils;

@DontBlock
@SearchTags({"no chat reports", "NoEncryption", "no encryption",
	"NoChatSigning", "no chat signing"})
public final class NoChatReportsOtf extends OtherFeature
	implements UpdateListener, ChatInputListener
{
	private final CheckboxSetting disableSignatures =
		new CheckboxSetting("Disable signatures", true)
		{
			@Override
			public void update()
			{
				EVENTS.add(UpdateListener.class, NoChatReportsOtf.this);
			}
		};
	
	public NoChatReportsOtf()
	{
		super("NoChatReports", "description.wurst.other_feature.nochatreports");
		addSetting(disableSignatures);
		
		ClientLoginConnectionEvents.INIT.register(this::onLoginStart);
		EVENTS.add(ChatInputListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		ClientPacketListener netHandler = MC.getConnection();
		if(netHandler == null)
			return;
		
		if(isActive())
		{
			netHandler.chatSession = null;
			netHandler.signedMessageEncoder =
				SignedMessageChain.Encoder.UNSIGNED;
			
		}else if(netHandler.chatSession == null)
			MC.getProfileKeyPairManager().prepareKeyPair()
				.thenAcceptAsync(optional -> optional
					.ifPresent(profileKeys -> netHandler.chatSession =
						LocalChatSession.create(profileKeys)),
					MC);
		
		EVENTS.remove(UpdateListener.class, this);
	}
	
	@Override
	public void onReceivedMessage(ChatInputEvent event)
	{
		if(!isActive())
			return;
		
		Component originalText = event.getComponent();
		if(!(originalText
			.getContents() instanceof TranslatableContents trContent))
			return;
		
		if(!trContent.getKey().equals("chat.disabled.missingProfileKey"))
			return;
		
		event.cancel();
		
		ClickEvent clickEvent = new ClickEvent.OpenUrl(
			URI.create("https://www.wurstclient.net/chat-disabled-mpk/"));
		HoverEvent hoverEvent = new HoverEvent.ShowText(
			Component.literal("Original message: ").append(originalText));
		
		ChatUtils.component(Component.literal(
			"The server is refusing to let you chat without enabling chat reports. Click \u00a7nhere\u00a7r to learn more.")
			.withStyle(
				s -> s.withClickEvent(clickEvent).withHoverEvent(hoverEvent)));
	}
	
	private void onLoginStart(ClientHandshakePacketListenerImpl handler,
		Minecraft client)
	{
		EVENTS.add(UpdateListener.class, NoChatReportsOtf.this);
	}
	
	public GuiMessageTag modifyIndicator(Component message,
		MessageSignature signature, GuiMessageTag indicator)
	{
		if(!WURST.isEnabled() || MC.isLocalServer())
			return indicator;
		
		if(indicator != null || signature == null)
			return indicator;
		
		return new GuiMessageTag(0xE84F58, Icon.CHAT_MODIFIED,
			Component.literal(ChatUtils.WURST_PREFIX
				+ "\u00a7cReportable\u00a7r - "
				+ WURST.translate(
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
		return isEnabled() && WURST.isEnabled() && !MC.isLocalServer();
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
	
	@Override
	public Category getCategory()
	{
		return Category.CHAT;
	}
	
	// See ChatHudMixin, ClientPlayNetworkHandlerMixin.onOnServerMetadata(),
	// MinecraftClientMixin.onGetProfileKeys()
}
