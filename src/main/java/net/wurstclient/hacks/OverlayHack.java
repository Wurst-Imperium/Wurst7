/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import org.lwjgl.opengl.GL11;

import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.wurstclient.Category;
import net.wurstclient.events.RenderListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.util.RenderUtils;

public final class OverlayHack extends Hack implements RenderListener
{
	public OverlayHack()
	{
		super("Overlay",
			"Renders the Nuker animation whenever you mine a block.");
		setCategory(Category.RENDER);
	}
	
	@Override
	public void onEnable()
	{
		EVENTS.add(RenderListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(RenderListener.class, this);
	}
	
	@Override
	public void onRender(float partialTicks)
	{
		if(MC.crosshairTarget == null
			|| MC.crosshairTarget.getType() != HitResult.Type.BLOCK)
			return;
		
		BlockHitResult blockHitResult = (BlockHitResult)MC.crosshairTarget;
		BlockPos pos = new BlockPos(blockHitResult.getBlockPos());
		
		// check if hitting block
		if(!MC.interactionManager.isBreakingBlock())
			return;
		
		// GL settings
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		GL11.glLineWidth(2);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		
		GL11.glPushMatrix();
		RenderUtils.applyRenderOffset();
		
		// set position
		GL11.glTranslated(pos.getX(), pos.getY(), pos.getZ());
		
		// get progress
		float progress =
			IMC.getInteractionManager().getCurrentBreakingProgress();
		
		// set size
		GL11.glTranslated(0.5, 0.5, 0.5);
		GL11.glScaled(progress, progress, progress);
		GL11.glTranslated(-0.5, -0.5, -0.5);
		
		// get color
		float red = progress * 2F;
		float green = 2 - red;
		
		// draw box
		GL11.glColor4f(red, green, 0, 0.25F);
		RenderUtils.drawSolidBox();
		GL11.glColor4f(red, green, 0, 0.5F);
		RenderUtils.drawOutlinedBox();
		
		GL11.glPopMatrix();
		
		// GL resets
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_LINE_SMOOTH);
	}
}
