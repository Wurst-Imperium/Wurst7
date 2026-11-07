/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings;

import java.util.function.Function;
import java.util.function.Predicate;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.hack.Hack;
import net.wurstclient.util.RotationUtils;
import net.wurstclient.util.text.WText;

public final class FovSliderSetting extends SliderSetting
	implements Predicate<Vec3>
{
	public FovSliderSetting(WText description, double value)
	{
		super("FOV", description, value, 30, 360, 10, ValueDisplay.DEGREES);
	}
	
	public FovSliderSetting(Hack hack, double value)
	{
		this(hackDescription(hack), value);
	}
	
	public FovSliderSetting(Hack hack)
	{
		this(hack, 360);
	}
	
	public static FovSliderSetting genericCombat(double value)
	{
		return new FovSliderSetting(
			WText.translated("description.wurst.setting.generic.fov_combat"),
			value);
	}
	
	private static WText hackDescription(Hack hack)
	{
		return WText.translated("description.wurst.setting."
			+ hack.getName().toLowerCase() + ".fov");
	}
	
	@Override
	public boolean test(Vec3 vec)
	{
		if(getValue() >= 360)
			return true;
		
		return RotationUtils.getAngleToLookVec(vec) <= getValue() / 2;
	}
	
	public Predicate<Entity> asEntityPredicate(Function<Entity, Vec3> converter)
	{
		return e -> test(converter.apply(e));
	}
}
