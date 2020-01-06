/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.options.GameOptions;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.EnumSetting;

@SearchTags({"NightVision", "full bright", "brightness", "night vision"})
public final class FullbrightHack extends Hack implements UpdateListener
{
	private final EnumSetting<Method> method = new EnumSetting<>("Method",
		"\u00a7lGamma\u00a7r works by setting the brightness to\n"
			+ "beyond 100%. It supports the \u00a76Fade\u00a7r effect,\n"
			+ "but isn't compatible with shader packs.\n\n"
			+ "\u00a7lNight Vision\u00a7r works by applying the night\n"
			+ "vision effect. This \u00a7ousually\u00a7r works with\n"
			+ "shader packs, but doesn't support the\n"
			+ "\u00a76Fade\u00a7r effect.",
		Method.values(), Method.GAMMA);
	
	private final CheckboxSetting fade = new CheckboxSetting("Fade",
		"Slowly fades between brightness and darkness.\n"
			+ "Only works if \u00a76Method\u00a7r is set to \u00a76Gamma\u00a7r.",
		true);
	
	private boolean hasAppliedNightVision;
	
	public FullbrightHack()
	{
		super("Fullbright", "Allows you to see in the dark.");
		setCategory(Category.RENDER);
		addSetting(method);
		addSetting(fade);
		
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		if(isEnabled() && method.getSelected() == Method.GAMMA)
			approachGamma(16);
		else
			approachGamma(0.5);
		
		if(isEnabled() && method.getSelected() == Method.NIGHT_VISION)
			applyNightVision();
		else
			clearNightVision();
	}
	
	private void approachGamma(double target)
	{
		GameOptions options = MC.options;
		boolean doFade =
			fade.isChecked() && method.getSelected() == Method.GAMMA;
		
		if(!doFade || Math.abs(options.gamma - target) <= 0.5)
		{
			options.gamma = target;
			return;
		}
		
		if(options.gamma < target)
			options.gamma += 0.5;
		else
			options.gamma -= 0.5;
	}
	
	private void applyNightVision()
	{
		MC.player.addStatusEffect(new StatusEffectInstance(
			StatusEffects.NIGHT_VISION, 16360, 0, false, false));
		hasAppliedNightVision = true;
	}
	
	private void clearNightVision()
	{
		if(!hasAppliedNightVision)
			return;
		
		MC.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
		hasAppliedNightVision = false;
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
