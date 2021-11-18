/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util.world;

import org.jetbrains.annotations.Contract;

/**
 * Specialization of a {@link BoolView} to represent a single Minecraft
 * {@link net.minecraft.world.chunk.Chunk}'s worth of boolean values.
 */
public interface BoolChunk extends BoolView
{
    int WIDTH = 16;
    int HEIGHT = 256;
    int LENGTH = 16;

    /**
     * @return `true` if not bits are set in this chunk.
     */
    @Contract(pure = true)
    boolean isEmpty();

    /**
     * Get an estimation of how many bits are set.
     * @return A maximum boundary of bits set in this chunk.
     */
    @Contract(pure = true)
    int getBitsSetAtLeastOnce();
}
