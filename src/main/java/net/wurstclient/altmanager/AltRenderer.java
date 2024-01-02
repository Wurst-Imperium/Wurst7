/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.altmanager;

import java.util.HashMap;
import java.util.UUID;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

public final class AltRenderer
{
	private static final HashMap<String, Identifier> loadedSkins =
		new HashMap<>();
	
	private static void bindSkinTexture(String name)
	{
		if(name.isEmpty())
			name = "Steve";
		
		if(loadedSkins.get(name) == null)
		{
			UUID uuid = Uuids.getOfflinePlayerUuid(name);
			
			PlayerListEntry entry =
				new PlayerListEntry(new GameProfile(uuid, name), false);
			
			loadedSkins.put(name, entry.getSkinTextures().texture());
		}
		
		RenderSystem.setShaderTexture(0, loadedSkins.get(name));
	}
	
	private static void drawTexture(DrawContext context, int x, int y, float u,
		float v, int w, int h, int fw, int fh)
	{
		int x2 = x + w;
		int y2 = y + h;
		int z = 0;
		float uOverFw = u / fw;
		float uPlusWOverFw = (u + w) / fw;
		float vOverFh = v / fh;
		float vPlusHOverFh = (v + h) / fh;
		
		RenderSystem.setShader(GameRenderer::getPositionTexProgram);
		MatrixStack matrixStack = context.getMatrices();
		Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();
		BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
		bufferBuilder.begin(VertexFormat.DrawMode.QUADS,
			VertexFormats.POSITION_TEXTURE);
		bufferBuilder.vertex(matrix4f, x, y, z).texture(uOverFw, vOverFh)
			.next();
		bufferBuilder.vertex(matrix4f, x, y2, z).texture(uOverFw, vPlusHOverFh)
			.next();
		bufferBuilder.vertex(matrix4f, x2, y2, z)
			.texture(uPlusWOverFw, vPlusHOverFh).next();
		bufferBuilder.vertex(matrix4f, x2, y, z).texture(uPlusWOverFw, vOverFh)
			.next();
		BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
	}
	
	public static void drawAltFace(DrawContext context, String name, int x,
		int y, int w, int h, boolean selected)
	{
		try
		{
			bindSkinTexture(name);
			GL11.glEnable(GL11.GL_BLEND);
			
			if(selected)
				RenderSystem.setShaderColor(1, 1, 1, 1);
			else
				RenderSystem.setShaderColor(0.9F, 0.9F, 0.9F, 1);
			
			// Face
			int fw = 192;
			int fh = 192;
			float u = 24;
			float v = 24;
			drawTexture(context, x, y, u, v, w, h, fw, fh);
			
			// Hat
			fw = 192;
			fh = 192;
			u = 120;
			v = 24;
			drawTexture(context, x, y, u, v, w, h, fw, fh);
			
			GL11.glDisable(GL11.GL_BLEND);
			
		}catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void drawAltBody(DrawContext context, String name, int x,
		int y, int width, int height)
	{
		try
		{
			bindSkinTexture(name);
			
			boolean slim = DefaultSkinHelper
				.getSkinTextures(Uuids.getOfflinePlayerUuid(name))
				.model() == SkinTextures.Model.SLIM;
			
			GL11.glEnable(GL11.GL_BLEND);
			RenderSystem.setShaderColor(1, 1, 1, 1);
			
			// Face
			x = x + width / 4;
			y = y + 0;
			int w = width / 2;
			int h = height / 4;
			int fw = height * 2;
			int fh = height * 2;
			float u = height / 4;
			float v = height / 4;
			drawTexture(context, x, y, u, v, w, h, fw, fh);
			
			// Hat
			x = x + 0;
			y = y + 0;
			w = width / 2;
			h = height / 4;
			u = height / 4 * 5;
			v = height / 4;
			drawTexture(context, x, y, u, v, w, h, fw, fh);
			
			// Chest
			x = x + 0;
			y = y + height / 4;
			w = width / 2;
			h = height / 8 * 3;
			u = height / 4 * 2.5F;
			v = height / 4 * 2.5F;
			drawTexture(context, x, y, u, v, w, h, fw, fh);
			
			// Jacket
			x = x + 0;
			y = y + 0;
			w = width / 2;
			h = height / 8 * 3;
			u = height / 4 * 2.5F;
			v = height / 4 * 4.5F;
			drawTexture(context, x, y, u, v, w, h, fw, fh);
			
			// Left Arm
			x = x - width / 16 * (slim ? 3 : 4);
			y = y + (slim ? height / 32 : 0);
			w = width / 16 * (slim ? 3 : 4);
			h = height / 8 * 3;
			u = height / 4 * 5.5F;
			v = height / 4 * 2.5F;
			drawTexture(context, x, y, u, v, w, h, fw, fh);
			
			// Left Sleeve
			x = x + 0;
			y = y + 0;
			w = width / 16 * (slim ? 3 : 4);
			h = height / 8 * 3;
			u = height / 4 * 5.5F;
			v = height / 4 * 4.5F;
			drawTexture(context, x, y, u, v, w, h, fw, fh);
			
			// Right Arm
			x = x + width / 16 * (slim ? 11 : 12);
			y = y + 0;
			w = width / 16 * (slim ? 3 : 4);
			h = height / 8 * 3;
			u = height / 4 * 5.5F;
			v = height / 4 * 2.5F;
			drawTexture(context, x, y, u, v, w, h, fw, fh);
			
			// Right Sleeve
			x = x + 0;
			y = y + 0;
			w = width / 16 * (slim ? 3 : 4);
			h = height / 8 * 3;
			u = height / 4 * 5.5F;
			v = height / 4 * 4.5F;
			drawTexture(context, x, y, u, v, w, h, fw, fh);
			
			// Left Leg
			x = x - width / 2;
			y = y + height / 32 * (slim ? 11 : 12);
			w = width / 4;
			h = height / 8 * 3;
			u = height / 4 * 0.5F;
			v = height / 4 * 2.5F;
			drawTexture(context, x, y, u, v, w, h, fw, fh);
			
			// Left Pants
			x = x + 0;
			y = y + 0;
			w = width / 4;
			h = height / 8 * 3;
			u = height / 4 * 0.5F;
			v = height / 4 * 4.5F;
			drawTexture(context, x, y, u, v, w, h, fw, fh);
			
			// Right Leg
			x = x + width / 4;
			y = y + 0;
			w = width / 4;
			h = height / 8 * 3;
			u = height / 4 * 0.5F;
			v = height / 4 * 2.5F;
			drawTexture(context, x, y, u, v, w, h, fw, fh);
			
			// Right Pants
			x = x + 0;
			y = y + 0;
			w = width / 4;
			h = height / 8 * 3;
			u = height / 4 * 0.5F;
			v = height / 4 * 4.5F;
			drawTexture(context, x, y, u, v, w, h, fw, fh);
			
			GL11.glDisable(GL11.GL_BLEND);
			
		}catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void drawAltBack(DrawContext context, String name, int x,
		int y, int width, int height)
	{
		try
		{
			bindSkinTexture(name);
			
			boolean slim = DefaultSkinHelper
				.getSkinTextures(Uuids.getOfflinePlayerUuid(name))
				.model() == SkinTextures.Model.SLIM;
			
			GL11.glEnable(GL11.GL_BLEND);
			RenderSystem.setShaderColor(1, 1, 1, 1);
			
			// Face
			x = x + width / 4;
			y = y + 0;
			int w = width / 2;
			int h = height / 4;
			int fw = height * 2;
			int fh = height * 2;
			float u = height / 4 * 3;
			float v = height / 4;
			drawTexture(context, x, y, u, v, w, h, fw, fh);
			
			// Hat
			x = x + 0;
			y = y + 0;
			w = width / 2;
			h = height / 4;
			u = height / 4 * 7;
			v = height / 4;
			drawTexture(context, x, y, u, v, w, h, fw, fh);
			
			// Chest
			x = x + 0;
			y = y + height / 4;
			w = width / 2;
			h = height / 8 * 3;
			u = height / 4 * 4;
			v = height / 4 * 2.5F;
			drawTexture(context, x, y, u, v, w, h, fw, fh);
			
			// Jacket
			x = x + 0;
			y = y + 0;
			w = width / 2;
			h = height / 8 * 3;
			u = height / 4 * 4;
			v = height / 4 * 4.5F;
			drawTexture(context, x, y, u, v, w, h, fw, fh);
			
			// Left Arm
			x = x - width / 16 * (slim ? 3 : 4);
			y = y + (slim ? height / 32 : 0);
			w = width / 16 * (slim ? 3 : 4);
			h = height / 8 * 3;
			u = height / 4 * (slim ? 6.375F : 6.5F);
			v = height / 4 * 2.5F;
			drawTexture(context, x, y, u, v, w, h, fw, fh);
			
			// Left Sleeve
			x = x + 0;
			y = y + 0;
			w = width / 16 * (slim ? 3 : 4);
			h = height / 8 * 3;
			u = height / 4 * (slim ? 6.375F : 6.5F);
			v = height / 4 * 4.5F;
			drawTexture(context, x, y, u, v, w, h, fw, fh);
			
			// Right Arm
			x = x + width / 16 * (slim ? 11 : 12);
			y = y + 0;
			w = width / 16 * (slim ? 3 : 4);
			h = height / 8 * 3;
			u = height / 4 * (slim ? 6.375F : 6.5F);
			v = height / 4 * 2.5F;
			drawTexture(context, x, y, u, v, w, h, fw, fh);
			
			// Right Sleeve
			x = x + 0;
			y = y + 0;
			w = width / 16 * (slim ? 3 : 4);
			h = height / 8 * 3;
			u = height / 4 * (slim ? 6.375F : 6.5F);
			v = height / 4 * 4.5F;
			drawTexture(context, x, y, u, v, w, h, fw, fh);
			
			// Left Leg
			x = x - width / 2;
			y = y + height / 32 * (slim ? 11 : 12);
			w = width / 4;
			h = height / 8 * 3;
			u = height / 4 * 1.5F;
			v = height / 4 * 2.5F;
			drawTexture(context, x, y, u, v, w, h, fw, fh);
			
			// Left Pants
			x = x + 0;
			y = y + 0;
			w = width / 4;
			h = height / 8 * 3;
			u = height / 4 * 1.5F;
			v = height / 4 * 4.5F;
			drawTexture(context, x, y, u, v, w, h, fw, fh);
			
			// Right Leg
			x = x + width / 4;
			y = y + 0;
			w = width / 4;
			h = height / 8 * 3;
			u = height / 4 * 1.5F;
			v = height / 4 * 2.5F;
			drawTexture(context, x, y, u, v, w, h, fw, fh);
			
			// Right Pants
			x = x + 0;
			y = y + 0;
			w = width / 4;
			h = height / 8 * 3;
			u = height / 4 * 1.5F;
			v = height / 4 * 4.5F;
			drawTexture(context, x, y, u, v, w, h, fw, fh);
			
			GL11.glDisable(GL11.GL_BLEND);
			
		}catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
