/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayList;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.RenderListener;
import net.wurstclient.hack.Hack;
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
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		// set color
		float alpha = 0.5F + 0.25F * MathHelper
			.sin(System.currentTimeMillis() % 1000 / 500F * MathHelper.PI);
		int color = RenderUtils.toIntColor(new float[]{1, 0, 0}, alpha);
		
		// draw boxes
		ArrayList<Box> boxes = new ArrayList<>();
		for(Entity entity : MC.world.getEntities())
		{
			if(!(entity instanceof MobEntity))
				continue;
			
			if(!entity.isInvisible())
				continue;
			
			if(MC.player.squaredDistanceTo(entity) < 0.25)
				continue;
			
			boxes.add(FAKE_BLOCK_BOX.offset(entity.getPos()));
		}
		
		RenderUtils.drawSolidBoxes(matrixStack, boxes, color, false);
		RenderUtils.drawOutlinedBoxes(matrixStack, boxes, color, false);
	}
}
