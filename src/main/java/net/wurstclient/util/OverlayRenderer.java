/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.wurstclient.WurstClient;
import net.wurstclient.mixinterface.IMinecraftClient;

public final class OverlayRenderer
{
	protected static final MinecraftClient MC = WurstClient.MC;
	protected static final IMinecraftClient IMC = WurstClient.IMC;
	
	private float progress;
	private float prevProgress;
	private BlockPos prevPos;
	
	public void resetProgress()
	{
		progress = 0;
		prevProgress = 0;
		prevPos = null;
	}
	
	public void updateProgress()
	{
		prevProgress = progress;
		progress = IMC.getInteractionManager().getCurrentBreakingProgress();
		
		if(progress < prevProgress)
			prevProgress = progress;
	}
	
	public void render(float partialTicks, BlockPos pos)
	{
		if(pos == null)
			return;
		
		// reset progress if breaking a different block
		if(prevPos != null && !pos.equals(prevPos))
			resetProgress();
		
		prevPos = pos;
		
		// GL settings
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		GL11.glLineWidth(2);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_LIGHTING);
		
		GL11.glPushMatrix();
		
		BlockPos camPos = RenderUtils.getCameraBlockPos();
		int regionX = (camPos.getX() >> 9) * 512;
		int regionZ = (camPos.getZ() >> 9) * 512;
		RenderUtils.applyRegionalRenderOffset(regionX, regionZ);
		
		// set position
		GL11.glTranslated(pos.getX() - regionX, pos.getY(),
			pos.getZ() - regionZ);
		
		// get interpolated progress
		boolean breaksInstantly = (MC.player.abilities.creativeMode
			|| BlockUtils.getHardness(pos) >= 1)
			&& BlockUtils.canBeClicked(pos);
		float p = breaksInstantly ? 1
			: MathHelper.lerp(partialTicks, prevProgress, progress);
		
		// set size
		if(p < 1)
		{
			GL11.glTranslated(0.5, 0.5, 0.5);
			GL11.glScaled(p, p, p);
			GL11.glTranslated(-0.5, -0.5, -0.5);
		}
		
		// get color
		float red = p * 2F;
		float green = 2 - red;
		
		// draw box
		GL11.glColor4f(red, green, 0, 0.25F);
		RenderUtils.drawSolidBox();
		GL11.glColor4f(red, green, 0, 0.5F);
		RenderUtils.drawOutlinedBox();
		
		GL11.glPopMatrix();
		
		// GL resets
		GL11.glColor4f(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_LINE_SMOOTH);
	}
}
