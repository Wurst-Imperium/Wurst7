/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util.world;

import net.minecraft.block.Block;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.wurstclient.hack.BlockMatchHack;
import net.wurstclient.util.BlockVertexCompiler;
import net.wurstclient.util.ChunkSearcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Queue;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class BlockMatchingChunk
{
    private final BlockMatchingWorld matchingWorld;
    private final int cx;
    private final int cz;

    private final ChunkSearcher searcher;

    private final BoolChunk matchChunk;

    private final AtomicBoolean hasSearchTask = new AtomicBoolean(false);
    private final AtomicBoolean runSearch = new AtomicBoolean(false);
    private Future<Void> searchTask;

    private final VertexBuffer vertexBuffer = new VertexBuffer();

    private Future<Void> vertexCompileTask;

    private final Box boundingBox;

    BlockMatchingChunk(BlockMatchingWorld matchingWorld, Chunk chunk, Supplier<BoolChunk> matchChunkSupplier, Predicate<Block> blockMatcher)
    {
        this.matchingWorld = matchingWorld;
        this.matchChunk = matchChunkSupplier.get();
        this.searcher = new ChunkSearcher(chunk, blockMatcher, this.matchChunk);
        this.cx = chunk.getPos().x;
        this.cz = chunk.getPos().z;
        int bx = cx << 4;
        int bz = cz << 4;
        this.boundingBox = new Box(bx, 0, bz, bx + 16.0, chunk.getHeight(), bz + 16.0);
    }

    private void queueVertexUpdate(BlockMatchHack.MatchingExecutorService pool,
         @NotNull Queue<Pair<VertexBuffer, BufferBuilder>> bufferUploadQueue)
    {
        if(vertexCompileTask != null)
            vertexCompileTask.cancel(false);
        vertexCompileTask = pool.submitVertexCompile(() ->
        {
            bufferUploadQueue.add(new Pair<>(vertexBuffer, BlockVertexCompiler.compileVertices(matchingWorld, cx, cz)));
            vertexCompileTask = null;
        });
    }

    public void queueUpdate(BlockMatchHack.MatchingExecutorService pool, @Nullable Queue<Pair<VertexBuffer, BufferBuilder>> bufferUploadQueue)
    {
        runSearch.set(true); // Asks for a search, or re-search if task is already running
        if(!hasSearchTask.getAndSet(true))
            searchTask = pool.submitSearch(() ->
            {
                while(runSearch.getAndSet(false))
                {
                    searcher.search();
                    if(bufferUploadQueue != null && !runSearch.get() && !matchChunk.isEmpty())
                    {
                        queueVertexUpdate(pool, bufferUploadQueue);
                        // Queue updates for chunks around it as new matching info becomes available,
                        // potentially changing the face count on chunk egdes.
                        BlockMatchingChunk neighbor = matchingWorld.getChunk(ChunkPos.toLong(cx - 1, cz));
                        if(neighbor != null && !neighbor.getMatchChunk().isEmpty())
                            neighbor.queueVertexUpdate(pool, bufferUploadQueue);
                        neighbor = matchingWorld.getChunk(ChunkPos.toLong(cx + 1, cz));
                        if(neighbor != null && !neighbor.getMatchChunk().isEmpty())
                            neighbor.queueVertexUpdate(pool, bufferUploadQueue);
                        neighbor = matchingWorld.getChunk(ChunkPos.toLong(cx, cz - 1));
                        if(neighbor != null && !neighbor.getMatchChunk().isEmpty())
                            neighbor.queueVertexUpdate(pool, bufferUploadQueue);
                        neighbor = matchingWorld.getChunk(ChunkPos.toLong(cx, cz + 1));
                        if(neighbor != null && !neighbor.getMatchChunk().isEmpty())
                            neighbor.queueVertexUpdate(pool, bufferUploadQueue);
                    }
                }
                hasSearchTask.set(false);
            });
    }

    public BoolChunk getMatchChunk()
    {
        return matchChunk;
    }

    public VertexBuffer getVertexBuffer()
    {
        return vertexBuffer;
    }

    public Box getBoundingBox()
    {
        return boundingBox;
    }

    public void close()
    {
        searcher.interrupt();
        searchTask.cancel(false);
        vertexBuffer.close();
    }
}
