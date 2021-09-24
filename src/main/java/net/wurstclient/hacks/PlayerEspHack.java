/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.CameraTransformViewBobbingListener;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.util.FakePlayerEntity;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.RotationUtils;

@SearchTags({"player esp", "PlayerTracers", "player tracers"})
public final class PlayerEspHack extends Hack implements UpdateListener,
	CameraTransformViewBobbingListener, RenderListener
{
	private final EnumSetting<Style> style =
		new EnumSetting<>("风格", Style.values(), Style.LINES_AND_BOXES);
	
	private final EnumSetting<BoxSize> boxSize = new EnumSetting<>("碰撞箱",
		"§l准确§r模式显示每个生物的准确碰撞箱 \n§l人性化模式显示更大的碰撞箱，更容易被看到",
		BoxSize.values(), BoxSize.FANCY);
	
	private final CheckboxSetting filterSleeping = new CheckboxSetting(
		"排除睡眠", "", false);
	
	private final CheckboxSetting filterInvisible = new CheckboxSetting(
		"排除隐形", "", false);
	
	private final ArrayList<PlayerEntity> players = new ArrayList<>();
	
	public PlayerEspHack()
	{
		super("高亮玩家","高亮显示附近的玩家.\n白名单内的玩家将以蓝色显示");
		setCategory(Category.RENDER);
		
		addSetting(style);
		addSetting(boxSize);
		addSetting(filterSleeping);
		addSetting(filterInvisible);
	}
	
	@Override
	public void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(CameraTransformViewBobbingListener.class, this);
		EVENTS.add(RenderListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(CameraTransformViewBobbingListener.class, this);
		EVENTS.remove(RenderListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		PlayerEntity player = MC.player;
		ClientWorld world = MC.world;
		
		players.clear();
		Stream<AbstractClientPlayerEntity> stream = world.getPlayers()
			.parallelStream().filter(e -> !e.isRemoved() && e.getHealth() > 0)
			.filter(e -> e != player)
			.filter(e -> !(e instanceof FakePlayerEntity))
			.filter(e -> Math.abs(e.getY() - MC.player.getY()) <= 1e6);
		
		if(filterSleeping.isChecked())
			stream = stream.filter(e -> !e.isSleeping());
		
		if(filterInvisible.isChecked())
			stream = stream.filter(e -> !e.isInvisible());
		
		players.addAll(stream.collect(Collectors.toList()));
	}
	
	@Override
	public void onCameraTransformViewBobbing(
		CameraTransformViewBobbingEvent event)
	{
		if(style.getSelected().lines)
			event.cancel();
	}
	
	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		// GL settings
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		
		matrixStack.push();
		RenderUtils.applyRegionalRenderOffset(matrixStack);
		
		BlockPos camPos = RenderUtils.getCameraBlockPos();
		int regionX = (camPos.getX() >> 9) * 512;
		int regionZ = (camPos.getZ() >> 9) * 512;
		
		// draw boxes
		if(style.getSelected().boxes)
			renderBoxes(matrixStack, partialTicks, regionX, regionZ);
		
		if(style.getSelected().lines)
			renderTracers(matrixStack, partialTicks, regionX, regionZ);
		
		matrixStack.pop();
		
		// GL resets
		RenderSystem.setShaderColor(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_LINE_SMOOTH);
	}
	
	private void renderBoxes(MatrixStack matrixStack, double partialTicks,
		int regionX, int regionZ)
	{
		float extraSize = boxSize.getSelected().extraSize;
		
		for(PlayerEntity e : players)
		{
			matrixStack.push();
			
			matrixStack.translate(
				e.prevX + (e.getX() - e.prevX) * partialTicks - regionX,
				e.prevY + (e.getY() - e.prevY) * partialTicks,
				e.prevZ + (e.getZ() - e.prevZ) * partialTicks - regionZ);
			
			matrixStack.scale(e.getWidth() + extraSize,
				e.getHeight() + extraSize, e.getWidth() + extraSize);
			
			// set color
			if(WURST.getFriends().contains(e.getEntityName()))
				RenderSystem.setShaderColor(0, 0, 1, 0.5F);
			else
			{
				float f = MC.player.distanceTo(e) / 20F;
				RenderSystem.setShaderColor(2 - f, f, 0, 0.5F);
			}
			
			Box bb = new Box(-0.5, 0, -0.5, 0.5, 1, 0.5);
			RenderUtils.drawOutlinedBox(bb, matrixStack);
			
			matrixStack.pop();
		}
	}
	
	private void renderTracers(MatrixStack matrixStack, double partialTicks,
		int regionX, int regionZ)
	{
		Vec3d start =
			RotationUtils.getClientLookVec().add(RenderUtils.getCameraPos());
		
		Matrix4f matrix = matrixStack.peek().getModel();
		BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
		RenderSystem.setShader(GameRenderer::getPositionShader);
		
		bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINES,
			VertexFormats.POSITION);
		for(PlayerEntity e : players)
		{
			Vec3d end = e.getBoundingBox().getCenter()
				.subtract(new Vec3d(e.getX(), e.getY(), e.getZ())
					.subtract(e.prevX, e.prevY, e.prevZ)
					.multiply(1 - partialTicks));
			
			if(WURST.getFriends().contains(e.getEntityName()))
				RenderSystem.setShaderColor(0, 0, 1, 0.5F);
			else
			{
				float f = MC.player.distanceTo(e) / 20F;
				RenderSystem.setShaderColor(2 - f, f, 0, 0.5F);
			}
			
			bufferBuilder.vertex(matrix, (float)start.x - regionX,
				(float)start.y, (float)start.z - regionZ).next();
			bufferBuilder.vertex(matrix, (float)end.x - regionX, (float)end.y,
				(float)end.z - regionZ).next();
		}
		bufferBuilder.end();
		BufferRenderer.draw(bufferBuilder);
	}
	
	private enum Style
	{
		BOXES("Boxes only", true, false),
		LINES("Lines only", false, true),
		LINES_AND_BOXES("Lines and boxes", true, true);
		
		private final String name;
		private final boolean boxes;
		private final boolean lines;
		
		private Style(String name, boolean boxes, boolean lines)
		{
			this.name = name;
			this.boxes = boxes;
			this.lines = lines;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
	
	private enum BoxSize
	{
		ACCURATE("Accurate", 0),
		FANCY("Fancy", 0.1F);
		
		private final String name;
		private final float extraSize;
		
		private BoxSize(String name, float extraSize)
		{
			this.name = name;
			this.extraSize = extraSize;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}
