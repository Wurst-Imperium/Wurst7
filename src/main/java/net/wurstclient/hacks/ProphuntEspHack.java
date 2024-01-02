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
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.RenderListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.util.RegionPos;
import net.wurstclient.util.RenderUtils;

@SearchTags({"prophunt esp"})
public final class ProphuntEspHack extends Hack implements RenderListener
{
	private static final Box FAKE_BLOCK_BOX =
		new Box(-0.5, 0, -0.5, 0.5, 1, 0.5);
	
	public ProphuntEspHack()
	{
		super("ProphuntESP");
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
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		// GL settings
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		
		matrixStack.push();
		
		RegionPos region = RenderUtils.getCameraRegion();
		RenderUtils.applyRegionalRenderOffset(matrixStack, region);
		
		// set color
		float alpha = 0.5F + 0.25F * MathHelper
			.sin(System.currentTimeMillis() % 1000 / 500F * (float)Math.PI);
		RenderSystem.setShaderColor(1, 0, 0, alpha);
		
		// draw boxes
		for(Entity entity : MC.world.getEntities())
		{
			if(!(entity instanceof MobEntity))
				continue;
			
			if(!entity.isInvisible())
				continue;
			
			if(MC.player.squaredDistanceTo(entity) < 0.25)
				continue;
			
			matrixStack.push();
			matrixStack.translate(entity.getX() - region.x(), entity.getY(),
				entity.getZ() - region.z());
			
			RenderUtils.drawOutlinedBox(FAKE_BLOCK_BOX, matrixStack);
			RenderUtils.drawSolidBox(FAKE_BLOCK_BOX, matrixStack);
			
			matrixStack.pop();
		}
		
		matrixStack.pop();
		
		// GL resets
		RenderSystem.setShaderColor(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_BLEND);
	}
}
