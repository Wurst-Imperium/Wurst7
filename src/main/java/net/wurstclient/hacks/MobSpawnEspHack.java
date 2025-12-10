/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.awt.Color;
import java.util.Map.Entry;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.WurstRenderLayers;
import net.wurstclient.events.PacketInputListener;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.hacks.mobspawnesp.HitboxCheckSetting;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.ChunkAreaSetting;
import net.wurstclient.settings.ChunkAreaSetting.ChunkArea;
import net.wurstclient.settings.ColorSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.EasyVertexBuffer;
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
	
	private final HitboxCheckSetting hitboxCheck = new HitboxCheckSetting();
	
	private final ChunkVertexBufferCoordinator coordinator =
		new ChunkVertexBufferCoordinator(this::isSpawnable, Mode.LINES,
			DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH,
			this::buildBuffer, drawDistance);
	
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
		addSetting(hitboxCheck);
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
	public void onRender(PoseStack matrixStack, float partialTicks)
	{
		RenderType layer = WurstRenderLayers.getLines(depthTest.isChecked());
		
		for(Entry<ChunkPos, EasyVertexBuffer> entry : coordinator.getBuffers())
		{
			RegionPos region = RegionPos.of(entry.getKey());
			
			matrixStack.pushPose();
			RenderUtils.applyRegionalRenderOffset(matrixStack, region);
			
			entry.getValue().draw(matrixStack, layer, 1, 1, 1,
				opacity.getValueF());
			
			matrixStack.popPose();
		}
	}
	
	private boolean isSpawnable(BlockPos pos, BlockState state)
	{
		// Check for solid blocks, fluids, redstone, prevent_spawning tags, etc.
		// See SpawnLocationTypes.ON_GROUND
		if(!SpawnPlacements.isSpawnPositionOk(EntityType.CREEPER, MC.level,
			pos))
			return false;
		
		// Check for hitbox collisions
		if(!hitboxCheck.isSpaceEmpty(pos))
			return false;
		
		// Check block light level
		return MC.level.getBrightness(LightLayer.BLOCK, pos) < 1;
	}
	
	private void buildBuffer(VertexConsumer buffer, ChunkSearcher searcher,
		Iterable<Result> results)
	{
		RegionPos region = RegionPos.of(searcher.getPos());
		
		for(Result result : results)
		{
			if(searcher.isInterrupted())
				return;
			
			drawCross(buffer, result.pos(), region);
		}
	}
	
	private void drawCross(VertexConsumer buffer, BlockPos pos,
		RegionPos region)
	{
		float x1 = pos.getX() - region.x();
		float x2 = x1 + 1;
		float y = pos.getY() + 0.01F;
		float z1 = pos.getZ() - region.z();
		float z2 = z1 + 1;
		
		int color = MC.level.getBrightness(LightLayer.SKY, pos) < 8
			? cachedDayColor : cachedNightColor;
		
		RenderUtils.drawLine(buffer, x1, y, z1, x2, y, z2, color);
		RenderUtils.drawLine(buffer, x2, y, z1, x1, y, z2, color);
	}
}
