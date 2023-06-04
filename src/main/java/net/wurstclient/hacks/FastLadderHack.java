/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;

@SearchTags({"FastClimb", "fast ladder", "fast climb"})
public final class FastLadderHack extends Hack implements UpdateListener
{
	private final SliderSetting speed = new SliderSetting("Speed",
				"Determines how fast you will climb.",
					2.44, 0, 5, 0.01, SliderSetting.ValueDisplay.PERCENTAGE);

	private final CheckboxSetting slowonsneak = new CheckboxSetting("Slow down when sneaking",
			"Doesn't speed your climbing up when sneaking", true);

	public FastLadderHack()
	{
		super("FastLadder");
		setCategory(Category.MOVEMENT);
		addSetting(speed);
		addSetting(slowonsneak);
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
		ClientPlayerEntity player = MC.player;
		
		if(!player.isClimbing())
			return;

		if(player.input.sneaking && slowonsneak.isChecked())
			return;

		if(!player.input.jumping && player.input.movementForward == 0 && player.input.movementSideways == 0)
			return;
		
		Vec3d velocity = player.getVelocity();
		player.setVelocity(velocity.x / 2, (0.2872 / 2.44 * speed.getPercentage() * speed.getMaximum()), velocity.z / 2);
	}
}
