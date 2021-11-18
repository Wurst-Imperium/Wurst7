/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util.world;

import it.unimi.dsi.fastutil.longs.LongArraySet;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Contract;

import java.util.Set;

/**
 * Implementation of a {@link BoolChunk} that stores the matched block
 * positions in a set.
 * This is the best structure for quickly iterating over a sparse set of
 * matched blocks (e.g. finding diamonds), but it isn't space efficient as it
 * stores a 64-bit packed block position for each match; for bulk matching
 * see {@link ArrayBoolChunk}.
 */
public class SetBoolChunk implements BoolChunk
{
    private final Set<Long> values = new LongArraySet();

    public SetBoolChunk()
    {
        clear();
    }

    @Contract(mutates = "this")
    @Override
    public void clear()
    {
        values.clear();
    }

    @Contract(pure = true)
    @Override
    public boolean get(int x, int y, int z)
    {
        return values.contains(BlockPos.asLong(x, y, z));
    }

    @Contract(mutates = "this")
    @Override
    public void set(int x, int y, int z)
    {
        values.add(BlockPos.asLong(x, y, z));
    }

    @Contract(mutates = "this")
    @Override
    public void unset(int x, int y, int z)
    {
        values.remove(BlockPos.asLong(x, y, z));
    }

    @Contract(pure = true)
    @Override
    public int getBitsSetAtLeastOnce()
    {
        return values.size();
    }

    @Contract(pure = true)
    @Override
    public boolean isEmpty()
    {
        return values.isEmpty();
    }

    public Set<Long> getBlockPositions()
    {
        return values;
    }
}
