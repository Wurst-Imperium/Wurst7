/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.animal.dolphin.Dolphin;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.animal.fish.Pufferfish;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
import net.minecraft.world.entity.animal.panda.Panda;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.cubemob.MagmaCube;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.entity.monster.piglin.Piglin;

public enum MobDisposition
{
	PASSIVE,
	NEUTRAL,
	HOSTILE;
	
	public static MobDisposition of(Mob mob)
	{
		if(mob instanceof IronGolem golem)
			return golem.isPlayerCreated() ? PASSIVE : NEUTRAL;
		
		if(mob instanceof Panda panda)
			return panda.getVariant() == Panda.Gene.AGGRESSIVE ? NEUTRAL
				: PASSIVE;
		
		if(mob instanceof Piglin piglin)
			return piglin.isBaby() ? PASSIVE : NEUTRAL;
		
		if(mob instanceof Rabbit rabbit)
			return rabbit.getVariant() == Rabbit.Variant.EVIL ? HOSTILE
				: PASSIVE;
		
		if(mob instanceof Slime slime && !(mob instanceof MagmaCube))
			return slime.isTiny() ? PASSIVE : HOSTILE;
		
		// Minor retaliation does not make these a credible combat threat.
		if(mob instanceof Dolphin || mob instanceof Llama)
			return PASSIVE;
		
		if(mob instanceof Pufferfish || mob instanceof Goat
			|| mob instanceof AbstractNautilus || mob instanceof NeutralMob)
			return NEUTRAL;
		
		return mob instanceof Enemy ? HOSTILE : PASSIVE;
	}
}
