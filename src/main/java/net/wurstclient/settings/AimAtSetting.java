/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings;

import java.util.function.Function;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.util.RotationUtils;

public final class AimAtSetting extends EnumSetting<AimAtSetting.AimAt>
{
	private static final String FULL_DESCRIPTION_SUFFIX =
		buildDescriptionSuffix();
	
	private AimAtSetting(String name, String description, AimAt[] values,
		AimAt selected)
	{
		super(name, description, values, selected);
	}
	
	public AimAtSetting(String name, String description, AimAt selected)
	{
		this(name, description + FULL_DESCRIPTION_SUFFIX, AimAt.values(),
			selected);
	}
	
	public AimAtSetting(String description, AimAt selected)
	{
		this("Aim at", description, selected);
	}
	
	public AimAtSetting(String description)
	{
		this(description, AimAt.AUTO);
	}
	
	public Vec3d getAimPoint(Entity e)
	{
		return getSelected().aimFunction.apply(e);
	}
	
	private static String buildDescriptionSuffix()
	{
		StringBuilder builder = new StringBuilder("\n\n");
		AimAt[] values = AimAt.values();
		
		for(AimAt value : values)
			builder.append("\u00a7l").append(value.name).append("\u00a7r - ")
				.append(value.description).append("\n\n");
		
		return builder.toString();
	}
	
	private static Vec3d aimAtClosestPoint(Entity e)
	{
		Box box = e.getBoundingBox();
		Vec3d eyes = RotationUtils.getEyesPos();
		
		if(box.contains(eyes))
			return eyes;
		
		double clampedX = MathHelper.clamp(eyes.x, box.minX, box.maxX);
		double clampedY = MathHelper.clamp(eyes.y, box.minY, box.maxY);
		double clampedZ = MathHelper.clamp(eyes.z, box.minZ, box.maxZ);
		
		return new Vec3d(clampedX, clampedY, clampedZ);
	}
	
	private static Vec3d aimAtHead(Entity e)
	{
		float eyeHeight = e.getEyeHeight(e.getPose());
		return e.getEntityPos().add(0, eyeHeight, 0);
	}
	
	private static Vec3d aimAtCenter(Entity e)
	{
		return e.getBoundingBox().getCenter();
	}
	
	private static Vec3d aimAtFeet(Entity e)
	{
		return e.getEntityPos().add(0, 0.001, 0);
	}
	
	public enum AimAt
	{
		AUTO("Auto", "Aims at the closest point of the target's hitbox.",
			AimAtSetting::aimAtClosestPoint),
		
		HEAD("Head", "Aims at the target's eye position.",
			AimAtSetting::aimAtHead),
		
		CENTER("Center", "Aims at the center of the target's hitbox.",
			AimAtSetting::aimAtCenter),
		
		FEET("Feet", "Aims at the bottom of the target's hitbox.",
			AimAtSetting::aimAtFeet);
		
		private final String name;
		private final String description;
		private final Function<Entity, Vec3d> aimFunction;
		
		private AimAt(String name, String description,
			Function<Entity, Vec3d> aimFunction)
		{
			this.name = name;
			this.description = description;
			this.aimFunction = aimFunction;
		}
		
		public Vec3d getAimPoint(Entity e)
		{
			return aimFunction.apply(e);
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}
