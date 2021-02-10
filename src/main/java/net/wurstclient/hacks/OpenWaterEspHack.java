/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import org.lwjgl.opengl.GL11;

import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.RenderListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.mixinterface.IFishingBobberEntity;
import net.wurstclient.util.RenderUtils;

@SearchTags({"open water esp", "AutoFishESP", "auto fish esp"})
public final class OpenWaterEspHack extends Hack implements RenderListener
{
	private int openWaterBox;
	private int shallowWaterBox;
	
	public OpenWaterEspHack()
	{
		super("OpenWaterESP",
			"Shows whether or not you are fishing in 'open water' and\n"
				+ "draws a box around the area used for the open water\n"
				+ "calculation.");
		
		setCategory(Category.RENDER);
	}
	
	@Override
	public String getRenderName()
	{
		FishingBobberEntity bobber = MC.player.fishHook;
		
		if(bobber == null)
			return getName();
		
		if(isInOpenWater(bobber))
			return getName() + " [open]";
		else
			return getName() + " [shallow]";
	}
	
	@Override
	public void onEnable()
	{
		Box bb = new Box(-2, -1, -2, 3, 2, 3);
		
		openWaterBox = GL11.glGenLists(1);
		GL11.glNewList(openWaterBox, GL11.GL_COMPILE);
		GL11.glColor4f(0, 1, 0, 0.5F);
		RenderUtils.drawOutlinedBox(bb);
		GL11.glEndList();
		
		shallowWaterBox = GL11.glGenLists(1);
		GL11.glNewList(shallowWaterBox, GL11.GL_COMPILE);
		GL11.glColor4f(1, 0, 0, 0.5F);
		RenderUtils.drawCrossBox(bb);
		RenderUtils.drawOutlinedBox(bb);
		GL11.glEndList();
		
		EVENTS.add(RenderListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(RenderListener.class, this);
		
		GL11.glDeleteLists(openWaterBox, 1);
		GL11.glDeleteLists(shallowWaterBox, 1);
	}
	
	@Override
	public void onRender(float partialTicks)
	{
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
		RenderUtils.applyRegionalRenderOffset();
		
		FishingBobberEntity bobber = MC.player.fishHook;
		if(bobber != null)
			drawOpenWater(bobber);
		
		GL11.glPopMatrix();
		
		// GL resets
		GL11.glColor4f(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_LINE_SMOOTH);
	}
	
	private void drawOpenWater(FishingBobberEntity bobber)
	{
		BlockPos camPos = RenderUtils.getCameraBlockPos();
		int regionX = (camPos.getX() >> 9) * 512;
		int regionZ = (camPos.getZ() >> 9) * 512;
		
		GL11.glPushMatrix();
		BlockPos pos = bobber.getBlockPos();
		GL11.glTranslated(pos.getX() - regionX, pos.getY(),
			pos.getZ() - regionZ);
		boolean inOpenWater = isInOpenWater(bobber);
		GL11.glCallList(inOpenWater ? openWaterBox : shallowWaterBox);
		GL11.glPopMatrix();
	}
	
	private boolean isInOpenWater(FishingBobberEntity bobber)
	{
		return ((IFishingBobberEntity)bobber)
			.checkOpenWaterAround(bobber.getBlockPos());
	}
}
