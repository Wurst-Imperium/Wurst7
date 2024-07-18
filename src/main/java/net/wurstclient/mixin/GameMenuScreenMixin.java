/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import java.util.List;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.systems.RenderSystem;

import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.wurstclient.WurstClient;
import net.wurstclient.options.WurstOptionsScreen;

@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin extends Screen
{
	@Unique
	private static final Identifier WURST_TEXTURE =
		Identifier.of("wurst", "wurst_128.png");
	
	@Unique
	private ButtonWidget wurstOptionsButton;
	
	private GameMenuScreenMixin(WurstClient wurst, Text title)
	{
		super(title);
	}
	
	@Inject(at = @At("TAIL"), method = "initWidgets()V")
	private void onInitWidgets(CallbackInfo ci)
	{
		if(!WurstClient.INSTANCE.isEnabled())
			return;
		
		addWurstOptionsButton();
	}
	
	@Inject(at = @At("TAIL"),
		method = "render(Lnet/minecraft/client/gui/DrawContext;IIF)V")
	private void onRender(DrawContext context, int mouseX, int mouseY,
		float partialTicks, CallbackInfo ci)
	{
		if(!WurstClient.INSTANCE.isEnabled() || wurstOptionsButton == null)
			return;
		
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glDepthMask(false);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		RenderSystem.setShaderColor(1, 1, 1, 1);
		
		int x = wurstOptionsButton.getX() + 34;
		int y = wurstOptionsButton.getY() + 2;
		int w = 63;
		int h = 16;
		int fw = 63;
		int fh = 16;
		float u = 0;
		float v = 0;
		context.drawTexture(WURST_TEXTURE, x, y, u, v, w, h, fw, fh);
		
		GL11.glDisable(GL11.GL_BLEND);
	}
	
	@Unique
	private void addWurstOptionsButton()
	{
		List<ClickableWidget> buttons = Screens.getButtons(this);
		
		int buttonX = width / 2 - 102;
		int buttonY = -1;
		int buttonWidth = 204;
		int buttonHeight = 20;
		
		// Find the first row containing a feedback or ModMenu button
		for(ClickableWidget button : buttons)
		{
			if(!isFeedbackButton(button))
				continue;
			
			buttonY = button.getY();
			break;
		}
		
		// Crash if ModMenu can't behave itself again
		if(buttonY == -1)
			throw new CrashException(CrashReport.create(
				new IllegalStateException(
					"Someone deleted the Feedback button."),
				"I bet ModMenu is breaking stuff again!"));
			
		// Make any conflicting feedback/report/ModMenu buttons invisible
		// We don't remove them completely, because unlike ModMenu, we care
		// about compatibility with other mods here
		for(ClickableWidget button : buttons)
		{
			if(button.getRight() < buttonX
				|| button.getX() > buttonX + buttonWidth
				|| button.getBottom() < buttonY
				|| button.getY() > buttonY + buttonHeight)
				continue;
			
			button.visible = false;
		}
		
		// Add the Wurst Options button
		MutableText buttonText = Text.literal("            Options");
		wurstOptionsButton = ButtonWidget
			.builder(buttonText, b -> openWurstOptions())
			.dimensions(buttonX, buttonY, buttonWidth, buttonHeight).build();
		buttons.add(wurstOptionsButton);
	}
	
	@Unique
	private void openWurstOptions()
	{
		client.setScreen(new WurstOptionsScreen(this));
	}
	
	@Unique
	private boolean isFeedbackButton(ClickableWidget button)
	{
		if(FabricLoader.getInstance().isModLoaded("modmenu")
			&& containsTrKey(button, "modmenu.title"))
			return true;
		
		return isTrKey(button, "menu.sendFeedback")
			|| isTrKey(button, "menu.feedback");
	}
	
	@Unique
	private boolean isTrKey(ClickableWidget button, String key)
	{
		String message = button.getMessage().getString();
		return message != null && message.equals(I18n.translate(key));
	}
	
	@Unique
	private boolean containsTrKey(ClickableWidget button, String key)
	{
		String message = button.getMessage().getString();
		return message != null && message.contains(I18n.translate(key));
	}
}
