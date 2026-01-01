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
import net.minecraft.util.CommonColors;
import net.minecraft.util.StringUtil;
import net.wurstclient.WurstClient;
import net.wurstclient.other_feature.OtfList;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.LastServerRememberer;

public final class NcrModRequiredScreen extends Screen
{
	private static final List<String> DISCONNECT_REASONS = Arrays.asList(
		// Older versions of NCR have a bug that sends the raw translation key.
		"disconnect.nochatreports.server",
		"You do not have No Chat Reports, and this server is configured to require it on client!");
	
	private final Screen prevScreen;
	private final Component reason;
	private MultiLineLabel reasonFormatted = MultiLineLabel.EMPTY;
	private int reasonHeight;
	
	private Button signatureButton;
	private final Supplier<String> sigButtonMsg;
	
	private Button vsButton;
	private final Supplier<String> vsButtonMsg;
	
	public NcrModRequiredScreen(Screen prevScreen)
	{
		super(Component.literal(ChatUtils.WURST_PREFIX + WurstClient.INSTANCE
			.translate("gui.wurst.nochatreports.ncr_mod_server.title")));
		this.prevScreen = prevScreen;
		
		reason = Component.literal(WurstClient.INSTANCE
			.translate("gui.wurst.nochatreports.ncr_mod_server.message"));
		
		OtfList otfs = WurstClient.INSTANCE.getOtfs();
		
		sigButtonMsg = () -> WurstClient.INSTANCE
			.translate("button.wurst.nochatreports.signatures_status")
			+ blockedOrAllowed(otfs.noChatReportsOtf.isEnabled());
		
		vsButtonMsg =
			() -> "VanillaSpoof: " + onOrOff(otfs.vanillaSpoofOtf.isEnabled());
	}
	
	private String onOrOff(boolean on)
	{
		return WurstClient.INSTANCE.translate("options." + (on ? "on" : "off"))
			.toUpperCase();
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
			.bounds(buttonX - 48, signaturesY, 148, 20).build());
		
		addRenderableWidget(vsButton = Button
			.builder(Component.literal(vsButtonMsg.get()),
				b -> toggleVanillaSpoof())
			.bounds(buttonX + 102, signaturesY, 148, 20).build());
		
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
	
	private void toggleVanillaSpoof()
	{
		WurstClient.INSTANCE.getOtfs().vanillaSpoofOtf.doPrimaryAction();
		vsButton.setMessage(Component.literal(vsButtonMsg.get()));
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
	
	public static boolean isCausedByLackOfNCR(Component disconnectReason)
	{
		OtfList otfs = WurstClient.INSTANCE.getOtfs();
		if(otfs.noChatReportsOtf.isActive()
			&& !otfs.vanillaSpoofOtf.isEnabled())
			return false;
		
		String text = disconnectReason.getString();
		if(text == null)
			return false;
		
		text = StringUtil.stripColor(text);
		return DISCONNECT_REASONS.contains(text);
	}
}
