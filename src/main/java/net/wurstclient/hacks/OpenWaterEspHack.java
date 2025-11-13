/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.Optional;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.phys.AABB;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.RenderListener;
import net.wurstclient.hack.Hack;
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
		FishingHook bobber = Optional.ofNullable(MC.player)
			.map(player -> player.fishing).orElse(null);
		if(bobber == null)
			return getName();
		
		return getName() + (isInOpenWater(bobber) ? " [open]" : " [shallow]");
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
		FishingHook bobber = MC.player.fishing;
		if(bobber == null)
			return;
		
		AABB box = new AABB(-2, -1, -2, 3, 2, 3).move(bobber.blockPosition());
		boolean inOpenWater = isInOpenWater(bobber);
		int color = inOpenWater ? 0x8000FF00 : 0x80FF0000;
		
		if(!inOpenWater)
			RenderUtils.drawCrossBox(matrixStack, box, color, false);
		
		RenderUtils.drawOutlinedBox(matrixStack, box, color, false);
	}
	
	private boolean isInOpenWater(FishingHook bobber)
	{
		return bobber.calculateOpenWater(bobber.blockPosition());
	}
}
