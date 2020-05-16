/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import net.minecraft.client.gui.screen.*;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.TranslatableText;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.wurstclient.WurstClient;
import net.wurstclient.options.WurstOptionsScreen;

@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin extends Screen
{
	private static final Identifier wurstTexture =
		new Identifier("wurst", "wurst_128.png");
	
	private ButtonWidget wurstOptionsButton;
	private ButtonWidget wurstConnectButton;

	private ServerInfo serverEntry = new ServerInfo("", "", false);
	
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
		addWurstConnectButton();
		removeFeedbackAndBugReportButtons();
	}
	
	private void addWurstOptionsButton()
	{
		wurstOptionsButton = new ButtonWidget(width / 2 - 102, height / 4 + 56,
			98, 20, "          Options", b -> openWurstOptions());
		
		addButton(wurstOptionsButton);
	}

	private void addWurstConnectButton()
	{
		wurstConnectButton = new ButtonWidget(width/2 + 4, height/4 + 56,
				98, 20, "Direct Connect",
				b -> WurstClient.MC.openScreen(new DirectConnectScreen(this, this::directConnect, serverEntry)));
		addButton(wurstConnectButton);
	}
	
	private void openWurstOptions()
	{
		minecraft.openScreen(new WurstOptionsScreen(this));
	}
	
	private void removeFeedbackAndBugReportButtons()
	{
		buttons.removeIf(this::isFeedbackOrBugReportButton);
		children.removeIf(this::isFeedbackOrBugReportButton);
	}
	
	private boolean isFeedbackOrBugReportButton(Element element)
	{
		if(element == null || !(element instanceof AbstractButtonWidget))
			return false;
		
		AbstractButtonWidget button = (AbstractButtonWidget)element;
		String message = button.getMessage();
		
		return message != null
			&& (message.equals(I18n.translate("menu.sendFeedback"))
				|| message.equals(I18n.translate("menu.reportBugs")));
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
		
		int x = wurstOptionsButton.x + 2;
		int y = wurstOptionsButton.y + 3;
		int w = 46;
		int h = 14;
		int fw = 46;
		int fh = 14;
		float u = 0;
		float v = 0;
		blit(x, y, u, v, w, h, fw, fh);
	}

	private void directConnect(boolean confirmedAction)
	{
		if(confirmedAction)
			connect(serverEntry);
		else
			WurstClient.MC.openScreen(this);
	}

	private void connect(ServerInfo entry)
	{
		WurstClient.MC.world.disconnect();
		if(WurstClient.MC.isInSingleplayer())
			WurstClient.MC.disconnect(new SaveLevelScreen(new TranslatableText("menu.savingLevel")));
		WurstClient.MC.openScreen(new ConnectScreen(this, WurstClient.MC, entry));
	}
}
