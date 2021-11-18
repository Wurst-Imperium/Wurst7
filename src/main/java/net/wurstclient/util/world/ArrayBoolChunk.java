/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util.world;

import org.jetbrains.annotations.Contract;

import java.util.Arrays;

/**
 * Implementation of a {@link BoolChunk} that stores the matched block
 * flags into a contiguous array.
 * This is the most space efficient representation, taking only 1 bit per
 * block, and is perfect for bulk matching (e.g. base finding),
 * but it isn't the best for iterating sparse match sets (e.g. finding
 * diamonds); for that see {@link SetBoolChunk}.
 */
public class ArrayBoolChunk implements BoolChunk
{
    // NOTE: this implementation is not yet ready for 1.18's below-zero Y coords

    private final short[] values = new short[HEIGHT * LENGTH];
    private int bitsSetAtLeastOnce;

    public ArrayBoolChunk()
    {
        clear();
    }

    @Contract(mutates = "this")
    @Override
    public void clear()
    {
        Arrays.fill(values, (short)0);
        bitsSetAtLeastOnce = 0;
    }

    @Contract(pure = true)
    public short getRow(int y, int z)
    {
        return values[y + z * HEIGHT];
    }

    @Contract(pure = true)
    @Override
    public boolean get(int x, int y, int z)
    {
        return ((values[y + (z & (LENGTH - 1)) * HEIGHT] >> (x & (WIDTH - 1))) & 1) == 1;
    }

    @Contract(mutates = "this")
    @Override
    public void set(int x, int y, int z)
    {
        values[y + (z & (LENGTH - 1)) * HEIGHT] |= 1 << (x & (WIDTH - 1));
        bitsSetAtLeastOnce++;
    }

    @Contract(mutates = "this")
    @Override
    public void unset(int x, int y, int z)
    {
        values[y + (z & (LENGTH - 1)) * HEIGHT] &= ~(1 << (x & (WIDTH - 1)));
    }

    @Contract(pure = true)
    @Override
    public int getBitsSetAtLeastOnce()
    {
        return bitsSetAtLeastOnce;
    }

    @Contract(pure = true)
    @Override
    public boolean isEmpty()
    {
        if(bitsSetAtLeastOnce == 0)
            return true;
        int sum = 0;
        for (short v : values) {
            sum |= v;
        }
        return sum == 0;
    }
}
