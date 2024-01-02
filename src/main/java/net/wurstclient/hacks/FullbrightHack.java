/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.option.SimpleOption;
import net.minecraft.util.math.MathHelper;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.mixinterface.ISimpleOption;
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
		"\u00a7lGamma\u00a7r works by setting your brightness slider beyond 100%. Incompatible with shader packs.\n\n"
			+ "\u00a7lNight Vision\u00a7r works by applying the night vision effect. This \u00a7ousually\u00a7r works with shader packs.",
		Method.values(), Method.GAMMA);
	
	private final CheckboxSetting fade = new CheckboxSetting("Fade",
		"Slowly fades between brightness and darkness.", true);
	
	private final SliderSetting defaultGamma = new SliderSetting(
		"Default brightness",
		"Fullbright will set your brightness slider back to this value when you turn it off.",
		0.5, 0, 1, 0.01, ValueDisplay.PERCENTAGE);
	
	private boolean wasGammaChanged;
	private float nightVisionStrength;
	
	public FullbrightHack()
	{
		super("Fullbright");
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
				double gamma = MC.options.getGamma().getValue();
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
		if(isChangingGamma())
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
		
		SimpleOption<Double> gammaOption = MC.options.getGamma();
		ISimpleOption<Double> gammaOption2 = ISimpleOption.get(gammaOption);
		double oldGammaValue = gammaOption.getValue();
		
		if(!fade.isChecked() || Math.abs(oldGammaValue - target) <= 0.5)
		{
			gammaOption2.forceSetValue(target);
			return;
		}
		
		if(oldGammaValue < target)
			gammaOption2.forceSetValue(oldGammaValue + 0.5);
		else
			gammaOption2.forceSetValue(oldGammaValue - 0.5);
	}
	
	private void resetGamma(double target)
	{
		SimpleOption<Double> gammaOption = MC.options.getGamma();
		ISimpleOption<Double> gammaOption2 = ISimpleOption.get(gammaOption);
		double oldGammaValue = gammaOption.getValue();
		
		if(!fade.isChecked() || Math.abs(oldGammaValue - target) <= 0.5)
		{
			gammaOption2.forceSetValue(target);
			wasGammaChanged = false;
			return;
		}
		
		if(oldGammaValue < target)
			gammaOption2.forceSetValue(oldGammaValue + 0.5);
		else
			gammaOption2.forceSetValue(oldGammaValue - 0.5);
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
	
	public boolean isChangingGamma()
	{
		return isEnabled() && method.getSelected() == Method.GAMMA;
	}
	
	/**
	 * Returns the value of Fullbright's "Default brightness" slider. Used by
	 * {@link XRayHack} to restore the gamma value when X-Ray is turned off.
	 */
	public double getDefaultGamma()
	{
		return defaultGamma.getValue();
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
	
	// See ClientPlayerEntityMixin.hasStatusEffect()
}
