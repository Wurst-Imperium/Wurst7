/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.awt.Color;
import java.util.ArrayList;

import net.wurstclient.core.MatrixUtils;
import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
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
import net.wurstclient.settings.ColorSetting;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.ItemListSetting;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.RotationUtils;
import net.wurstclient.util.SearchType;


@SearchTags({"item esp", "ItemTracers", "item tracers"})
public final class ItemEspHack extends Hack implements UpdateListener,
	CameraTransformViewBobbingListener, RenderListener
{
	private final EnumSetting<Style> style =
		new EnumSetting<>("Style", Style.values(), Style.BOXES);
	
	private final EnumSetting<BoxSize> boxSize = new EnumSetting<>("Box size",
		"§lAccurate§r mode shows the exact\n"
			+ "hitbox of each item.\n"
			+ "§lFancy§r mode shows larger boxes\n"
			+ "that look better.",
		BoxSize.values(), BoxSize.FANCY);

	private final ItemListSetting itemSearch = new ItemListSetting(new SearchType(), "Search List",
			"Use this to search for specific items!\n" + SearchType._DISABLED + ": Will disable the setting\n"
					+ SearchType._ITEMID + ": Each input name is actually an item id\n" + SearchType._NAME
					+ ": Each input name is the name of an item\n",
			"");

	private final ColorSetting color = new ColorSetting("Color",
		"Items will be highlighted in this color.", Color.YELLOW);
	
	private final ArrayList<ItemEntity> items = new ArrayList<>();

	public ItemEspHack()
	{
		super("ItemESP");
		setCategory(Category.RENDER);
		
		addSetting(style);
		addSetting(boxSize);
		addSetting(color);
		addSetting(itemSearch);
	}

	@Override
	public void onEnable() {
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(CameraTransformViewBobbingListener.class, this);
		EVENTS.add(RenderListener.class, this);
	}

	@Override
	public void onDisable() {
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(CameraTransformViewBobbingListener.class, this);
		EVENTS.remove(RenderListener.class, this);
	}

	@Override
	public void onUpdate() {
		items.clear();
		switch (itemSearch.getSearchType().getCurrent()) {
			case SearchType._ITEMID:
				for (Entity entity : MC.world.getEntities()) {
					if (entity instanceof ItemEntity) {
						if (itemSearch.containsItemId(((ItemEntity) entity).getStack().getItem())) {
							items.add((ItemEntity) entity);
						}
					}
				}
				break;
			case SearchType._NAME:
				for (Entity entity : MC.world.getEntities()) {
					if (entity instanceof ItemEntity) {
						if (itemSearch.containsItemName(((ItemEntity) entity).getStack())) {
							items.add((ItemEntity) entity);
						}
					}
				}
				break;
			default:
				for (Entity entity : MC.world.getEntities()) {
					if (entity instanceof ItemEntity) {
						items.add((ItemEntity) entity);
					}
				}
		}
	}

	@Override
	public void onCameraTransformViewBobbing(CameraTransformViewBobbingEvent event) {
		if (style.getSelected().lines)
			event.cancel();
	}

	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		
		matrixStack.push();
		RenderUtils.applyRegionalRenderOffset(matrixStack);
		
		BlockPos camPos = RenderUtils.getCameraBlockPos();
		int regionX = (camPos.getX() >> 9) * 512;
		int regionZ = (camPos.getZ() >> 9) * 512;
		
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
		
		for(ItemEntity e : items)
		{
			matrixStack.push();
			
			matrixStack.translate(
				e.prevX + (e.getX() - e.prevX) * partialTicks - regionX,
				e.prevY + (e.getY() - e.prevY) * partialTicks,
				e.prevZ + (e.getZ() - e.prevZ) * partialTicks - regionZ);
			
			if(style.getSelected().boxes)
			{
				matrixStack.push();
				matrixStack.scale(e.getWidth() + extraSize,
					e.getHeight() + extraSize, e.getWidth() + extraSize);
				
				GL11.glEnable(GL11.GL_BLEND);
				GL11.glDisable(GL11.GL_DEPTH_TEST);
				float[] colorF = color.getColorF();
				RenderSystem.setShaderColor(colorF[0], colorF[1], colorF[2],
					0.5F);
				RenderUtils.drawOutlinedBox(new Box(-0.5, 0, -0.5, 0.5, 1, 0.5),
					matrixStack);
				
				matrixStack.pop();
			}
			
			matrixStack.pop();
		}
	}
	
	private void renderTracers(MatrixStack matrixStack, double partialTicks,
		int regionX, int regionZ)
	{
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		float[] colorF = color.getColorF();
		RenderSystem.setShaderColor(colorF[0], colorF[1], colorF[2], 0.5F);
		
		Matrix4f matrix = MatrixUtils.getPositionMatrix(matrixStack);
		BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
		RenderSystem.setShader(GameRenderer::getPositionShader);
		
		Vec3d start =
			RotationUtils.getClientLookVec().add(RenderUtils.getCameraPos().subtract(regionX, 0, regionZ));
		
		bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINES,
			VertexFormats.POSITION);
		for(ItemEntity e : items)
		{
			Vec3d end = e.getBoundingBox().getCenter()
				.subtract(regionX, 0, regionZ)
				.subtract(new Vec3d(e.getX(), e.getY(), e.getZ())
					.subtract(e.prevX, e.prevY, e.prevZ)
					.multiply(1 - partialTicks));
			
			bufferBuilder.vertex(matrix, (float)start.x, (float)start.y, (float)start.z).next();
			bufferBuilder.vertex(matrix, (float)end.x, (float)end.y, (float)end.z).next();
		}
		bufferBuilder.end();
		BufferRenderer.draw(bufferBuilder);
	}

	private enum Style {
		BOXES("Boxes only", true, false), LINES("Lines only", false, true),
		LINES_AND_BOXES("Lines and boxes", true, true);

		private final String name;
		private final boolean boxes;
		private final boolean lines;

		private Style(String name, boolean boxes, boolean lines) {
			this.name = name;
			this.boxes = boxes;
			this.lines = lines;
		}

		@Override
		public String toString() {
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
		public String toString() {
			return name;
		}
	}
}
