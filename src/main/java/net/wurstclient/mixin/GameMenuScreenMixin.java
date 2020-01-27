/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import java.util.Iterator;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.wurstclient.WurstClient;
import net.wurstclient.options.WurstOptionsScreen;

@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin extends Screen
{
	private static final Identifier wurstTexture =
		new Identifier("wurst", "wurst_128.png");
	
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
		removeFeedbackAndBugReportButtons();
	}
	
	private void addWurstOptionsButton()
	{
		addButton(new ButtonWidget(width / 2 - 102, height / 4 + 56, 204, 20,
			"            Options",
			b -> minecraft.openScreen(new WurstOptionsScreen(this))));
	}
	
	private void removeFeedbackAndBugReportButtons()
	{
		for(Iterator<AbstractButtonWidget> itr = buttons.iterator(); itr
			.hasNext();)
		{
			AbstractButtonWidget button = itr.next();
			
			if(isFeedbackOrBugReportButton(button))
				itr.remove();
		}
		
		for(Iterator<Element> itr = children.iterator(); itr.hasNext();)
		{
			Element element = itr.next();
			
			if(isFeedbackOrBugReportButton(element))
				itr.remove();
		}
	}
	
	private boolean isFeedbackOrBugReportButton(Element element)
	{
		if(!(element instanceof AbstractButtonWidget))
			return false;
		
		AbstractButtonWidget button = (AbstractButtonWidget)element;
		
		if(button.y != height / 4 + 72 + -16)
			return false;
		
		if(button.x != width / 2 - 102 && button.x != width / 2 + 4)
			return false;
		
		if(button.getWidth() != 98)
			return false;
		
		return true;
	}
	
	@Inject(at = {@At("TAIL")}, method = {"render(IIF)V"})
	private void onRender(int mouseX, int mouseY, float partialTicks,
		CallbackInfo ci)
	{
		if(!WurstClient.INSTANCE.isEnabled())
			return;
		
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glDepthMask(false);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glColor4f(1, 1, 1, 1);
		
		minecraft.getTextureManager().bindTexture(wurstTexture);
		
		int x = width / 2 - 68;
		int y = height / 4 + 73 - 15;
		int w = 63;
		int h = 16;
		int fw = 63;
		int fh = 16;
		float u = 0;
		float v = 0;
		blit(x, y, u, v, w, h, fw, fh);
	}
}
