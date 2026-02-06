/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.wurstclient.WurstClient;
import net.wurstclient.options.WurstOptionsScreen;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen
{
	@Unique
	private Button wurstOptionsButton;
	
	private PauseScreenMixin(WurstClient wurst, Component title)
	{
		super(title);
	}
	
	@Inject(at = @At("TAIL"), method = "createPauseMenu()V")
	private void onInitWidgets(CallbackInfo ci)
	{
		if(WurstClient.INSTANCE.getOtfs().wurstOptionsOtf.isVisibleInGameMenu())
			addWurstOptionsButton();
	}
	
	@Inject(at = @At("TAIL"),
		method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V")
	private void onRender(GuiGraphics context, int mouseX, int mouseY,
		float partialTicks, CallbackInfo ci)
	{
		WurstClient wurst = WurstClient.INSTANCE;
		if(!wurst.isEnabled())
			return;
		
		wurst.getOtfs().wurstOptionsOtf.drawWurstLogoOnButton(context,
			wurstOptionsButton);
	}
	
	@Unique
	private void addWurstOptionsButton()
	{
		List<AbstractWidget> buttons = Screens.getWidgets(this);
		
		// Fallback position
		int buttonX = width / 2 - 102;
		int buttonY = 60;
		int buttonWidth = 204;
		int buttonHeight = 20;
		
		for(AbstractWidget button : buttons)
		{
			// If feedback button exists, use its position
			if(isTrKey(button, "menu.sendFeedback")
				|| isTrKey(button, "menu.feedback"))
			{
				buttonY = button.getY();
				break;
			}
			
			// If options button exists, go 24px above it
			if(isTrKey(button, "menu.options"))
			{
				buttonY = button.getY() - 24;
				break;
			}
		}
		
		// Clear required space for Wurst Options
		hideFeedbackReportAndServerLinksButtons();
		ensureSpaceAvailable(buttonX, buttonY, buttonWidth, buttonHeight);
		
		// Create Wurst Options button
		wurstOptionsButton = WurstClient.INSTANCE.getOtfs().wurstOptionsOtf
			.buttonBuilder(this::openWurstOptions)
			.bounds(buttonX, buttonY, buttonWidth, buttonHeight).build();
		buttons.add(wurstOptionsButton);
	}
	
	@Unique
	private void hideFeedbackReportAndServerLinksButtons()
	{
		for(AbstractWidget button : Screens.getWidgets(this))
			if(isTrKey(button, "menu.sendFeedback")
				|| isTrKey(button, "menu.reportBugs")
				|| isTrKey(button, "menu.feedback")
				|| isTrKey(button, "menu.server_links"))
				button.visible = false;
	}
	
	@Unique
	private void ensureSpaceAvailable(int x, int y, int width, int height)
	{
		// Check if there are any buttons in the way
		ArrayList<AbstractWidget> buttonsInTheWay = new ArrayList<>();
		for(AbstractWidget button : Screens.getWidgets(this))
		{
			if(button.getRight() < x || button.getX() > x + width
				|| button.getBottom() < y || button.getY() > y + height)
				continue;
			
			if(!button.visible)
				continue;
			
			buttonsInTheWay.add(button);
		}
		
		// If not, we're done
		if(buttonsInTheWay.isEmpty())
			return;
		
		// If yes, clear space below and move the buttons there
		ensureSpaceAvailable(x, y + 24, width, height);
		for(AbstractWidget button : buttonsInTheWay)
			button.setY(button.getY() + 24);
	}
	
	@Unique
	private void openWurstOptions(Button button)
	{
		minecraft.setScreen(new WurstOptionsScreen(this));
	}
	
	@Unique
	private boolean isTrKey(AbstractWidget button, String key)
	{
		String message = button.getMessage().getString();
		return message != null && message.equals(I18n.get(key));
	}
}
