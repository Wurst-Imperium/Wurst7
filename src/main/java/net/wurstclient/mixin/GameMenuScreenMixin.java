/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import java.util.List;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.systems.RenderSystem;

import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.wurstclient.WurstClient;
import net.wurstclient.options.WurstOptionsScreen;

@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin extends Screen
{
	private static final Identifier wurstTexture =
		new Identifier("wurst", "wurst_128.png");
	
	private ButtonWidget wurstOptionsButton;
	
	private GameMenuScreenMixin(WurstClient wurst, Text text_1)
	{
		super(text_1);
	}
	
	@Inject(at = {@At("TAIL")}, method = {"initWidgets()V"})
	private void onInitWidgets(CallbackInfo ci)
	{
		if(!WurstClient.INSTANCE.isEnabled())
			return;
		
		addWurstOptionsButton();
	}
	
	private void addWurstOptionsButton()
	{
		List<ClickableWidget> buttons = Screens.getButtons(this);
		
		int buttonY = -1;
		int buttonI = -1;
		
		for(int i = 0; i < buttons.size(); ++i)
		{
			ClickableWidget button = buttons.get(i);
			
			// insert Wurst button in place of feedback/report row
			if(isFeedbackButton(button))
			{
				buttonY = button.y;
				buttonI = i;
			}
			
			// make feedback/report buttons invisible
			// (removing them completely would break ModMenu)
			if(isFeedbackButton(button) || isBugReportButton(button))
				button.visible = false;
		}
		
		if(buttonY == -1 || buttonI == -1)
			throw new CrashException(
				CrashReport.create(new IllegalStateException(),
					"Someone deleted the Feedback button!"));
		
		wurstOptionsButton = new ButtonWidget(width / 2 - 102, buttonY, 204, 20,
			Text.literal("            Options"), b -> openWurstOptions());
		buttons.add(buttonI, wurstOptionsButton);
	}
	
	private boolean isFeedbackButton(ClickableWidget button)
	{
		return hasTrKey(button, "menu.sendFeedback");
	}
	
	private boolean isBugReportButton(ClickableWidget button)
	{
		return hasTrKey(button, "menu.reportBugs");
	}
	
	private boolean hasTrKey(ClickableWidget button, String key)
	{
		String message = button.getMessage().getString();
		return message != null && message.equals(I18n.translate(key));
	}
	
	private void openWurstOptions()
	{
		client.setScreen(new WurstOptionsScreen(this));
	}
	
	@Inject(at = {@At("TAIL")},
		method = {"render(Lnet/minecraft/client/util/math/MatrixStack;IIF)V"})
	private void onRender(MatrixStack matrixStack, int mouseX, int mouseY,
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
		
		RenderSystem.setShaderTexture(0, wurstTexture);
		
		int x = wurstOptionsButton.x + 34;
		int y = wurstOptionsButton.y + 2;
		int w = 63;
		int h = 16;
		int fw = 63;
		int fh = 16;
		float u = 0;
		float v = 0;
		drawTexture(matrixStack, x, y, u, v, w, h, fw, fh);
	}
}
