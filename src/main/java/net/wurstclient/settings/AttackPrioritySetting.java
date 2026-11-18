/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings;

import java.util.Comparator;
import java.util.function.Function;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.hack.Hack;
import net.wurstclient.util.EntityUtils;
import net.wurstclient.util.RotationUtils;
import net.wurstclient.util.text.WText;

public final class AttackPrioritySetting
	extends EnumSetting<AttackPrioritySetting.AttackPriority>
{
	private static final WText FULL_DESCRIPTION_SUFFIX =
		buildDescriptionSuffix(true);
	private static final WText REDUCED_DESCRIPTION_SUFFIX =
		buildDescriptionSuffix(false);
	
	private AttackPrioritySetting(WText description, AttackPriority[] values,
		AttackPriority selected)
	{
		super("Priority", description, values, selected);
	}
	
	public AttackPrioritySetting(WText description, AttackPriority selected)
	{
		this(description.append(FULL_DESCRIPTION_SUFFIX),
			AttackPriority.values(), selected);
	}
	
	public AttackPrioritySetting(Hack hack, AttackPriority selected)
	{
		this(hackDescription(hack), selected);
	}
	
	public static AttackPrioritySetting withoutAngleDistOption(
		WText description, AttackPriority selected)
	{
		AttackPriority[] values = {AttackPriority.DISTANCE,
			AttackPriority.ANGLE, AttackPriority.HEALTH};
		return new AttackPrioritySetting(
			description.append(REDUCED_DESCRIPTION_SUFFIX), values, selected);
	}
	
	public static AttackPrioritySetting withoutAngleDistOption(Hack hack,
		AttackPriority selected)
	{
		return withoutAngleDistOption(hackDescription(hack), selected);
	}
	
	public static WText genericDescription()
	{
		return WText
			.translated("description.wurst.setting.generic.attack_priority");
	}
	
	private static WText hackDescription(Hack hack)
	{
		return WText.translated("description.wurst.setting."
			+ hack.getName().toLowerCase() + ".priority");
	}
	
	public Comparator<Entity> getComparator(Function<Entity, Vec3> aimPoint)
	{
		return getSelected().getComparator(aimPoint);
	}
	
	private static WText buildDescriptionSuffix(boolean includeAngleDist)
	{
		WText text = WText.literal("\n\n");
		AttackPriority[] values = includeAngleDist ? AttackPriority.values()
			: new AttackPriority[]{AttackPriority.DISTANCE,
				AttackPriority.ANGLE, AttackPriority.HEALTH};
		
		for(AttackPriority value : values)
			text.append("\u00a7l" + value.name + "\u00a7r - ")
				.append(value.description).append("\n\n");
		
		return text;
	}
	
	public enum AttackPriority
	{
		DISTANCE("Distance"),
		ANGLE("Angle"),
		ANGLE_DIST("Angle+Dist"),
		HEALTH("Health");
		
		private static final String TRANSLATION_KEY_PREFIX =
			"description.wurst.setting.generic.attack_priority.";
		
		private final String name;
		private final WText description;
		
		private AttackPriority(String name)
		{
			this.name = name;
			description =
				WText.translated(TRANSLATION_KEY_PREFIX + name().toLowerCase());
		}
		
		public Comparator<Entity> getComparator(Function<Entity, Vec3> aimPoint)
		{
			return switch(this)
			{
				case DISTANCE -> Comparator
					.comparingDouble(EntityUtils::distanceToHitboxSq);
				
				case ANGLE -> Comparator.comparingDouble(e -> RotationUtils
					.getYawPitchDistanceToLookVec(aimPoint.apply(e)));
				
				case ANGLE_DIST -> Comparator.comparingDouble(e -> Math
					.pow(RotationUtils
						.getYawPitchDistanceToLookVec(aimPoint.apply(e)), 2)
					+ EntityUtils.distanceToHitboxSq(e));
				
				case HEALTH -> Comparator
					.comparingDouble(e -> e instanceof LivingEntity le
						? le.getHealth() : Integer.MAX_VALUE);
			};
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}
