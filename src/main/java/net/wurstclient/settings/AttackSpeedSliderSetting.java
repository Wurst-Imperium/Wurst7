/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings;

import net.minecraft.util.RandomSource;
import net.wurstclient.WurstClient;

public final class AttackSpeedSliderSetting extends SliderSetting
{
	private final RandomSource random =
		RandomSource.createNewThreadLocalInstance();
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
		double value = getValue();
		if(value <= 0)
			tickTimer = -1;
		else
			tickTimer = (int)(1000 / value);
	}
	
	public void resetTimer(double maxRandMS)
	{
		if(maxRandMS <= 0)
		{
			resetTimer();
			return;
		}
		
		double value = getValue();
		double rand = random.nextGaussian();
		int randOffset = (int)(rand * maxRandMS);
		
		if(value <= 0)
			tickTimer = randOffset;
		else
			tickTimer = (int)(1000 / value) + randOffset;
	}
	
	public void updateTimer()
	{
		if(tickTimer >= 0)
			tickTimer -= 50;
	}
	
	public boolean isTimeToAttack()
	{
		double value = getValue();
		if(value <= 0 && WurstClient.MC.player.getAttackStrengthScale(0) < 1)
			return false;
		
		return tickTimer <= 0;
	}
}
