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
import net.wurstclient.mixinterface.IKeyMapping;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

@SearchTags({"EasyElytra", "extra elytra", "easy elytra"})
public final class ExtraElytraHack extends Hack implements UpdateListener
{
	private final CheckboxSetting instantFly = new CheckboxSetting(
		"Instant fly", "Jump to fly, no weird double-jump needed!", true);
	
	private final CheckboxSetting speedCtrl = new CheckboxSetting(
		"Speed control", "Control your speed with the Forward and Back keys.\n"
			+ "(default: W and S)\n" + "No fireworks needed!",
		true);
	private final SliderSetting horizontalVelocity =
		new SliderSetting("Horizontal velocity",
			"description.wurst.setting.extra_elytra.horizontal_velocity", 0.05,
			0.01, 1, 0.01, ValueDisplay.DECIMAL.withSuffix(" blocks/tick"));
	private final CheckboxSetting heightCtrl =
		new CheckboxSetting("Height control",
			"Control your height with the Jump and Sneak keys.\n"
				+ "(default: Spacebar and Shift)\n" + "No fireworks needed!",
			false);
	private final SliderSetting upwardVelocity =
		new SliderSetting("Upward velocity",
			"description.wurst.setting.extra_elytra.upward_velocity", 0.08,
			0.01, 1, 0.01, ValueDisplay.DECIMAL.withSuffix(" blocks/tick"));
	private final SliderSetting downwardVelocity =
		new SliderSetting("Downward velocity",
			"description.wurst.setting.extra_elytra.downward_velocity", 0.04,
			0.01, 1, 0.01, ValueDisplay.DECIMAL.withSuffix(" blocks/tick"));
	private final CheckboxSetting stopInWater =
		new CheckboxSetting("Stop flying in water", true);
	
	private int jumpTimer;
	
	public ExtraElytraHack()
	{
		super("ExtraElytra");
		setCategory(Category.MOVEMENT);
		addSetting(instantFly);
		addSetting(speedCtrl);
		addSetting(horizontalVelocity);
		addSetting(heightCtrl);
		addSetting(upwardVelocity);
		addSetting(downwardVelocity);
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
		
		boolean jump = MC.options.keyJump.isDown();
		boolean sneak = IKeyMapping.get(MC.options.keyShift).isActuallyDown();
		
		// ensure we don't enter sneaking pose
		if(sneak)
			MC.options.keyShift.setDown(false);
		
		if(jump && !sneak)
			MC.player.setDeltaMovement(v.x, v.y + upwardVelocity.getValue(),
				v.z);
		else if(sneak && !jump)
			MC.player.setDeltaMovement(v.x, v.y - downwardVelocity.getValue(),
				v.z);
	}
	
	private void controlSpeed()
	{
		if(!speedCtrl.isChecked())
			return;
		
		float yaw = (float)Math.toRadians(MC.player.getYRot());
		Vec3 forward = new Vec3(-Mth.sin(yaw) * horizontalVelocity.getValue(),
			0, Mth.cos(yaw) * horizontalVelocity.getValue());
		
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
