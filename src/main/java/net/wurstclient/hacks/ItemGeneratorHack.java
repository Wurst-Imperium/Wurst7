/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.Random;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.util.registry.Registry;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.ChatUtils;

@SearchTags({"item generator", "drop infinite"})
public final class ItemGeneratorHack extends Hack implements UpdateListener
{
	private final SliderSetting speed = new SliderSetting("Speed",
		"\u00a74\u00a7lWARNING:\u00a7r High speeds will cause a ton\n"
			+ "of lag and can easily crash the game!",
		1, 1, 36, 1, ValueDisplay.INTEGER);
	
	private final SliderSetting stackSize = new SliderSetting("Stack size",
		"How many items to place in each stack.\n"
			+ "Doesn't seem to affect performance.",
		1, 1, 64, 1, ValueDisplay.INTEGER);
	
	private final Random random = new Random();
	
	public ItemGeneratorHack()
	{
		super("ItemGenerator",
			"Generates random items and drops them on the ground.\n"
				+ "\u00a7oCreative mode only.\u00a7r");
		
		setCategory(Category.ITEMS);
		addSetting(speed);
		addSetting(stackSize);
	}
	
	@Override
	public void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
		
		if(!MC.player.abilities.creativeMode)
		{
			ChatUtils.error("Creative mode only.");
			setEnabled(false);
		}
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		int stacks = speed.getValueI();
		for(int i = 9; i < 9 + stacks; i++)
		{
			Item item = Registry.ITEM.getRandom(random);
			ItemStack stack = new ItemStack(item, stackSize.getValueI());
			
			CreativeInventoryActionC2SPacket packet =
				new CreativeInventoryActionC2SPacket(i, stack);
			
			MC.player.networkHandler.sendPacket(packet);
		}
		
		for(int i = 9; i < 9 + stacks; i++)
			IMC.getInteractionManager().windowClick_THROW(i);
	}
}
