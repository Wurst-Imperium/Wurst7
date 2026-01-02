/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayList;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.EntityUtils;

@SearchTags({"shears aura", "SheepAura", "sheep aura", "autoshears", "auto shears"})
public final class ShearsAuraHack extends GenericInteractHack
{
	private final SliderSetting range = new SliderSetting("Range",
		"Determines how far ShearsAura will reach to shear sheep.\n"
			+ "Anything that is further away than the specified value will not be sheared.",
		5, 1, 10, 0.05, ValueDisplay.DECIMAL);
	
	private final Random random = new Random();
	
	public ShearsAuraHack()
	{
		super("ShearsAura");
		setCategory(Category.OTHER);
		addSetting(range);
	}
	
	@Override
	public void onUpdate()
	{
		LocalPlayer player = MC.player;
		ItemStack heldStack = player.getInventory().getSelectedItem();
		
		double rangeSq = range.getValueSq();
		Stream<Animal> stream = EntityUtils.getValidAnimals();
		
		if(heldStack.getItem() instanceof ShearsItem)
			stream = stream.filter(e -> player.distanceToSqr(e) <= rangeSq)
				.filter(e -> e instanceof Sheep)
				.filter(e -> ((Sheep)e).readyForShearing());
		else
			return;
		
		
		// convert targets to list
		ArrayList<Animal> targets =
			stream.collect(Collectors.toCollection(ArrayList::new));
		
		// pick a target at random
		target = targets.isEmpty() ? null
			: targets.get(random.nextInt(targets.size()));
		
		renderTarget = target;
		if(target == null)
			return;
		
		WURST.getRotationFaker()
			.faceVectorPacket(target.getBoundingBox().getCenter());
	}
}
