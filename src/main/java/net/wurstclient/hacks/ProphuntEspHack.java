/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayList;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.RenderListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.util.RenderUtils;

@SearchTags({"prophunt esp"})
public final class ProphuntEspHack extends Hack implements RenderListener
{
	private static final AABB FAKE_BLOCK_BOX =
		new AABB(-0.5, 0, -0.5, 0.5, 1, 0.5);
	
	public ProphuntEspHack()
	{
		super("ProphuntESP");
		setCategory(Category.RENDER);
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(RenderListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(RenderListener.class, this);
	}
	
	@Override
	public void onRender(PoseStack matrixStack, float partialTicks)
	{
		// set color
		float alpha = 0.5F + 0.25F
			* Mth.sin(System.currentTimeMillis() % 1000 / 500F * Mth.PI);
		int color = RenderUtils.toIntColor(new float[]{1, 0, 0}, alpha);
		
		// draw boxes
		ArrayList<AABB> boxes = new ArrayList<>();
		for(Entity entity : MC.level.entitiesForRendering())
		{
			if(!(entity instanceof Mob))
				continue;
			
			if(!entity.isInvisible())
				continue;
			
			if(MC.player.distanceToSqr(entity) < 0.25)
				continue;
			
			boxes.add(FAKE_BLOCK_BOX.move(entity.position()));
		}
		
		RenderUtils.drawSolidBoxes(matrixStack, boxes, color, false);
		RenderUtils.drawOutlinedBoxes(matrixStack, boxes, color, false);
	}
}
