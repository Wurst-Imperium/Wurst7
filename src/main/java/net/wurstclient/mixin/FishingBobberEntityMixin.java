/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.util.math.BlockPos;
import net.wurstclient.mixinterface.IFishingBobberEntity;

@Mixin(FishingBobberEntity.class)
public abstract class FishingBobberEntityMixin implements IFishingBobberEntity
{
	@Override
	public boolean checkOpenWaterAround(BlockPos pos)
	{
		return isOpenOrWaterAround(pos);
	}
	
	@Shadow
	private boolean isOpenOrWaterAround(BlockPos pos)
	{
		return false;
	}
}
