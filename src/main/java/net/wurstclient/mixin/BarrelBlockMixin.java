/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */

package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.block.BarrelBlock;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

// Implements getTicker for barrels so that it can be seen by ChestESP
@Mixin(BarrelBlock.class)
public abstract class BarrelBlockMixin implements BlockEntityProvider
{
    // This takes a BlockEntity but it will receive a BarrelBlockEntity
    private static void clientTick(World world, BlockPos pos, BlockState state, BlockEntity blockEntity)
    {
        // Does absolutely nothing
        // A normal chest would animate here.
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type)
    {
        // This imitates other Ticker functions and manually inlines `checkType`
        if (world.isClient && type == BlockEntityType.BARREL)
            return BarrelBlockMixin::clientTick;
        else
            return null;
    }
}
