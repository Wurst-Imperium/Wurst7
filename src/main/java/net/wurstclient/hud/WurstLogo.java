/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
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
import net.wurstclient.other_features.WurstLogoOtf;

public final class WurstLogo
{
	private static final Identifier texture =
		new Identifier("wurst", "wurst_128.png");
	
	public void render(MatrixStack matrixStack)
	{
		WurstLogoOtf otf = WurstClient.INSTANCE.getOtfs().wurstLogoOtf;
		if(!otf.isVisible())
			return;
		
		String version = getVersionString();
		TextRenderer tr = WurstClient.MC.textRenderer;
		
		// draw version background
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		
		float[] color;
		if(WurstClient.INSTANCE.getHax().rainbowUiHack.isEnabled())
			color = WurstClient.INSTANCE.getGui().getAcColor();
		else
			color = otf.getBackgroundColor();
		
		drawQuads(0, 6, tr.getWidth(version) + 76, 17, color[0], color[1],
			color[2], 0.5F);
		
		// draw version string
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		tr.draw(matrixStack, version, 74, 8, otf.getTextColor());
		
		// draw Wurst logo
		GL11.glColor4f(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_BLEND);
		WurstClient.MC.getTextureManager().bindTexture(texture);
		DrawableHelper.drawTexture(matrixStack, 0, 3, 0, 0, 72, 18, 72, 18);
	}
	
	private String getVersionString()
	{
		String version = "v" + WurstClient.VERSION;
		version += " MC" + WurstClient.MC_VERSION;
		
		if(WurstClient.INSTANCE.getUpdater().isOutdated())
			version += " (outdated)";
		
		return version;
	}
	
	private void drawQuads(int x1, int y1, int x2, int y2, float r, float g,
		float b, float a)
	{
		GL11.glColor4f(r, g, b, a);
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glVertex2i(x1, y1);
		GL11.glVertex2i(x2, y1);
		GL11.glVertex2i(x2, y2);
		GL11.glVertex2i(x1, y2);
		GL11.glEnd();
	}
}
