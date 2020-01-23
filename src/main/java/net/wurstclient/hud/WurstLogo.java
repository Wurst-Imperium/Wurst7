/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hud;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.util.Identifier;
import net.wurstclient.WurstClient;

public final class WurstLogo
{
	private static final Identifier texture =
		new Identifier("wurst", "wurst_128.png");
	
	public void render()
	{
		if(!WurstClient.INSTANCE.getOtfs().wurstLogoOtf.isVisible())
			return;
		
		String version = getVersionString();
		TextRenderer tr = WurstClient.MC.textRenderer;
		
		// draw version background
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		
		if(WurstClient.INSTANCE.getHax().rainbowUiHack.isEnabled())
		{
			float[] acColor = WurstClient.INSTANCE.getGui().getAcColor();
			GL11.glColor4f(acColor[0], acColor[1], acColor[2], 0.5F);
			
		}else
			GL11.glColor4f(1, 1, 1, 0.5F);
		
		drawQuads(0, 6, tr.getStringWidth(version) + 76, 17);
		
		// draw version string
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		tr.draw(version, 74, 8, 0xFF000000);
		
		// draw Wurst logo
		GL11.glColor4f(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_BLEND);
		WurstClient.MC.getTextureManager().bindTexture(texture);
		DrawableHelper.blit(0, 3, 0, 0, 72, 18, 72, 18);
	}
	
	private String getVersionString()
	{
		String version = "v" + WurstClient.VERSION;
		version += " MC" + WurstClient.MC_VERSION;
		
		if(WurstClient.INSTANCE.getUpdater().isOutdated())
			version += " (outdated)";
		
		return version;
	}
	
	private void drawQuads(int x1, int y1, int x2, int y2)
	{
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glVertex2i(x1, y1);
		GL11.glVertex2i(x2, y1);
		GL11.glVertex2i(x2, y2);
		GL11.glVertex2i(x1, y2);
		GL11.glEnd();
	}
}
