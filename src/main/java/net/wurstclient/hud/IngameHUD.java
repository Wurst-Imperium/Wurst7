/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hud;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.DrawContext;
import net.wurstclient.WurstClient;
import net.wurstclient.clickgui.ClickGui;
import net.wurstclient.clickgui.screens.ClickGuiScreen;
import net.wurstclient.events.GUIRenderListener;

public final class IngameHUD implements GUIRenderListener
{
	private final WurstLogo wurstLogo = new WurstLogo();
	private final HackListHUD hackList = new HackListHUD();
	private TabGui tabGui;
	
	@Override
	public void onRenderGUI(DrawContext context, float partialTicks)
	{
		if(!WurstClient.INSTANCE.isEnabled())
			return;
		
		if(tabGui == null)
			tabGui = new TabGui();
		
		boolean blend = GL11.glGetBoolean(GL11.GL_BLEND);
		ClickGui clickGui = WurstClient.INSTANCE.getGui();
		
		// GL settings
		GL11.glDisable(GL11.GL_CULL_FACE);
		
		clickGui.updateColors();
		
		wurstLogo.render(context);
		hackList.render(context, partialTicks);
		tabGui.render(context, partialTicks);
		
		// pinned windows
		if(!(WurstClient.MC.currentScreen instanceof ClickGuiScreen))
			clickGui.renderPinnedWindows(context, partialTicks);
		
		// GL resets
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		RenderSystem.setShaderColor(1, 1, 1, 1);
		
		if(blend)
			GL11.glEnable(GL11.GL_BLEND);
		else
			GL11.glDisable(GL11.GL_BLEND);
	}
	
	public HackListHUD getHackList()
	{
		return hackList;
	}
}
