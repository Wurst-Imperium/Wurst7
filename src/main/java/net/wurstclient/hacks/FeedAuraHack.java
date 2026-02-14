/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
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
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.item.ItemStack;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.settings.filters.FilterBabiesSetting;
import net.wurstclient.util.EntityUtils;

@SearchTags({"feed aura", "BreedAura", "breed aura", "AutoBreeder",
	"auto breeder"})
public final class FeedAuraHack extends GenericInteractHack
{
	private final SliderSetting range = new SliderSetting("Range",
		"Determines how far FeedAura will reach to feed animals.\n"
			+ "Anything that is further away than the specified value will not be fed.",
		5, 1, 10, 0.05, ValueDisplay.DECIMAL);
	
	private final FilterBabiesSetting filterBabies =
		new FilterBabiesSetting("Won't feed baby animals.\n"
			+ "Saves food, but doesn't speed up baby growth.", true);
	
	private final CheckboxSetting filterUntamed =
		new CheckboxSetting("Filter untamed",
			"Won't feed tameable animals that haven't been tamed yet.", false);
	
	private final CheckboxSetting filterHorses = new CheckboxSetting(
		"Filter horse-like animals",
		"Won't feed horses, llamas, donkeys, etc.\n"
			+ "Recommended in Minecraft versions before 1.20.3 due to MC-233276,"
			+ "which causes these animals to consume items indefinitely.",
		false);
	
	private final Random random = new Random();
	
	public FeedAuraHack()
	{
		super("FeedAura");
		setCategory(Category.OTHER);
		addSetting(range);
		addSetting(filterBabies);
		addSetting(filterUntamed);
		addSetting(filterHorses);
	}
	
	@Override
	public void onUpdate()
	{
		LocalPlayer player = MC.player;
		ItemStack heldStack = player.getInventory().getSelectedItem();
		
		double rangeSq = range.getValueSq();
		Stream<Animal> stream = EntityUtils.getValidAnimals();
		
		
		stream = stream.filter(e -> player.distanceToSqr(e) <= rangeSq)
			.filter(e -> e.isFood(heldStack)).filter(Animal::canFallInLove);
		
		if(filterBabies.isChecked())
			stream = stream.filter(filterBabies);
		
		if(filterUntamed.isChecked())
			stream = stream.filter(e -> !isUntamed(e));
		
		if(filterHorses.isChecked())
			stream = stream.filter(e -> !(e instanceof AbstractHorse));
		
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
