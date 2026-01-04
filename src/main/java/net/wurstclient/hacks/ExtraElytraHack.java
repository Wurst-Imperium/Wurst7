/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;

@SearchTags({"EasyElytra", "extra elytra", "easy elytra"})
public final class ExtraElytraHack extends Hack implements UpdateListener
{
	private final CheckboxSetting instantFly = new CheckboxSetting(
		"Instant fly", "Jump to fly, no weird double-jump needed!", true);
	
	private final CheckboxSetting speedCtrl = new CheckboxSetting(
		"Speed control", "Control your speed with the Forward and Back keys.\n"
			+ "(default: W and S)\n" + "No fireworks needed!",
		true);
	
	private final CheckboxSetting heightCtrl =
		new CheckboxSetting("Height control",
			"Control your height with the Jump and Sneak keys.\n"
				+ "(default: Spacebar and Shift)\n" + "No fireworks needed!",
			false);
	
	private final CheckboxSetting stopInWater =
		new CheckboxSetting("Stop flying in water", true);
	
	private int jumpTimer;
	
	public ExtraElytraHack()
	{
		super("ExtraElytra");
		setCategory(Category.MOVEMENT);
		addSetting(instantFly);
		addSetting(speedCtrl);
		addSetting(heightCtrl);
		addSetting(stopInWater);
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
		jumpTimer = 0;
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		if(jumpTimer > 0)
			jumpTimer--;
		
		if(!MC.player.canGlide())
			return;
		
		if(MC.player.isFallFlying())
		{
			if(stopInWater.isChecked() && MC.player.isInWater())
			{
				sendStartStopPacket();
				return;
			}
			
			controlSpeed();
			controlHeight();
			return;
		}
		
		if(MC.options.keyJump.isDown())
			doInstantFly();
	}
	
	private void sendStartStopPacket()
	{
		ServerboundPlayerCommandPacket packet =
			new ServerboundPlayerCommandPacket(MC.player,
				ServerboundPlayerCommandPacket.Action.START_FALL_FLYING);
		MC.player.connection.send(packet);
	}
	
	private void controlHeight()
	{
		if(!heightCtrl.isChecked())
			return;
		
		Vec3 v = MC.player.getDeltaMovement();
		
		if(MC.options.keyJump.isDown())
			MC.player.setDeltaMovement(v.x, v.y + 0.08, v.z);
		else if(MC.options.keyShift.isDown())
			MC.player.setDeltaMovement(v.x, v.y - 0.04, v.z);
	}
	
	private void controlSpeed()
	{
		if(!speedCtrl.isChecked())
			return;
		
		float yaw = (float)Math.toRadians(MC.player.getYRot());
		Vec3 forward = new Vec3(-Mth.sin(yaw) * 0.05, 0, Mth.cos(yaw) * 0.05);
		
		Vec3 v = MC.player.getDeltaMovement();
		
		if(MC.options.keyUp.isDown())
			MC.player.setDeltaMovement(v.add(forward));
		else if(MC.options.keyDown.isDown())
			MC.player.setDeltaMovement(v.subtract(forward));
	}
	
	private void doInstantFly()
	{
		if(!instantFly.isChecked())
			return;
		
		if(jumpTimer <= 0)
		{
			jumpTimer = 20;
			MC.player.setJumping(false);
			MC.player.setSprinting(true);
			MC.player.jumpFromGround();
		}
		
		sendStartStopPacket();
	}
}
