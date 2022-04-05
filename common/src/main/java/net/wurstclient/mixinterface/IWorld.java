/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixinterface;

import java.util.List;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.chunk.BlockEntityTickInvoker;

public interface IWorld
{
	public List<BlockEntityTickInvoker> getBlockEntityTickers();
	
	public Stream<VoxelShape> getBlockCollisionsStream(@Nullable Entity entity,
		Box box);
}
