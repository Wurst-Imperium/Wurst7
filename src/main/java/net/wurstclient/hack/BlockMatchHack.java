/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hack;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.Block;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;
import net.wurstclient.events.CameraTransformViewBobbingListener;
import net.wurstclient.events.PacketInputListener;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.util.BufferBuilderStorage;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.RotationUtils;
import net.wurstclient.util.world.BlockMatchingChunk;
import net.wurstclient.util.world.BlockMatchingWorld;
import net.wurstclient.util.world.ArrayBoolChunk;
import net.wurstclient.util.world.SetBoolChunk;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;

public abstract class BlockMatchHack extends Hack
    implements PacketInputListener, CameraTransformViewBobbingListener
{
    protected final EnumSetting<BlockMatchHack.Area> area = new EnumSetting<>("Area",
            "The area around the player to search in.\n"
                    + "Higher values require a faster computer.",
            BlockMatchHack.Area.values(), BlockMatchHack.Area.D11);

    private int lastDimensionId;
    private Frustum frustum;

    protected enum DisplayStyle {
        BLOCKS,
        TRACERS,
        BOTH
    }
    private DisplayStyle displayStyle;
    private Predicate<Block> blockMatcher = b -> false;
    protected BlockMatchingWorld matchingWorld;

    public interface MatchingExecutorService
    {
        Future<Void> submitSearch(Runnable r);
        Future<Void> submitVertexCompile(Runnable r);
    }

    private class SearchWorkerThreadPoolExecutor extends ThreadPoolExecutor
        implements MatchingExecutorService
    {
        class PriorityFutureTask<T> extends FutureTask<T>
            implements Comparable<PriorityFutureTask<T>>
        {
            private final int priority;
            public PriorityFutureTask(Runnable task, int priority)
            {
                super(task, null);
                this.priority = priority;
            }

            @Override
            public int compareTo(PriorityFutureTask other)
            {
                return Integer.compare(this.priority, other.priority);
            }

            @Override
            public String toString() {
                String str = super.toString();
                return "PriorityFutureTask@" +
                    Integer.toHexString(this.hashCode()) +
                    "[Priority " + priority + ", " +
                    str.substring(str.indexOf('[') + 1);
            }
        }

        public SearchWorkerThreadPoolExecutor()
        {
            super(
                1, Runtime.getRuntime().availableProcessors(),
                15, TimeUnit.SECONDS,
                new PriorityBlockingQueue<>(),
                runnable -> {
                    final Thread thread = Executors.defaultThreadFactory().newThread(runnable);
                    thread.setName("Wurst-" + BlockMatchHack.this.getClass().getSimpleName() + "Worker-" + thread.getId());
                    return thread;
                }
            );
        }

        @Override
        protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public Future<Void> submitSearch(Runnable r) {
            RunnableFuture<Void> ftask = new PriorityFutureTask<>(r, 0);
            this.execute(ftask);
            return ftask;
        }

        @Override
        public Future<Void> submitVertexCompile(Runnable r) {
            RunnableFuture<Void> ftask = new PriorityFutureTask<>(r, 1);
            this.execute(ftask);
            return ftask;
        }
    }
    protected SearchWorkerThreadPoolExecutor workerPool;
    protected @Nullable Queue<Pair<VertexBuffer, BufferBuilder>> bufferUploadQueue;

    protected BlockMatchHack(String name)
    {
        super(name);
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(e -> frustum = e.frustum());
        addSetting(area);
        setDisplayStyle(DisplayStyle.BLOCKS);
    }

    protected void setDisplayStyle(DisplayStyle style)
    {
        displayStyle = style;
        matchingWorld = style == DisplayStyle.BLOCKS ? new BlockMatchingWorld(ArrayBoolChunk::new) : new BlockMatchingWorld(SetBoolChunk::new);
        bufferUploadQueue = style == DisplayStyle.TRACERS ? null : new LinkedList<>();
    }

    @Override
    public void onEnable()
    {
        workerPool = new SearchWorkerThreadPoolExecutor();

        EVENTS.add(PacketInputListener.class, this);
        EVENTS.add(CameraTransformViewBobbingListener.class, this);
    }

    public void onDisable()
    {
        EVENTS.remove(CameraTransformViewBobbingListener.class, this);
        EVENTS.remove(PacketInputListener.class, this);

        matchingWorld.clear();
        workerPool.shutdownNow();
    }

    @Override
    public void onReceivedPacket(PacketInputEvent event)
    {
        ClientPlayerEntity player = MC.player;
        ClientWorld world = MC.world;
        if(player == null || world == null)
            return;

        Packet<?> packet = event.getPacket();
        Chunk chunk;

        if(packet instanceof BlockUpdateS2CPacket)
        {
            BlockUpdateS2CPacket change = (BlockUpdateS2CPacket)packet;
            BlockPos pos = change.getPos();
            chunk = world.getChunk(pos);

        }else if(packet instanceof ChunkDeltaUpdateS2CPacket)
        {
            ChunkDeltaUpdateS2CPacket change =
                    (ChunkDeltaUpdateS2CPacket)packet;

            ArrayList<BlockPos> changedBlocks = new ArrayList<>();
            change.visitUpdates((pos, state) -> changedBlocks.add(pos));
            if(changedBlocks.isEmpty())
                return;

            chunk = world.getChunk(changedBlocks.get(0));

        }else if(packet instanceof ChunkDataS2CPacket)
        {
            ChunkDataS2CPacket chunkData = (ChunkDataS2CPacket)packet;
            chunk = world.getChunk(chunkData.getX(), chunkData.getZ());

        }else
            return;

        BlockMatchingChunk matchingChunk = matchingWorld.getChunk(chunk.getPos().toLong());
        if(matchingChunk != null)
            matchingChunk.queueUpdate(workerPool, bufferUploadQueue);
    }

    @Override
    public void onCameraTransformViewBobbing(
            CameraTransformViewBobbingEvent event)
    {
        if(displayStyle != DisplayStyle.BLOCKS)
            event.cancel();
    }

    private void addSearchersInRange(ChunkPos center)
    {
        int chunkRange = area.getSelected().getChunkRange();
        for(int x = center.x - chunkRange; x <= center.x + chunkRange; x++)
            for(int z = center.z - chunkRange; z <= center.z + chunkRange; z++)
            {
                Chunk chunk = MC.world.getChunk(x, z);
                if(chunk instanceof EmptyChunk || matchingWorld.hasChunk(chunk.getPos().toLong()))
                    continue;

                matchingWorld.addChunk(chunk, blockMatcher).queueUpdate(workerPool, bufferUploadQueue);
            }
    }

    private void removeSearchersOutOfRange(ChunkPos center)
    {
        int chunkRange = area.getSelected().getChunkRange();
        matchingWorld.chunkEntries().removeIf(matchingChunk -> {
            int cx = ChunkPos.getPackedX(matchingChunk.getKey());
            int cz = ChunkPos.getPackedZ(matchingChunk.getKey());
            if(Math.abs(cx - center.x) > chunkRange
                || Math.abs(cz - center.z) > chunkRange)
            {
                matchingChunk.getValue().close();
                return true;
            }
            return false;
        });
    }

    protected void reset()
    {
        matchingWorld.clear();
    }

    protected void updateSearch()
    {
        int dimensionId = MC.world.getRegistryKey().toString().hashCode();
        if(dimensionId != lastDimensionId)
        {
            lastDimensionId = dimensionId;
            reset();
        }

        BlockPos eyesPos = new BlockPos(RotationUtils.getEyesPos());
        ChunkPos center = new ChunkPos(eyesPos);

        removeSearchersOutOfRange(center);
        addSearchersInRange(center);

        uploadBuffers();
    }

    private void uploadBuffers()
    {
        if(!RenderSystem.isOnRenderThread())
            throw new UnsupportedOperationException(
                "Can't upload buffers on non-main thread");
        if(bufferUploadQueue == null || bufferUploadQueue.isEmpty())
            return;
        while(true)
        {
            Pair<VertexBuffer, BufferBuilder> buf = bufferUploadQueue.poll();
            if(buf == null)
                break;
            // VertexBuffer::upload is synchronous here since we are guaranteed
            // to be executing on the main thread.
            buf.getLeft().upload(buf.getRight());
            BufferBuilderStorage.putBack(buf.getRight());
        }
    }

    protected void setBlockMatcher(Predicate<Block> matcher)
    {
        this.blockMatcher = matcher;
        reset();
    }

    protected void render(MatrixStack matrixStack, float red, float green, float blue, float alpha)
    {
        // GL settings
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableTexture();
        RenderSystem.enableCull();
        RenderSystem.disableDepthTest();

        RenderSystem.setShader(GameRenderer::getPositionShader);
        RenderSystem.setShaderColor(red, green, blue, alpha);

        final Vec3d cam = RenderUtils.getCameraPos();

        if(displayStyle == DisplayStyle.BLOCKS || displayStyle == DisplayStyle.BOTH)
        {
            for(Map.Entry<Long, BlockMatchingChunk> entry : matchingWorld.chunkEntries())
            {
                if(entry.getValue().getMatchChunk().isEmpty() || !frustum.isVisible(entry.getValue().getBoundingBox()))
                    continue;
                entry.getValue().getVertexBuffer().bind();
                int cx = ChunkPos.getPackedX(entry.getKey());
                int cz = ChunkPos.getPackedZ(entry.getKey());
                matrixStack.push();
                matrixStack.translate((cx << 4) - cam.x, -cam.y, (cz << 4) - cam.z);
                entry.getValue().getVertexBuffer().setShader(
                        matrixStack.peek().getModel(),
                        RenderSystem.getProjectionMatrix(),
                        RenderSystem.getShader());
                matrixStack.pop();
            }
        }

        if(displayStyle == DisplayStyle.TRACERS || displayStyle == DisplayStyle.BOTH)
        {
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            matrixStack.push();
            BufferBuilder builder = Tessellator.getInstance().getBuffer();
            builder.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION);

            final Matrix4f matrix = matrixStack.peek().getModel();
            final Vec3d start = RotationUtils.getClientLookVec();
            for(BlockMatchingChunk chunk : matchingWorld.chunks())
            {
                if(!(chunk.getMatchChunk() instanceof SetBoolChunk))
                    throw new UnsupportedOperationException();
                for(Long pos : ((SetBoolChunk)chunk.getMatchChunk()).getBlockPositions())
                {
                    double x = BlockPos.unpackLongX(pos) - cam.x + 0.5;
                    double y = BlockPos.unpackLongY(pos) - cam.y + 0.5;
                    double z = BlockPos.unpackLongZ(pos) - cam.z + 0.5;
                    builder.vertex(matrix, (float)start.x, (float)start.y, (float)start.z).next();
                    builder.vertex(matrix, (float)x, (float)y, (float)z).next();
                }
            }

            builder.end();
            BufferRenderer.draw(builder);
            matrixStack.pop();
            GL11.glDisable(GL11.GL_LINE_SMOOTH);
        }

        // Resets
        RenderSystem.enableDepthTest();
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    protected enum Area
    {
        D3("3x3 chunks", 1),
        D5("5x5 chunks", 2),
        D7("7x7 chunks", 3),
        D9("9x9 chunks", 4),
        D11("11x11 chunks", 5),
        D13("13x13 chunks", 6),
        D15("15x15 chunks", 7),
        D17("17x17 chunks", 8),
        D19("19x19 chunks", 9),
        D21("21x21 chunks", 10),
        D23("23x23 chunks", 11),
        D25("25x25 chunks", 12),
        D27("27x27 chunks", 13),
        D29("29x29 chunks", 14),
        D31("31x31 chunks", 15),
        D33("33x33 chunks", 16);

        private final String name;
        private final int chunkRange;

        Area(String name, int chunkRange)
        {
            this.name = name;
            this.chunkRange = chunkRange;
        }

        public int getChunkRange() {
            return chunkRange;
        }

        @Override
        public String toString()
        {
            return name;
        }
    }
}
