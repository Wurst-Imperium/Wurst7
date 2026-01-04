/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.AirStrafingSpeedListener;
import net.wurstclient.events.IsPlayerInWaterListener;
import net.wurstclient.events.MouseScrollListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.mixinterface.IKeyMapping;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

@SearchTags({"FlyHack", "fly hack", "flying"})
public final class FlightHack extends Hack implements UpdateListener,
	IsPlayerInWaterListener, AirStrafingSpeedListener, MouseScrollListener
{
	private final SliderSetting horizontalSpeed = new SliderSetting(
		"Horizontal speed", "description.wurst.setting.flight.horizontal_speed",
		1, 0.05, 10, 0.05, ValueDisplay.DECIMAL);
	
	private final SliderSetting verticalSpeed =
		new SliderSetting("Vertical speed",
			"description.wurst.setting.flight.vertical_speed", 1, 0.05, 5, 0.05,
			v -> ValueDisplay.DECIMAL.getValueString(getActualVerticalSpeed()));
	
	private final CheckboxSetting allowUnsafeVerticalSpeed =
		new CheckboxSetting("Allow unsafe vertical speed",
			"description.wurst.setting.flight.allow_unsafe_vertical_speed",
			false);
	
	private final CheckboxSetting scrollToChangeSpeed =
		new CheckboxSetting("Scroll to change speed",
			"description.wurst.setting.flight.scroll_to_change_speed", true);
	
	private final CheckboxSetting renderSpeed =
		new CheckboxSetting("Show speed in HackList",
			"description.wurst.setting.flight.show_speed_in_hacklist", true);
	
	private final CheckboxSetting antiKick = new CheckboxSetting("Anti-Kick",
		"description.wurst.setting.flight.anti-kick", false);
	
	private final SliderSetting antiKickInterval =
		new SliderSetting("Anti-Kick Interval",
			"description.wurst.setting.flight.anti-kick_interval", 70, 5, 80, 1,
			ValueDisplay.INTEGER.withSuffix(" ticks").withLabel(1, "1 tick"));
	
	private final SliderSetting antiKickDistance =
		new SliderSetting("Anti-Kick Distance",
			"description.wurst.setting.flight.anti-kick_distance", 0.035, 0.01,
			0.2, 0.001, ValueDisplay.DECIMAL.withSuffix("m"));
	
	private int tickCounter = 0;
	
	public FlightHack()
	{
		super("Flight");
		setCategory(Category.MOVEMENT);
		addSetting(horizontalSpeed);
		addSetting(verticalSpeed);
		addSetting(allowUnsafeVerticalSpeed);
		addSetting(scrollToChangeSpeed);
		addSetting(renderSpeed);
		addSetting(antiKick);
		addSetting(antiKickInterval);
		addSetting(antiKickDistance);
	}
	
	@Override
	public String getRenderName()
	{
		if(!renderSpeed.isChecked())
			return getName();
		
		return getName() + " [" + horizontalSpeed.getValueString() + ", "
			+ verticalSpeed.getValueString() + "]";
	}
	
	@Override
	protected void onEnable()
	{
		tickCounter = 0;
		
		WURST.getHax().creativeFlightHack.setEnabled(false);
		WURST.getHax().jetpackHack.setEnabled(false);
		
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(IsPlayerInWaterListener.class, this);
		EVENTS.add(AirStrafingSpeedListener.class, this);
		EVENTS.add(MouseScrollListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(IsPlayerInWaterListener.class, this);
		EVENTS.remove(AirStrafingSpeedListener.class, this);
		EVENTS.remove(MouseScrollListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		if(WURST.getHax().freecamHack.isEnabled())
			return;
		
		LocalPlayer player = MC.player;
		player.setDeltaMovement(Vec3.ZERO);
		player.getAbilities().flying = false;
		
		double vSpeed = getActualVerticalSpeed();
		
		if(MC.options.keyJump.isDown())
			player.addDeltaMovement(new Vec3(0, vSpeed, 0));
		
		if(IKeyMapping.get(MC.options.keyShift).isActuallyDown())
		{
			MC.options.keyShift.setDown(false);
			player.addDeltaMovement(new Vec3(0, -vSpeed, 0));
		}
		
		if(antiKick.isChecked())
			doAntiKick();
	}
	
	@Override
	public void onGetAirStrafingSpeed(AirStrafingSpeedEvent event)
	{
		if(WURST.getHax().freecamHack.isEnabled())
			return;
		
		event.setSpeed(horizontalSpeed.getValueF());
	}
	
	@Override
	public void onMouseScroll(double amount)
	{
		if(!isControllingScrollEvents())
			return;
		
		if(amount > 0)
			horizontalSpeed.increaseValue();
		else if(amount < 0)
			horizontalSpeed.decreaseValue();
	}
	
	public boolean isControllingScrollEvents()
	{
		return isEnabled() && scrollToChangeSpeed.isChecked()
			&& !WURST.getOtfs().zoomOtf.isControllingScrollEvents()
			&& !WURST.getHax().freecamHack.isEnabled();
	}
	
	private void doAntiKick()
	{
		if(tickCounter > antiKickInterval.getValueI() + 1)
			tickCounter = 0;
		
		Vec3 velocity = MC.player.getDeltaMovement();
		
		switch(tickCounter)
		{
			case 0 ->
			{
				if(velocity.y <= -antiKickDistance.getValue())
					tickCounter = 2;
				else
					MC.player.setDeltaMovement(velocity.x,
						-antiKickDistance.getValue(), velocity.z);
			}
			
			case 1 -> MC.player.setDeltaMovement(velocity.x,
				antiKickDistance.getValue(), velocity.z);
		}
		
		tickCounter++;
	}
	
	@Override
	public void onIsPlayerInWater(IsPlayerInWaterEvent event)
	{
		event.setInWater(false);
	}
	
	public double getHorizontalSpeed()
	{
		return horizontalSpeed.getValue();
	}
	
	public double getActualVerticalSpeed()
	{
		boolean limitVerticalSpeed = !allowUnsafeVerticalSpeed.isChecked()
			&& !MC.player.getAbilities().invulnerable;
		
		return Mth.clamp(horizontalSpeed.getValue() * verticalSpeed.getValue(),
			0.05, limitVerticalSpeed ? 3.95 : 10);
	}
}
