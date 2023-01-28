/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.projectile.ShulkerBulletEntity;
import net.wurstclient.WurstClient;

public enum EntityUtils
{
	;
	
	protected static final WurstClient WURST = WurstClient.INSTANCE;
	protected static final MinecraftClient MC = WurstClient.MC;
	
	public static Stream<Entity> getAttackableEntities()
	{
		return StreamSupport.stream(MC.world.getEntities().spliterator(), true)
			.filter(IS_ATTACKABLE);
	}
	
	public static Predicate<Entity> IS_ATTACKABLE = e -> e != null
		&& !e.isRemoved()
		&& (e instanceof LivingEntity && ((LivingEntity)e).getHealth() > 0
			|| e instanceof EndCrystalEntity
			|| e instanceof ShulkerBulletEntity)
		&& e != MC.player && !(e instanceof FakePlayerEntity)
		&& !WURST.getFriends().isFriend(e);
}
