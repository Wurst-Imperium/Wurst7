/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
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
import net.wurstclient.events.IsPlayerInWaterListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.settings.CheckboxSetting;

@SearchTags({"FlyHack", "fly hack", "flying"})
public final class FlightHack extends Hack
	implements UpdateListener, IsPlayerInWaterListener
{
	public final SliderSetting speed =
		new SliderSetting("Speed", 1, 0.05, 5, 0.05, ValueDisplay.DECIMAL);
	private final CheckboxSetting antiKick =
		new CheckboxSetting("Anti Kick", "Makes you fall a little bit every now and then.", false);
	private final SliderSetting antiKickInterval =
		new SliderSetting("Anti Kick Interval", 30, 5, 100, 1.0, ValueDisplay.INTEGER);

	private int tickCounter = 0;

	public FlightHack()
	{
		super("Flight");
		setCategory(Category.MOVEMENT);
		addSetting(speed);
		addSetting(antiKick);
		addSetting(antiKickInterval);
	}
	
	@Override
	public void onEnable()
	{
		tickCounter = 0;

		WURST.getHax().creativeFlightHack.setEnabled(false);
		WURST.getHax().jetpackHack.setEnabled(false);
		
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(IsPlayerInWaterListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(IsPlayerInWaterListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		ClientPlayerEntity player = MC.player;
		
		player.getAbilities().flying = false;
		player.airStrafingSpeed = speed.getValueF();
		
		player.setVelocity(0, 0, 0);
		Vec3d velocity = player.getVelocity();
		
		if (antiKick.isChecked()) {
			if (tickCounter > antiKickInterval.getValueI() + 1) {
				tickCounter = 0;
			}
			if (tickCounter == 0) {
				if (MC.options.sneakKey.isPressed()) {
					tickCounter = 2;
				}
				else {
					player.setVelocity(velocity.subtract(0, 0.07D, 0));
					tickCounter++;
					return;
				}
			}
			else if (tickCounter == 1) {
				player.setVelocity(velocity.add(0, 0.07D, 0));
				tickCounter++;
				return;
			}
			else {
				tickCounter++;
			}
		}

		if(MC.options.jumpKey.isPressed())
			player.setVelocity(velocity.add(0, speed.getValue(), 0));
		
		if(MC.options.sneakKey.isPressed())
			player.setVelocity(velocity.subtract(0, speed.getValue(), 0));
	}
	
	@Override
	public void onIsPlayerInWater(IsPlayerInWaterEvent event)
	{
		event.setInWater(false);
	}
}
