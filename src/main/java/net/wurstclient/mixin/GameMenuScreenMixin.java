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
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.wurstclient.WurstClient;
import net.wurstclient.options.WurstOptionsScreen;

@Mixin(PauseScreen.class)
public abstract class GameMenuScreenMixin extends Screen
{
	@Unique
	private static final Identifier WURST_TEXTURE =
		Identifier.fromNamespaceAndPath("wurst", "wurst_128.png");
	
	@Unique
	private Button wurstOptionsButton;
	
	private GameMenuScreenMixin(WurstClient wurst, Component title)
	{
		super(title);
	}
	
	@Inject(at = @At("TAIL"), method = "createPauseMenu()V")
	private void onInitWidgets(CallbackInfo ci)
	{
		if(!WurstClient.INSTANCE.isEnabled())
			return;
		
		addWurstOptionsButton();
	}
	
	@Inject(at = @At("TAIL"),
		method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V")
	private void onRender(GuiGraphics context, int mouseX, int mouseY,
		float partialTicks, CallbackInfo ci)
	{
		if(!WurstClient.INSTANCE.isEnabled() || wurstOptionsButton == null)
			return;
		
		int x = wurstOptionsButton.getX() + 34;
		int y = wurstOptionsButton.getY() + 2;
		int w = 63;
		int h = 16;
		int fw = 63;
		int fh = 16;
		float u = 0;
		float v = 0;
		context.guiRenderState.up();
		context.blit(RenderPipelines.GUI_TEXTURED, WURST_TEXTURE, x, y, u, v, w,
			h, fw, fh);
	}
	
	@Unique
	private void addWurstOptionsButton()
	{
		List<AbstractWidget> buttons = Screens.getButtons(this);
		
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
		MutableComponent buttonText = Component.literal("            Options");
		wurstOptionsButton = Button.builder(buttonText, b -> openWurstOptions())
			.bounds(buttonX, buttonY, buttonWidth, buttonHeight).build();
		buttons.add(wurstOptionsButton);
	}
	
	@Unique
	private void hideFeedbackReportAndServerLinksButtons()
	{
		for(AbstractWidget button : Screens.getButtons(this))
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
		for(AbstractWidget button : Screens.getButtons(this))
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
	private void openWurstOptions()
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
