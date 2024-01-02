/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.RenderListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.util.RegionPos;
import net.wurstclient.util.RenderUtils;

@SearchTags({"open water esp", "AutoFishESP", "auto fish esp"})
public final class OpenWaterEspHack extends Hack implements RenderListener
{
	public OpenWaterEspHack()
	{
		super("OpenWaterESP");
		
		setCategory(Category.RENDER);
	}
	
	@Override
	public String getRenderName()
	{
		FishingBobberEntity bobber = MC.player.fishHook;
		if(bobber == null)
			return getName();
		
		return getName() + (isInOpenWater(bobber) ? " [open]" : " [shallow]");
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
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		// GL settings
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		
		matrixStack.push();
		RenderUtils.applyRegionalRenderOffset(matrixStack);
		
		FishingBobberEntity bobber = MC.player.fishHook;
		if(bobber != null)
			drawOpenWater(matrixStack, bobber);
		
		matrixStack.pop();
		
		// GL resets
		RenderSystem.setShaderColor(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_BLEND);
	}
	
	private void drawOpenWater(MatrixStack matrixStack,
		FishingBobberEntity bobber)
	{
		RegionPos region = RenderUtils.getCameraRegion();
		
		matrixStack.push();
		BlockPos pos = bobber.getBlockPos().subtract(region.toBlockPos());
		matrixStack.translate(pos.getX(), pos.getY(), pos.getZ());
		
		Box bb = new Box(-2, -1, -2, 3, 2, 3);
		
		if(isInOpenWater(bobber))
			RenderSystem.setShaderColor(0, 1, 0, 0.5F);
		else
		{
			RenderSystem.setShaderColor(1, 0, 0, 0.5F);
			RenderUtils.drawCrossBox(bb, matrixStack);
		}
		RenderUtils.drawOutlinedBox(bb, matrixStack);
		
		matrixStack.pop();
	}
	
	private boolean isInOpenWater(FishingBobberEntity bobber)
	{
		return bobber.isOpenOrWaterAround(bobber.getBlockPos());
	}
}
