/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.mixinterface.IKeyBinding;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;

@SearchTags({"creative flight", "CreativeFly", "creative fly"})
public final class CreativeFlightHack extends Hack implements UpdateListener
{
	private final CheckboxSetting antiKick =
			new CheckboxSetting("Anti Kick", "Makes you fall a little bit every now and then.", false);
	private final SliderSetting antiKickInterval =
			new SliderSetting("Anti Kick Interval", 30, 5, 100, 1.0, SliderSetting.ValueDisplay.INTEGER);

	private int tickCounter = 0;

	public CreativeFlightHack()
	{
		super("CreativeFlight");
		setCategory(Category.MOVEMENT);
		addSetting(antiKick);
		addSetting(antiKickInterval);
	}
	
	@Override
	public void onEnable()
	{
		tickCounter = 0;

		WURST.getHax().jetpackHack.setEnabled(false);
		WURST.getHax().flightHack.setEnabled(false);
		
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		
		ClientPlayerEntity player = MC.player;
		PlayerAbilities abilities = player.getAbilities();
		
		boolean creative = player.isCreative();
		abilities.flying = creative && !player.isOnGround();
		abilities.allowFlying = creative;

		restoreKeyPress();
	}
	
	@Override
	public void onUpdate()
	{
		PlayerAbilities abilities = MC.player.getAbilities();
		abilities.allowFlying = true;

		if (antiKick.isChecked() && abilities.flying) {
			Vec3d velocity = MC.player.getVelocity();
			if (tickCounter > antiKickInterval.getValueI() + 2) {
				tickCounter = 0;
			}
			switch (tickCounter) {
				case 0 -> {
					if (MC.options.sneakKey.isPressed()) {
						tickCounter = 3;
					}
					else {
						MC.options.sneakKey.setPressed(false);
						MC.options.jumpKey.setPressed(false);
						MC.player.setVelocity(velocity.x, -0.07D, velocity.z);
					}
				}
				case 1 -> {
					MC.options.sneakKey.setPressed(false);
					MC.options.jumpKey.setPressed(false);
					MC.player.setVelocity(velocity.x, 0.07D, velocity.z);
				}
				case 2 -> {
					MC.options.sneakKey.setPressed(false);
					MC.options.jumpKey.setPressed(false);
					MC.player.setVelocity(velocity.x, 0, velocity.z);
				}
				case 3 -> {
					restoreKeyPress();
				}
			}
			tickCounter++;
		}
	}
	private void restoreKeyPress() {
		IKeyBinding sneakKey = (IKeyBinding) MC.options.sneakKey;
		((KeyBinding) sneakKey).setPressed(sneakKey.isActallyPressed());
		IKeyBinding jumpKey = (IKeyBinding) MC.options.jumpKey;
		((KeyBinding) jumpKey).setPressed(jumpKey.isActallyPressed());
	}
}
