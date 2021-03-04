/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hud;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.wurstclient.WurstClient;

public final class WurstLogo
{
	private static final Identifier texture =
		new Identifier("wurst", "wurst_200.png");
	
	public void render(MatrixStack matrixStack)
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
		
		drawQuads(0, 15, tr.getWidth(version) + 50, 26);
		
		// draw version string
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		tr.draw(matrixStack, version, 45, 17, 0xFF000000);
		
		// draw Wurst logo
		GL11.glColor4f(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_BLEND);
		WurstClient.MC.getTextureManager().bindTexture(texture);
		DrawableHelper.drawTexture(matrixStack, 2, 0, 0, 0, 40, 40, 40, 40);
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
