/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.freecam;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.util.text.WText;

public final class FreecamInitialPosSetting
	extends EnumSetting<FreecamInitialPosSetting.InitialPosition>
{
	private static final WText DESCRIPTION = buildDescription();
	
	public FreecamInitialPosSetting()
	{
		super("Initial position", DESCRIPTION, InitialPosition.values(),
			InitialPosition.INSIDE);
	}
	
	private static WText buildDescription()
	{
		WText text = WText
			.translated("description.wurst.setting.freecam.initial_position");
		
		for(InitialPosition value : InitialPosition.values())
			text = text
				.append(WText.literal("\n\n\u00a7l" + value.name + ":\u00a7r "))
				.append(value.description);
		
		return text;
	}
	
	public enum InitialPosition
	{
		INSIDE("Inside")
		{
			@Override
			public Vec3 getPos(Entity cameraEntity)
			{
				return cameraEntity.getEyePosition();
			}
		},
		
		IN_FRONT("In Front")
		{
			@Override
			public Vec3 getPos(Entity cameraEntity)
			{
				float yawRad = cameraEntity.getViewYRot(1) * Mth.DEG_TO_RAD;
				double distance = 0.55;
				if(cameraEntity instanceof LivingEntity le)
					distance *= le.getScale();
				double offsetX = -Mth.sin(yawRad) * distance;
				double offsetZ = Mth.cos(yawRad) * distance;
				return cameraEntity.getEyePosition().add(offsetX, 0, offsetZ);
			}
		},
		
		ABOVE("Above")
		{
			@Override
			public Vec3 getPos(Entity cameraEntity)
			{
				double distance = 0.55;
				if(cameraEntity instanceof LivingEntity le)
					distance *= le.getScale();
				return cameraEntity.getEyePosition().add(0, distance, 0);
			}
		};
		
		private static final String TRANSLATION_KEY_PREFIX =
			"description.wurst.setting.freecam.initial_position.";
		
		private final String name;
		private final WText description;
		
		private InitialPosition(String name)
		{
			this.name = name;
			description =
				WText.translated(TRANSLATION_KEY_PREFIX + name().toLowerCase());
		}
		
		public abstract Vec3 getPos(Entity cameraEntity);
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}
