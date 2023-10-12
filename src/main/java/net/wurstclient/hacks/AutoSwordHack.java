/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.stream.Stream;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolItem;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.ItemUtils;
import net.wurstclient.util.EntityUtils;

@SearchTags({"auto sword"})
public final class AutoSwordHack extends Hack implements UpdateListener
{
	private final EnumSetting<Priority> priority =
		new EnumSetting<>("Priority", Priority.values(), Priority.SPEED);
	
	private final CheckboxSetting switchBack = new CheckboxSetting(
		"Switch back",
		"Switches back to the previously selected slot after \u00a7lRelease time\u00a7r has passed.",
		true);
	
	private final SliderSetting releaseTime = new SliderSetting("Release time",
		"Time until AutoSword will switch back from the weapon to the previously selected slot.\n\n"
			+ "Only works when \u00a7lSwitch back\u00a7r is checked.",
		10, 1, 200, 1, ValueDisplay.INTEGER.withSuffix(" ticks"));
	
	private int oldSlot;
	private int timer;
	
	public AutoSwordHack()
	{
		super("AutoSword");
		setCategory(Category.COMBAT);
		
		addSetting(priority);
		addSetting(switchBack);
		addSetting(releaseTime);
	}
	
	@Override
	public void onEnable()
	{
		oldSlot = -1;
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		resetSlot();
	}
	
	@Override
	public void onUpdate()
	{
		if(MC.crosshairTarget != null
			&& MC.crosshairTarget.getType() == HitResult.Type.ENTITY)
		{
			Entity entity = ((EntityHitResult)MC.crosshairTarget).getEntity();
				//check if entity is correct
				if(!isCorrectEntity(entity))
					return;

			if(entity instanceof LivingEntity
				&& ((LivingEntity)entity).getHealth() > 0)
				setSlot();
		}
		
		// update timer
		if(timer > 0)
		{
			timer--;
			return;
		}
		
		resetSlot();
	}
	
	public void setSlot()
	{
		// check if active
		if(!isEnabled())
			return;
		
		// wait for AutoEat
		if(WURST.getHax().autoEatHack.isEating())
			return;
		
		// find best weapon
		float bestValue = Integer.MIN_VALUE;
		int bestSlot = -1;
		for(int i = 0; i < 9; i++)
		{
			// skip empty slots
			if(MC.player.getInventory().getStack(i).isEmpty())
				continue;
			
			Item item = MC.player.getInventory().getStack(i).getItem();
			
			// get damage
			float value = getValue(item);
			
			// compare with previous best weapon
			if(value > bestValue)
			{
				bestValue = value;
				bestSlot = i;
			}
		}
		
		// check if any weapon was found
		if(bestSlot == -1)
			return;
		
		// save old slot
		if(oldSlot == -1)
			oldSlot = MC.player.getInventory().selectedSlot;
		
		// set slot
		MC.player.getInventory().selectedSlot = bestSlot;
		
		// start timer
		timer = releaseTime.getValueI();
	}
	
	private float getValue(Item item)
	{
		switch(priority.getSelected())
		{
			case SPEED:
			if(item instanceof ToolItem tool)
				return ItemUtils.getAttackSpeed(tool);
			break;
			
			case DAMAGE:
			if(item instanceof SwordItem sword)
				return sword.getAttackDamage();
			if(item instanceof MiningToolItem miningTool)
				return miningTool.getAttackDamage();
			break;
		}
		
		return Integer.MIN_VALUE;
	}
	
	private void resetSlot()
	{
		if(!switchBack.isChecked())
		{
			oldSlot = -1;
			return;
		}
		
		if(oldSlot != -1)
		{
			MC.player.getInventory().selectedSlot = oldSlot;
			oldSlot = -1;
		}
	}
	
	private enum Priority
	{
		SPEED("Speed (swords)"),
		DAMAGE("Damage (axes)");
		
		private final String name;
		
		private Priority(String name)
		{
			this.name = name;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
	private boolean isCorrectEntity(Entity entity)
	{
		Stream<Entity> stream = Stream.of(entity);
		stream = stream.filter(EntityUtils.IS_ATTACKABLE);
		
		return stream.findFirst().isPresent();
	}
}

