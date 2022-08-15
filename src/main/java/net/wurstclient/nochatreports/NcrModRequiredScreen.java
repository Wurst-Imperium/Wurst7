/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.nochatreports;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.client.font.MultilineText;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.StringHelper;
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
	private final Text reason;
	private MultilineText reasonFormatted = MultilineText.EMPTY;
	private int reasonHeight;
	
	private ButtonWidget signatureButton;
	private final Supplier<String> sigButtonMsg;
	
	private ButtonWidget vsButton;
	private final Supplier<String> vsButtonMsg;
	
	public NcrModRequiredScreen(Screen prevScreen)
	{
		super(Text.literal(ChatUtils.WURST_PREFIX).append(
			Text.translatable("gui.wurst.nochatreports.ncr_mod_server.title")));
		this.prevScreen = prevScreen;
		
		reason =
			Text.translatable("gui.wurst.nochatreports.ncr_mod_server.message");
		
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
		reasonFormatted =
			MultilineText.create(textRenderer, reason, width - 50);
		reasonHeight = reasonFormatted.count() * textRenderer.fontHeight;
		
		int buttonX = width / 2 - 100;
		int belowReasonY =
			(height - 78) / 2 + reasonHeight / 2 + textRenderer.fontHeight * 2;
		int signaturesY = Math.min(belowReasonY, height - 68);
		int reconnectY = signaturesY + 24;
		int backButtonY = reconnectY + 24;
		
		addDrawableChild(
			signatureButton = new ButtonWidget(buttonX - 48, signaturesY, 148,
				20, Text.literal(sigButtonMsg.get()), b -> toggleSignatures()));
		
		addDrawableChild(
			vsButton = new ButtonWidget(buttonX + 102, signaturesY, 148, 20,
				Text.literal(vsButtonMsg.get()), b -> toggleVanillaSpoof()));
		
		addDrawableChild(new ButtonWidget(buttonX, reconnectY, 200, 20,
			Text.literal("Reconnect"),
			b -> LastServerRememberer.reconnect(prevScreen)));
		
		addDrawableChild(new ButtonWidget(buttonX, backButtonY, 200, 20,
			Text.translatable("gui.toMenu"),
			b -> client.setScreen(prevScreen)));
	}
	
	private void toggleSignatures()
	{
		WurstClient.INSTANCE.getOtfs().noChatReportsOtf.doPrimaryAction();
		signatureButton.setMessage(Text.literal(sigButtonMsg.get()));
	}
	
	private void toggleVanillaSpoof()
	{
		WurstClient.INSTANCE.getOtfs().vanillaSpoofOtf.doPrimaryAction();
		vsButton.setMessage(Text.literal(vsButtonMsg.get()));
	}
	
	@Override
	public void render(MatrixStack matrices, int mouseX, int mouseY,
		float delta)
	{
		renderBackground(matrices);
		
		int centerX = width / 2;
		int reasonY = (height - 68) / 2 - reasonHeight / 2;
		int titleY = reasonY - textRenderer.fontHeight * 2;
		
		DrawableHelper.drawCenteredText(matrices, textRenderer, title, centerX,
			titleY, 0xAAAAAA);
		reasonFormatted.drawCenterWithShadow(matrices, centerX, reasonY);
		
		super.render(matrices, mouseX, mouseY, delta);
	}
	
	@Override
	public boolean shouldCloseOnEsc()
	{
		return false;
	}
	
	public static boolean isCausedByLackOfNCR(Text disconnectReason)
	{
		OtfList otfs = WurstClient.INSTANCE.getOtfs();
		if(otfs.noChatReportsOtf.isActive()
			&& !otfs.vanillaSpoofOtf.isEnabled())
			return false;
		
		String text = disconnectReason.getString();
		if(text == null)
			return false;
		
		text = StringHelper.stripTextFormat(text);
		return DISCONNECT_REASONS.contains(text);
	}
}
