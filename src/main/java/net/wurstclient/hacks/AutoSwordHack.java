/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.EntityUtils;
import net.wurstclient.util.ItemUtils;

@SearchTags({"auto sword"})
public final class AutoSwordHack extends Hack implements UpdateListener
{
	private final EnumSetting<Priority> priority =
		new EnumSetting<>("Priority", Priority.values(), Priority.SPEED);
	
	private final CheckboxSetting switchBack = new CheckboxSetting(
		"Switch back", "Switches back to the previously selected slot after"
			+ " \u00a7lRelease time\u00a7r has passed.",
		true);
	
	private final SliderSetting releaseTime = new SliderSetting("Release time",
		"Time until AutoSword will switch back from the weapon to the"
			+ " previously selected slot.\n\n"
			+ "Only works when \u00a7lSwitch back\u00a7r is checked.",
		10, 1, 200, 1,
		ValueDisplay.INTEGER.withSuffix(" ticks").withLabel(1, "1 tick"));
	
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
	protected void onEnable()
	{
		oldSlot = -1;
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		resetSlot();
	}
	
	@Override
	public void onUpdate()
	{
		if(MC.hitResult != null
			&& MC.hitResult.getType() == HitResult.Type.ENTITY)
		{
			Entity entity = ((EntityHitResult)MC.hitResult).getEntity();
			
			if(entity instanceof LivingEntity
				&& EntityUtils.IS_ATTACKABLE.test(entity))
				setSlot(entity);
		}
		
		// update timer
		if(timer > 0)
		{
			timer--;
			return;
		}
		
		resetSlot();
	}
	
	public void setSlot(Entity entity)
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
			if(MC.player.getInventory().getItem(i).isEmpty())
				continue;
			
			// get weapon value
			ItemStack stack = MC.player.getInventory().getItem(i);
			float value = getValue(stack, entity);
			
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
			oldSlot = MC.player.getInventory().getSelectedSlot();
		
		// set slot
		MC.player.getInventory().setSelectedSlot(bestSlot);
		
		// start timer
		timer = releaseTime.getValueI();
	}
	
	private float getValue(ItemStack stack, Entity entity)
	{
		Item item = stack.getItem();
		if(stack.get(DataComponents.TOOL) == null
			&& stack.get(DataComponents.WEAPON) == null)
			return Integer.MIN_VALUE;
		
		switch(priority.getSelected())
		{
			case SPEED:
			return (float)ItemUtils.getAttribute(item, Attributes.ATTACK_SPEED)
				.orElse(Integer.MIN_VALUE);
			
			// Client-side item-specific attack damage calculation no
			// longer exists as of 24w18a (1.21). Related bug: MC-196250
			case DAMAGE:
			// EntityType<?> group = entity.getType();
			float dmg =
				(float)ItemUtils.getAttribute(item, Attributes.ATTACK_DAMAGE)
					.orElse(Integer.MIN_VALUE);
			
			// Check for mace, get bonus damage from fall
			if(item instanceof MaceItem mace)
				dmg = mace.getAttackDamageBonus(MC.player, dmg,
					entity.damageSources().playerAttack(MC.player));
			// dmg += EnchantmentHelper.getAttackDamage(stack, group);
			return dmg;
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
			MC.player.getInventory().setSelectedSlot(oldSlot);
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
}
