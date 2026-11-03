/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings;

import java.util.function.Function;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.WurstClient;
import net.wurstclient.hack.Hack;
import net.wurstclient.util.text.WText;

public final class AimAtSetting extends EnumSetting<AimAtSetting.AimAt>
{
	private static final WText FULL_DESCRIPTION_SUFFIX =
		buildDescriptionSuffix();
	
	public AimAtSetting(WText description, AimAt selected)
	{
		super("Aim at", description.append(FULL_DESCRIPTION_SUFFIX),
			AimAt.values(), selected);
	}
	
	public AimAtSetting(Hack hack, AimAt selected)
	{
		this(hackDescription(hack), selected);
	}
	
	public AimAtSetting(Hack hack)
	{
		this(hack, AimAt.AUTO);
	}
	
	private static WText hackDescription(Hack hack)
	{
		return WText.translated("description.wurst.setting."
			+ hack.getName().toLowerCase() + ".aim_at");
	}
	
	public Vec3 getAimPoint(Entity e)
	{
		return getSelected().aimFunction.apply(e);
	}
	
	private static WText buildDescriptionSuffix()
	{
		WText text = WText.literal("\n\n");
		
		for(AimAt value : AimAt.values())
			text.append("\u00a7l" + value.name + "\u00a7r - ")
				.append(value.description).append("\n\n");
		
		return text;
	}
	
	private static Vec3 aimAtClosestPoint(Entity e)
	{
		AABB box = e.getBoundingBox();
		Vec3 eyes = WurstClient.MC.player.getEyePosition();
		
		if(box.contains(eyes))
			return eyes;
		
		double clampedX = Math.clamp(eyes.x, box.minX, box.maxX);
		double clampedY = Math.clamp(eyes.y, box.minY, box.maxY);
		double clampedZ = Math.clamp(eyes.z, box.minZ, box.maxZ);
		
		return new Vec3(clampedX, clampedY, clampedZ);
	}
	
	private static Vec3 aimAtHead(Entity e)
	{
		float eyeHeight = e.getEyeHeight(e.getPose());
		return e.position().add(0, eyeHeight, 0);
	}
	
	private static Vec3 aimAtCenter(Entity e)
	{
		return e.getBoundingBox().getCenter();
	}
	
	private static Vec3 aimAtFeet(Entity e)
	{
		return e.position().add(0, 0.001, 0);
	}
	
	public enum AimAt
	{
		AUTO("Auto", AimAtSetting::aimAtClosestPoint),
		
		HEAD("Head", AimAtSetting::aimAtHead),
		
		CENTER("Center", AimAtSetting::aimAtCenter),
		
		FEET("Feet", AimAtSetting::aimAtFeet);
		
		private static final String TRANSLATION_KEY_PREFIX =
			"description.wurst.setting.generic.aim_at.";
		
		private final String name;
		private final WText description;
		private final Function<Entity, Vec3> aimFunction;
		
		private AimAt(String name, Function<Entity, Vec3> aimFunction)
		{
			this.name = name;
			description =
				WText.translated(TRANSLATION_KEY_PREFIX + name().toLowerCase());
			this.aimFunction = aimFunction;
		}
		
		public Vec3 getAimPoint(Entity e)
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
