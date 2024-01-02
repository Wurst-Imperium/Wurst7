/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings;

import net.wurstclient.WurstClient;

public final class AttackSpeedSliderSetting extends SliderSetting
{
	private int tickTimer;
	
	public AttackSpeedSliderSetting()
	{
		this("Speed", "description.wurst.setting.generic.attack_speed");
	}
	
	public AttackSpeedSliderSetting(String name, String description)
	{
		super(name, description, 0, 0, 20, 0.1,
			ValueDisplay.DECIMAL.withLabel(0, "auto"));
	}
	
	@Override
	public float[] getKnobColor()
	{
		if(getValue() == 0)
			return new float[]{0, 0.5F, 1};
		
		return super.getKnobColor();
	}
	
	public void resetTimer()
	{
		tickTimer = 0;
	}
	
	public void updateTimer()
	{
		tickTimer += 50;
	}
	
	public boolean isTimeToAttack()
	{
		if(getValue() > 0)
			return tickTimer >= 1000 / getValue();
		
		return WurstClient.MC.player.getAttackCooldownProgress(0) >= 1;
	}
}
