/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util.world;

import net.minecraft.util.math.BlockPos;
import net.wurstclient.util.ChunkSearcher;
import org.jetbrains.annotations.Contract;

/**
 * Interface to a 3D grid of boolean values.
 * Used for block-matching hacks' result sets.
 */
public interface BoolView extends ChunkSearcher.MatchContainer
{
    @Contract(mutates = "this")
    void clear();

    @Contract(pure = true)
    boolean get(int x, int y, int z);

    @Contract(pure = true)
    default boolean get(long blockPos)
    {
        return get(
                BlockPos.unpackLongX(blockPos),
                BlockPos.unpackLongY(blockPos),
                BlockPos.unpackLongZ(blockPos));
    }

    @Contract(mutates = "this")
    void set(int x, int y, int z);

    @Contract(mutates = "this")
    default void set(long blockPos)
    {
        set(
                BlockPos.unpackLongX(blockPos),
                BlockPos.unpackLongY(blockPos),
                BlockPos.unpackLongZ(blockPos));
    }

    @Contract(mutates = "this")
    void unset(int x, int y, int z);

    @Contract(mutates = "this")
    default void unset(long blockPos)
    {
        set(
                BlockPos.unpackLongX(blockPos),
                BlockPos.unpackLongY(blockPos),
                BlockPos.unpackLongZ(blockPos));
    }

    @Contract(mutates = "this")
    default void set(int x, int y, int z, boolean value)
    {
        if(value)
        {
            set(x, y, z);
        }else
        {
            unset(x, y, z);
        }
    }

    @Contract(mutates = "this")
    default void set(long blockPos, boolean value)
    {
        set(
                BlockPos.unpackLongX(blockPos),
                BlockPos.unpackLongY(blockPos),
                BlockPos.unpackLongZ(blockPos),
                value);
    }
}
