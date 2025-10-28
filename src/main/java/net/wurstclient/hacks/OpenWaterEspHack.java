/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.util.math.Box;
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
		if(MC.player == null)
			return getName();
		
		FishingBobberEntity bobber = MC.player.fishHook;
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
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		FishingBobberEntity bobber = MC.player.fishHook;
		if(bobber == null)
			return;
		
		Box box = new Box(-2, -1, -2, 3, 2, 3).offset(bobber.getBlockPos());
		boolean inOpenWater = isInOpenWater(bobber);
		int color = inOpenWater ? 0x8000FF00 : 0x80FF0000;
		
		if(!inOpenWater)
			RenderUtils.drawCrossBox(matrixStack, box, color, false);
		
		RenderUtils.drawOutlinedBox(matrixStack, box, color, false);
	}
	
	private boolean isInOpenWater(FishingBobberEntity bobber)
	{
		return bobber.isOpenOrWaterAround(bobber.getBlockPos());
	}
}
