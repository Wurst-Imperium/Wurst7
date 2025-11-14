/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.mixinterface.IKeyBinding;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

@SearchTags({"creative flight", "CreativeFly", "creative fly"})
public final class CreativeFlightHack extends Hack implements UpdateListener
{
	private final CheckboxSetting antiKick = new CheckboxSetting("Anti-Kick",
		"Makes you fall a little bit every now and then to prevent you from getting kicked.",
		false);
	
	private final SliderSetting antiKickInterval =
		new SliderSetting("Anti-Kick Interval",
			"How often Anti-Kick should prevent you from getting kicked.\n"
				+ "Most servers will kick you after 80 ticks.",
			30, 5, 80, 1, SliderSetting.ValueDisplay.INTEGER
				.withSuffix(" ticks").withLabel(1, "1 tick"));
	
	private final SliderSetting antiKickDistance = new SliderSetting(
		"Anti-Kick Distance",
		"How far Anti-Kick should make you fall.\n"
			+ "Most servers require at least 0.032m to stop you from getting kicked.",
		0.07, 0.01, 0.2, 0.001, ValueDisplay.DECIMAL.withSuffix("m"));
	
	private int tickCounter = 0;
	
	public CreativeFlightHack()
	{
		super("CreativeFlight");
		setCategory(Category.MOVEMENT);
		addSetting(antiKick);
		addSetting(antiKickInterval);
		addSetting(antiKickDistance);
	}
	
	@Override
	protected void onEnable()
	{
		tickCounter = 0;
		
		WURST.getHax().jetpackHack.setEnabled(false);
		WURST.getHax().flightHack.setEnabled(false);
		
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		
		LocalPlayer player = MC.player;
		Abilities abilities = player.getAbilities();
		
		boolean creative = player.isCreative();
		abilities.flying = creative && !player.onGround();
		abilities.mayfly = creative;
		
		restoreKeyPresses();
	}
	
	@Override
	public void onUpdate()
	{
		Abilities abilities = MC.player.getAbilities();
		abilities.mayfly = true;
		
		if(antiKick.isChecked() && abilities.flying)
			doAntiKick();
	}
	
	private void doAntiKick()
	{
		if(tickCounter > antiKickInterval.getValueI() + 2)
			tickCounter = 0;
		
		switch(tickCounter)
		{
			case 0 ->
			{
				if(MC.options.keyShift.isDown() && !MC.options.keyJump.isDown())
					tickCounter = 3;
				else
					setMotionY(-antiKickDistance.getValue());
			}
			
			case 1 -> setMotionY(antiKickDistance.getValue());
			
			case 2 -> setMotionY(0);
			
			case 3 -> restoreKeyPresses();
		}
		
		tickCounter++;
	}
	
	private void setMotionY(double motionY)
	{
		MC.options.keyShift.setDown(false);
		MC.options.keyJump.setDown(false);
		
		Vec3 velocity = MC.player.getDeltaMovement();
		MC.player.setDeltaMovement(velocity.x, motionY, velocity.z);
	}
	
	private void restoreKeyPresses()
	{
		KeyMapping[] keys = {MC.options.keyJump, MC.options.keyShift};
		
		for(KeyMapping key : keys)
			IKeyBinding.get(key).resetPressedState();
	}
}
