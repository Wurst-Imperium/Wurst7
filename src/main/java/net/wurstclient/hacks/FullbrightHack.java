/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.option.GameOptions;
import net.minecraft.util.math.MathHelper;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

@SearchTags({"Fullbrightness", "full brightness", "Fulbrightness",
	"ful brightness", "NightVision", "night vision", "FullLightness",
	"FulLightness", "full lightness", "FullGamma", "full gamma"})
public final class FullbrightHack extends Hack implements UpdateListener
{
	private final EnumSetting<Method> method = new EnumSetting<>("Method",
		"§l伽马§r使亮度达到最大,与光影不兼容\n§l夜晚模式§r利用夜视效果工作,\n§o通常§r与光影一起工作",
		Method.values(), Method.GAMMA);
	
	private final CheckboxSetting fade = new CheckboxSetting("淡入淡出",
		"使你开启Fullbright时,\n画面逐渐变亮,\n而不是突然变亮,\n关闭Fullbright时逐渐变暗,\n而不是突然变暗", true);
	
	private final SliderSetting defaultGamma =
		new SliderSetting("默认亮度",
			"当你关闭Fullbright时,\n将亮度设为这个值\n注:这段描述来自直接翻译",
			0.5, 0, 1, 0.01, ValueDisplay.PERCENTAGE);
	
	private boolean wasGammaChanged;
	private float nightVisionStrength;
	
	public FullbrightHack()
	{
		super("Fullbright", "让你在黑暗中看得见");
		setCategory(Category.RENDER);
		addSetting(method);
		addSetting(fade);
		addSetting(defaultGamma);
		
		checkGammaOnStartup();
		EVENTS.add(UpdateListener.class, this);
	}
	
	private void checkGammaOnStartup()
	{
		EVENTS.add(UpdateListener.class, new UpdateListener()
		{
			@Override
			public void onUpdate()
			{
				double gamma = MC.options.gamma;
				System.out.println("Brightness started at " + gamma);
				
				if(gamma > 1)
					wasGammaChanged = true;
				else
					defaultGamma.setValue(gamma);
				
				EVENTS.remove(UpdateListener.class, this);
			}
		});
	}
	
	@Override
	public void onUpdate()
	{
		updateGamma();
		updateNightVision();
	}
	
	private void updateGamma()
	{
		boolean shouldChangeGamma =
			isEnabled() && method.getSelected() == Method.GAMMA;
		
		if(shouldChangeGamma)
		{
			setGamma(16);
			return;
		}
		
		if(wasGammaChanged)
			resetGamma(defaultGamma.getValue());
	}
	
	private void setGamma(double target)
	{
		wasGammaChanged = true;
		GameOptions options = MC.options;
		
		if(!fade.isChecked() || Math.abs(options.gamma - target) <= 0.5)
		{
			options.gamma = target;
			return;
		}
		
		if(options.gamma < target)
			options.gamma += 0.5;
		else
			options.gamma -= 0.5;
	}
	
	private void resetGamma(double target)
	{
		GameOptions options = MC.options;
		
		if(!fade.isChecked() || Math.abs(options.gamma - target) <= 0.5)
		{
			options.gamma = target;
			wasGammaChanged = false;
			return;
		}
		
		if(options.gamma < target)
			options.gamma += 0.5;
		else
			options.gamma -= 0.5;
	}
	
	private void updateNightVision()
	{
		boolean shouldGiveNightVision =
			isEnabled() && method.getSelected() == Method.NIGHT_VISION;
		
		if(fade.isChecked())
		{
			if(shouldGiveNightVision)
				nightVisionStrength += 0.03125;
			else
				nightVisionStrength -= 0.03125;
			
			nightVisionStrength = MathHelper.clamp(nightVisionStrength, 0, 1);
			
		}else if(shouldGiveNightVision)
			nightVisionStrength = 1;
		else
			nightVisionStrength = 0;
	}
	
	public boolean isNightVisionActive()
	{
		return nightVisionStrength > 0;
	}
	
	public float getNightVisionStrength()
	{
		return nightVisionStrength;
	}
	
	private static enum Method
	{
		GAMMA("Gamma"),
		NIGHT_VISION("Night Vision");
		
		private final String name;
		
		private Method(String name)
		{
			this.name = name;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}
