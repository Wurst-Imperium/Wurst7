/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.nochatreports;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.PlainTextContents.LiteralContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.CommonColors;
import net.wurstclient.WurstClient;
import net.wurstclient.other_features.NoChatReportsOtf;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.LastServerRememberer;

public final class ForcedChatReportsScreen extends Screen
{
	private static final List<String> TRANSLATABLE_DISCONNECT_REASONS =
		Arrays.asList("multiplayer.disconnect.missing_public_key",
			"multiplayer.disconnect.invalid_public_key_signature",
			"multiplayer.disconnect.invalid_public_key",
			"multiplayer.disconnect.unsigned_chat");
	
	private static final List<String> LITERAL_DISCONNECT_REASONS =
		Arrays.asList("An internal error occurred in your connection.",
			"A secure profile is required to join this server.",
			"Secure profile expired.", "Secure profile invalid.");
	
	private final Screen prevScreen;
	private final Component reason;
	private MultiLineLabel reasonFormatted = MultiLineLabel.EMPTY;
	private int reasonHeight;
	
	private Button signatureButton;
	private final Supplier<String> sigButtonMsg;
	
	public ForcedChatReportsScreen(Screen prevScreen)
	{
		super(Component.literal(ChatUtils.WURST_PREFIX)
			.append(Component.literal(WurstClient.INSTANCE
				.translate("gui.wurst.nochatreports.unsafe_server.title"))));
		this.prevScreen = prevScreen;
		
		reason = Component.literal(WurstClient.INSTANCE
			.translate("gui.wurst.nochatreports.unsafe_server.message"));
		
		NoChatReportsOtf ncr = WurstClient.INSTANCE.getOtfs().noChatReportsOtf;
		sigButtonMsg = () -> WurstClient.INSTANCE
			.translate("button.wurst.nochatreports.signatures_status")
			+ blockedOrAllowed(ncr.isEnabled());
	}
	
	private String blockedOrAllowed(boolean blocked)
	{
		return WurstClient.INSTANCE.translate(
			"gui.wurst.generic.allcaps_" + (blocked ? "blocked" : "allowed"));
	}
	
	@Override
	protected void init()
	{
		reasonFormatted = MultiLineLabel.create(font, reason, width - 50);
		reasonHeight = reasonFormatted.getLineCount() * font.lineHeight;
		
		int buttonX = width / 2 - 100;
		int belowReasonY =
			(height - 78) / 2 + reasonHeight / 2 + font.lineHeight * 2;
		int signaturesY = Math.min(belowReasonY, height - 68);
		int reconnectY = signaturesY + 24;
		int backButtonY = reconnectY + 24;
		
		addRenderableWidget(signatureButton = Button
			.builder(Component.literal(sigButtonMsg.get()),
				b -> toggleSignatures())
			.bounds(buttonX, signaturesY, 200, 20).build());
		
		addRenderableWidget(Button
			.builder(Component.literal("Reconnect"),
				b -> LastServerRememberer.reconnect(prevScreen))
			.bounds(buttonX, reconnectY, 200, 20).build());
		
		addRenderableWidget(Button
			.builder(Component.translatable("gui.toMenu"),
				b -> minecraft.setScreen(prevScreen))
			.bounds(buttonX, backButtonY, 200, 20).build());
	}
	
	private void toggleSignatures()
	{
		WurstClient.INSTANCE.getOtfs().noChatReportsOtf.doPrimaryAction();
		signatureButton.setMessage(Component.literal(sigButtonMsg.get()));
	}
	
	@Override
	public void render(GuiGraphics context, int mouseX, int mouseY,
		float partialTicks)
	{
		int centerX = width / 2;
		int reasonY = (height - 68) / 2 - reasonHeight / 2;
		int titleY = reasonY - font.lineHeight * 2;
		
		context.drawCenteredString(font, title, centerX, titleY,
			CommonColors.LIGHT_GRAY);
		ActiveTextCollector otherContext = context.textRenderer();
		reasonFormatted.visitLines(TextAlignment.CENTER, centerX, reasonY, 9,
			otherContext);
		
		for(Renderable drawable : renderables)
			drawable.render(context, mouseX, mouseY, partialTicks);
	}
	
	@Override
	public boolean shouldCloseOnEsc()
	{
		return false;
	}
	
	public static boolean isCausedByNoChatReports(Component disconnectReason)
	{
		if(!WurstClient.INSTANCE.getOtfs().noChatReportsOtf.isActive())
			return false;
		
		if(disconnectReason.getContents() instanceof TranslatableContents tr
			&& TRANSLATABLE_DISCONNECT_REASONS.contains(tr.getKey()))
			return true;
		
		if(disconnectReason.getContents() instanceof LiteralContents lt
			&& LITERAL_DISCONNECT_REASONS.contains(lt.text()))
			return true;
		
		return false;
	}
}
