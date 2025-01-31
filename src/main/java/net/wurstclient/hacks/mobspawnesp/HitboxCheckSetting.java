/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.mobspawnesp;

import java.util.function.Function;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.BlockPos;
import net.wurstclient.WurstClient;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.util.text.WText;

public final class HitboxCheckSetting
	extends EnumSetting<HitboxCheckSetting.HitboxCheck>
{
	private static final MinecraftClient MC = WurstClient.MC;
	private static final WText DESCRIPTION =
		WText.translated("description.wurst.setting.mobspawnesp.hitbox_check")
			.append(buildDescriptionSuffix());
	
	public HitboxCheckSetting()
	{
		super("Hitbox check", DESCRIPTION, HitboxCheck.values(),
			HitboxCheck.OFF);
	}
	
	public boolean isSpaceEmpty(BlockPos pos)
	{
		return getSelected().check.apply(pos);
	}
	
	private static synchronized boolean slowHitboxCheck(BlockPos pos)
	{
		return unstableHitboxCheck(pos);
	}
	
	// "unstable" because isSpaceEmpty() is not thread-safe
	private static boolean unstableHitboxCheck(BlockPos pos)
	{
		return MC.world.isSpaceEmpty(EntityType.CREEPER
			.getSpawnBox(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5));
	}
	
	private static WText buildDescriptionSuffix()
	{
		WText text = WText.literal("\n\n");
		HitboxCheck[] values = HitboxCheck.values();
		
		for(HitboxCheck value : values)
			text.append("\u00a7l" + value.name + "\u00a7r - ")
				.append(value.description).append("\n\n");
		
		return text;
	}
	
	public enum HitboxCheck
	{
		OFF("Off", pos -> true),
		SLOW("Slow", HitboxCheckSetting::slowHitboxCheck),
		UNSTABLE("Unstable", HitboxCheckSetting::unstableHitboxCheck);
		
		private static final String TRANSLATION_KEY_PREFIX =
			"description.wurst.setting.mobspawnesp.hitbox_check.";
		
		private final String name;
		private final WText description;
		private final Function<BlockPos, Boolean> check;
		
		private HitboxCheck(String name, Function<BlockPos, Boolean> check)
		{
			this.name = name;
			description =
				WText.translated(TRANSLATION_KEY_PREFIX + name().toLowerCase());
			this.check = check;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}
