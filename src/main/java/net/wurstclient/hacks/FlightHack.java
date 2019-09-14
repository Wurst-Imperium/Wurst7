/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import com.mojang.realmsclient.gui.ChatFormatting;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.server.network.packet.PlayerMoveC2SPacket;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.IsPlayerInWaterListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

@SearchTags({"FlyHack", "fly hack", "flying"})
public final class FlightHack extends Hack
	implements UpdateListener, IsPlayerInWaterListener
{
	//TODO: Add timer (ex: cheat engine speed hacks) as var.
	public final SliderSetting speedXZ =
		new SliderSetting("SpeedXZ", 1, 0.05, 5, 0.05, ValueDisplay.DECIMAL);
	public final SliderSetting speedY =
			new SliderSetting("SpeedY", 0.5, 0.05, 5, 0.05, ValueDisplay.DECIMAL);
	public final SliderSetting time =
			new SliderSetting("Time", 1, 1, 50, 1, ValueDisplay.INTEGER);
	private CheckboxSetting zeroY = new CheckboxSetting("Zero Y",
			"Makes it so that the Y stays at 0",
			false);
	private CheckboxSetting spoofGround = new CheckboxSetting("Spoof Ground",
			"Makes it so that you spoof the ground.",
			false);
	private CheckboxSetting zeroXZ = new CheckboxSetting("Zero XZ",
			"Makes it so that the X and Z stays at 0",
			false);
	
	public FlightHack()
	{
		super("Flight",
			"Allows you to you fly.\n\n" + ChatFormatting.RED
				+ ChatFormatting.BOLD + "WARNING:" + ChatFormatting.RESET
				+ " You will take fall damage if you don't use NoFall.");
		setCategory(Category.MOVEMENT);
		addSetting(speedXZ);
		addSetting(speedY);
		addSetting(time);
		addSetting(zeroY);
		addSetting(zeroXZ);
		addSetting(spoofGround);
	}
	
	@Override
	public void onEnable()
	{
		WURST.getEventManager().add(UpdateListener.class, this);
		WURST.getEventManager().add(IsPlayerInWaterListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		WURST.getEventManager().remove(UpdateListener.class, this);
		WURST.getEventManager().remove(IsPlayerInWaterListener.class, this);

	}
	int ticks = 0;
	@Override
	public void onUpdate()
	{
		ClientPlayerEntity player = MC.player;



		ticks++;

		player.abilities.flying = false;
		if(ticks % time.getValueI() == 0) {
			player.field_6281 = speedXZ.getValueF();

			if (MC.options.keyJump.isPressed()){
				player.setVelocity(0, speedY.getValue(), 0);
			}else if(player.getVelocity().y>0 || zeroY.isChecked())
				player.setVelocity(player.getVelocity().x, 0, player.getVelocity().z);

			if (MC.options.keySneak.isPressed()){
				player.setVelocity(0, -speedY.getValue(), 0);
			}
		}else{
			if(player.getVelocity().y>0 || zeroY.isChecked())
				player.setVelocity(player.getVelocity().x, 0, player.getVelocity().z);
			if(zeroXZ.isChecked())
				player.setVelocity(0, player.getVelocity().y, 0);
		}
		if(spoofGround.isChecked())
			player.networkHandler.sendPacket(new PlayerMoveC2SPacket(true));
	}
	
	@Override
	public void onIsPlayerInWater(IsPlayerInWaterEvent event)
	{
		event.setInWater(false);
	}
}
