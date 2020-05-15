/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.OptionalInt;

import net.minecraft.item.Items;
import net.minecraft.item.Item;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;


@SearchTags({"auto totem"})
public final class AutoTotemHack extends Hack
	implements UpdateListener
{
	
	
	public AutoTotemHack()
	{
		super("AutoTotem", "Automatically moves totems to your off-hand.");
		setCategory(Category.COMBAT);
	}
	
	@Override
	public void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		if(MC.player.inventory.getInvStack(40).getItem() == Items.TOTEM_OF_UNDYING) {
			return;
		}
		
		if(MC.currentScreen != null) {
			return;
		}
		findItem(Items.TOTEM_OF_UNDYING).ifPresent(slot -> {
			moveItem(slot, 45);
		});
	}
	
	public void moveItem(int slot1, int slot2) {
		IMC.getInteractionManager().windowClick_PICKUP(slot1 < 9 ? 36 + slot1 : slot1);
		IMC.getInteractionManager().windowClick_PICKUP(slot2);
	}
	
	
	  private OptionalInt findItem(final Item item) {
		  for (int i = 0; i <= 36; i++) {
			  if (MC.player.inventory.getInvStack(i).getItem() == item) {
				  return OptionalInt.of(i);
			  }
		  }
		  return OptionalInt.empty();
	  }
}
