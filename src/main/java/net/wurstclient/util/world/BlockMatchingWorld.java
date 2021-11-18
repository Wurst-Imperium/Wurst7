/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util.world;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class BlockMatchingWorld implements BoolView {
    private final Supplier<BoolChunk> matchChunkSupplier;
    private final Map<Long, BlockMatchingChunk> blockMatchingChunks =
        new Long2ObjectOpenHashMap<>();

    public BlockMatchingWorld(Supplier<BoolChunk> matchChunkSupplier)
    {
        this.matchChunkSupplier = matchChunkSupplier;
    }

    @Contract(pure = true)
    private static long blockToChunkPos(int x, int z)
    {
        return ChunkPos.toLong(x >> 4, z >> 4);
    }

    public void clear()
    {
        for(BlockMatchingChunk matchingChunk : blockMatchingChunks.values())
            matchingChunk.close();
        blockMatchingChunks.clear();
    }

    @Override
    public boolean get(int x, int y, int z)
    {
        BlockMatchingChunk chunk = blockMatchingChunks.get(blockToChunkPos(x, z));
        if(chunk == null)
            return false;
        return chunk.getMatchChunk().get(x, y, z);
    }

    @Override
    public void set(int x, int y, int z)
    {
        BlockMatchingChunk chunk = blockMatchingChunks.get(blockToChunkPos(x, z));
        if(chunk == null)
            throw new IllegalArgumentException("Setting bit in absent chunk");
        chunk.getMatchChunk().set(x, y, z);
    }

    @Override
    public void unset(int x, int y, int z)
    {
        BlockMatchingChunk chunk = blockMatchingChunks.get(blockToChunkPos(x, z));
        if(chunk == null)
            throw new IllegalArgumentException("Unsetting bit in absent chunk");
        chunk.getMatchChunk().unset(x, y, z);
    }

    public boolean hasChunk(long chunkPos)
    {
        return blockMatchingChunks.containsKey(chunkPos);
    }

    public @NotNull BlockMatchingChunk addChunk(Chunk chunk, Predicate<Block> blockMatcher)
    {
        BlockMatchingChunk matching = new BlockMatchingChunk(this, chunk, matchChunkSupplier, blockMatcher);
        blockMatchingChunks.put(chunk.getPos().toLong(), matching);
        return matching;
    }

    public @Nullable BlockMatchingChunk getChunk(long chunkPos)
    {
        return blockMatchingChunks.get(chunkPos);
    }

    public void removeChunk(long chunkPos)
    {
        blockMatchingChunks.get(chunkPos).close();
        blockMatchingChunks.remove(chunkPos);
    }

    public Set<Map.Entry<Long, BlockMatchingChunk>> chunkEntries()
    {
        return blockMatchingChunks.entrySet();
    }

    public Collection<BlockMatchingChunk> chunks()
    {
        return blockMatchingChunks.values();
    }
}
