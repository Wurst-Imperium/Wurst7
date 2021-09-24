/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.wurstclient.Category;
import net.wurstclient.events.RightClickListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

public final class ThrowHack extends Hack implements RightClickListener
{
	private final SliderSetting amount = new SliderSetting("数量",
		"每次点击后的使用量", 16, 2, 1000000, 1, ValueDisplay.INTEGER);
	
	public ThrowHack()
	{
		super("重复","使你使用一次某个物品就相当于用很多次\n(适用于雪球,鸡蛋,刷怪蛋,放置矿车等)\n比如扔一次鸡蛋会飞出去很多个鸡蛋\n这可能会导致严重的延迟,\n甚至导致服务器崩溃.");
		
		setCategory(Category.OTHER);
		addSetting(amount);
	}
	
	@Override
	public String getRenderName()
	{
		return getName() + " [" + amount.getValueString() + "]";
	}
	
	@Override
	public void onEnable()
	{
		EVENTS.add(RightClickListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(RightClickListener.class, this);
	}
	
	@Override
	public void onRightClick(RightClickEvent event)
	{
		if(IMC.getItemUseCooldown() > 0)
			return;
		
		if(!MC.options.keyUse.isPressed())
			return;
		
		for(int i = 0; i < amount.getValueI(); i++)
		{
			if(MC.crosshairTarget.getType() == HitResult.Type.BLOCK)
			{
				BlockHitResult hitResult = (BlockHitResult)MC.crosshairTarget;
				IMC.getInteractionManager().rightClickBlock(
					hitResult.getBlockPos(), hitResult.getSide(),
					hitResult.getPos());
			}
			
			IMC.getInteractionManager().rightClickItem();
		}
	}
}
