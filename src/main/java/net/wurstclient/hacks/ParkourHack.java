/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.util.math.Box;
import net.wurstclient.Category;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

public final class ParkourHack extends Hack implements UpdateListener
{
	private final SliderSetting minDepth = new SliderSetting("Min depth",
		"Won't jump over a pit if it isn't at least this deep.\n"
			+ "Increase to stop Parkour from jumping down stairs.\n"
			+ "Decrease to make Parkour jump at the edge of carpets.",
		0.5, 0.05, 10, 0.05, ValueDisplay.DECIMAL.withSuffix("m"));
	
	private final SliderSetting edgeDistance =
		new SliderSetting("Edge distance",
			"How close Parkour will let you get to the edge before jumping.",
			0.001, 0.001, 0.25, 0.001, ValueDisplay.DECIMAL.withSuffix("m"));
	
	private final CheckboxSetting sneak = new CheckboxSetting(
		"Jump while sneaking",
		"Keeps Parkour active even while you are sneaking.\n"
			+ "You may want to increase the \u00a7lEdge \u00a7ldistance\u00a7r"
			+ " slider when using this option.",
		false);
	
	public ParkourHack()
	{
		super("Parkour");
		setCategory(Category.MOVEMENT);
		addSetting(minDepth);
		addSetting(edgeDistance);
		addSetting(sneak);
	}
	
	@Override
	public void onEnable()
	{
		WURST.getHax().safeWalkHack.setEnabled(false);
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
		if(!MC.player.isOnGround() || MC.options.jumpKey.isPressed())
			return;
		
		if(!sneak.isChecked()
			&& (MC.player.isSneaking() || MC.options.sneakKey.isPressed()))
			return;
		
		Box box = MC.player.getBoundingBox();
		Box adjustedBox = box.stretch(0, -minDepth.getValue(), 0)
			.expand(-edgeDistance.getValue(), 0, -edgeDistance.getValue());
		
		if(!MC.world.isSpaceEmpty(MC.player, adjustedBox))
			return;
		
		MC.player.jump();
	}
}
