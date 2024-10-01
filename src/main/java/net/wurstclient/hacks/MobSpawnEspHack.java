/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.awt.Color;
import java.util.Map.Entry;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.block.BlockState;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.LightType;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.PacketInputListener;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.ChunkAreaSetting;
import net.wurstclient.settings.ChunkAreaSetting.ChunkArea;
import net.wurstclient.settings.ColorSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.RegionPos;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.chunk.ChunkSearcher;
import net.wurstclient.util.chunk.ChunkSearcher.Result;
import net.wurstclient.util.chunk.ChunkVertexBufferCoordinator;

@SearchTags({"mob spawn esp", "LightLevelESP", "light level esp",
	"LightLevelOverlay", "light level overlay"})
public final class MobSpawnEspHack extends Hack
	implements UpdateListener, RenderListener
{
	private final ChunkAreaSetting drawDistance =
		new ChunkAreaSetting("Draw distance", "", ChunkArea.A9);
	
	private final ColorSetting nightColor = new ColorSetting("Night color",
		"description.wurst.setting.mobspawnesp.night_color", Color.YELLOW);
	
	private final ColorSetting dayColor = new ColorSetting("Day color",
		"description.wurst.setting.mobspawnesp.day_color", Color.RED);
	
	private final SliderSetting opacity =
		new SliderSetting("Opacity", 0.5, 0, 1, 0.01, ValueDisplay.PERCENTAGE);
	
	private final CheckboxSetting depthTest =
		new CheckboxSetting("Depth test", true);
	
	private final ChunkVertexBufferCoordinator coordinator =
		new ChunkVertexBufferCoordinator(this::isSpawnable, this::buildBuffer,
			drawDistance);
	
	private int cachedDayColor;
	private int cachedNightColor;
	
	public MobSpawnEspHack()
	{
		super("MobSpawnESP");
		setCategory(Category.RENDER);
		addSetting(drawDistance);
		addSetting(nightColor);
		addSetting(dayColor);
		addSetting(opacity);
		addSetting(depthTest);
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(PacketInputListener.class, coordinator);
		EVENTS.add(RenderListener.class, this);
		
		cachedDayColor = dayColor.getColorI();
		cachedNightColor = nightColor.getColorI();
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(PacketInputListener.class, coordinator);
		EVENTS.remove(RenderListener.class, this);
		
		coordinator.reset();
	}
	
	@Override
	public void onUpdate()
	{
		if(dayColor.getColorI() != cachedDayColor
			|| nightColor.getColorI() != cachedNightColor)
		{
			cachedDayColor = dayColor.getColorI();
			cachedNightColor = nightColor.getColorI();
			coordinator.reset();
		}
		
		coordinator.update();
	}
	
	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		// GL settings
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		
		boolean depthTest = this.depthTest.isChecked();
		if(!depthTest)
			GL11.glDisable(GL11.GL_DEPTH_TEST);
		
		RenderSystem.setShader(GameRenderer::getPositionColorProgram);
		RenderSystem.setShaderColor(1, 1, 1, opacity.getValueF());
		
		for(Entry<ChunkPos, VertexBuffer> entry : coordinator.getBuffers())
		{
			RegionPos region = RegionPos.of(entry.getKey());
			VertexBuffer vertexBuffer = entry.getValue();
			
			matrixStack.push();
			RenderUtils.applyRegionalRenderOffset(matrixStack, region);
			
			Matrix4f viewMatrix = matrixStack.peek().getPositionMatrix();
			Matrix4f projMatrix = RenderSystem.getProjectionMatrix();
			ShaderProgram shader = RenderSystem.getShader();
			vertexBuffer.bind();
			vertexBuffer.draw(viewMatrix, projMatrix, shader);
			VertexBuffer.unbind();
			
			matrixStack.pop();
		}
		
		// GL resets
		RenderSystem.setShaderColor(1, 1, 1, 1);
		if(!depthTest)
			GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_BLEND);
	}
	
	private boolean isSpawnable(BlockPos pos, BlockState state)
	{
		// Check for solid blocks, fluids, redstone, prevent_spawning tags, etc.
		// See SpawnLocationTypes.ON_GROUND
		if(!SpawnRestriction.isSpawnPosAllowed(EntityType.CREEPER, MC.world,
			pos))
			return false;
			
		// Check for hitbox collisions
		// (using a creeper because it's shorter than a zombie)
		if(!MC.world.isSpaceEmpty(EntityType.CREEPER
			.getSpawnBox(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5)))
			return false;
		
		// Check block light level
		return MC.world.getLightLevel(LightType.BLOCK, pos) < 1;
	}
	
	private BuiltBuffer buildBuffer(ChunkSearcher searcher,
		Iterable<Result> results)
	{
		RegionPos region = RegionPos.of(searcher.getPos());
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferBuilder = tessellator.begin(
			VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
		
		for(Result result : results)
		{
			if(searcher.isInterrupted())
				return null;
			
			drawCross(bufferBuilder, result.pos(), region);
		}
		
		return bufferBuilder.endNullable();
	}
	
	private void drawCross(BufferBuilder bufferBuilder, BlockPos pos,
		RegionPos region)
	{
		float x1 = pos.getX() - region.x();
		float x2 = x1 + 1;
		float y = pos.getY() + 0.01F;
		float z1 = pos.getZ() - region.z();
		float z2 = z1 + 1;
		
		int color = MC.world.getLightLevel(LightType.SKY, pos) < 8
			? cachedDayColor : cachedNightColor;
		
		bufferBuilder.vertex(x1, y, z1).color(color);
		bufferBuilder.vertex(x2, y, z2).color(color);
		bufferBuilder.vertex(x2, y, z1).color(color);
		bufferBuilder.vertex(x1, y, z2).color(color);
	}
}
