/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.altmanager;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceLocation;
import net.wurstclient.WurstClient;

public final class AltRenderer
{
	private static final ExecutorService BACKGROUND_THREAD =
		Executors.newSingleThreadExecutor();
	
	private static final ConcurrentHashMap<String, ResourceLocation> onlineSkins =
		new ConcurrentHashMap<>();
	
	private static final HashMap<String, ResourceLocation> offlineSkins =
		new HashMap<>();
	
	private static ResourceLocation getSkinTexture(String name)
	{
		if(name.isEmpty())
			name = "Steve";
		
		ResourceLocation offlineSkin = offlineSkins.get(name);
		if(offlineSkin == null)
		{
			queueOnlineSkinLoading(name);
			offlineSkin = loadOfflineSkin(name);
		}
		
		ResourceLocation onlineSkin = onlineSkins.get(name);
		return onlineSkin != null ? onlineSkin : offlineSkin;
	}
	
	private static ResourceLocation loadOfflineSkin(String name)
	{
		UUID uuid = UUIDUtil.createOfflinePlayerUUID(name);
		GameProfile profile = new GameProfile(uuid, name);
		PlayerInfo entry = new PlayerInfo(profile, false);
		ResourceLocation texture = entry.getSkin().texture();
		offlineSkins.put(name, texture);
		return texture;
	}
	
	private static void queueOnlineSkinLoading(String name)
	{
		Minecraft mc = WurstClient.MC;
		
		CompletableFuture.supplyAsync(() -> {
			
			UUID uuid = SkinStealer.getUUIDOrNull(name);
			ProfileResult result =
				mc.getMinecraftSessionService().fetchProfile(uuid, false);
			
			return result == null ? null : result.profile();
			
		}, BACKGROUND_THREAD).thenComposeAsync(profile -> {
			
			if(profile == null)
				return CompletableFuture.completedFuture(null);
			
			CompletableFuture<PlayerSkin> skinFuture =
				mc.getSkinManager().getOrLoad(profile);
			
			return skinFuture;
			
		}, BACKGROUND_THREAD).thenAcceptAsync(skinTextures -> {
			
			if(skinTextures != null)
				onlineSkins.put(name, skinTextures.texture());
			
		}, BACKGROUND_THREAD);
	}
	
	public static void drawAltFace(GuiGraphics context, String name, int x,
		int y, int w, int h, boolean selected)
	{
		try
		{
			ResourceLocation texture = getSkinTexture(name);
			
			if(selected)
				RenderSystem.setShaderColor(1, 1, 1, 1);
			else
				RenderSystem.setShaderColor(0.9F, 0.9F, 0.9F, 1);
			
			// Face
			int fw = 192;
			int fh = 192;
			float u = 24;
			float v = 24;
			context.blit(texture, x, y, u, v, w, h, fw, fh);
			
			// Hat
			fw = 192;
			fh = 192;
			u = 120;
			v = 24;
			context.blit(texture, x, y, u, v, w, h, fw, fh);
			
			RenderSystem.setShaderColor(1, 1, 1, 1);
			
		}catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void drawAltBody(GuiGraphics context, String name, int x,
		int y, int width, int height)
	{
		try
		{
			ResourceLocation texture = getSkinTexture(name);
			RenderSystem.setShaderColor(1, 1, 1, 1);
			
			boolean slim =
				DefaultPlayerSkin.get(UUIDUtil.createOfflinePlayerUUID(name))
					.model() == PlayerSkin.Model.SLIM;
			
			// Face
			x = x + width / 4;
			y = y + 0;
			int w = width / 2;
			int h = height / 4;
			int fw = height * 2;
			int fh = height * 2;
			float u = height / 4;
			float v = height / 4;
			context.blit(texture, x, y, u, v, w, h, fw, fh);
			
			// Hat
			x = x + 0;
			y = y + 0;
			w = width / 2;
			h = height / 4;
			u = height / 4 * 5;
			v = height / 4;
			context.blit(texture, x, y, u, v, w, h, fw, fh);
			
			// Chest
			x = x + 0;
			y = y + height / 4;
			w = width / 2;
			h = height / 8 * 3;
			u = height / 4 * 2.5F;
			v = height / 4 * 2.5F;
			context.blit(texture, x, y, u, v, w, h, fw, fh);
			
			// Jacket
			x = x + 0;
			y = y + 0;
			w = width / 2;
			h = height / 8 * 3;
			u = height / 4 * 2.5F;
			v = height / 4 * 4.5F;
			context.blit(texture, x, y, u, v, w, h, fw, fh);
			
			// Left Arm
			x = x - width / 16 * (slim ? 3 : 4);
			y = y + (slim ? height / 32 : 0);
			w = width / 16 * (slim ? 3 : 4);
			h = height / 8 * 3;
			u = height / 4 * 5.5F;
			v = height / 4 * 2.5F;
			context.blit(texture, x, y, u, v, w, h, fw, fh);
			
			// Left Sleeve
			x = x + 0;
			y = y + 0;
			w = width / 16 * (slim ? 3 : 4);
			h = height / 8 * 3;
			u = height / 4 * 5.5F;
			v = height / 4 * 4.5F;
			context.blit(texture, x, y, u, v, w, h, fw, fh);
			
			// Right Arm
			x = x + width / 16 * (slim ? 11 : 12);
			y = y + 0;
			w = width / 16 * (slim ? 3 : 4);
			h = height / 8 * 3;
			u = height / 4 * 5.5F;
			v = height / 4 * 2.5F;
			context.blit(texture, x, y, u, v, w, h, fw, fh);
			
			// Right Sleeve
			x = x + 0;
			y = y + 0;
			w = width / 16 * (slim ? 3 : 4);
			h = height / 8 * 3;
			u = height / 4 * 5.5F;
			v = height / 4 * 4.5F;
			context.blit(texture, x, y, u, v, w, h, fw, fh);
			
			// Left Leg
			x = x - width / 2;
			y = y + height / 32 * (slim ? 11 : 12);
			w = width / 4;
			h = height / 8 * 3;
			u = height / 4 * 0.5F;
			v = height / 4 * 2.5F;
			context.blit(texture, x, y, u, v, w, h, fw, fh);
			
			// Left Pants
			x = x + 0;
			y = y + 0;
			w = width / 4;
			h = height / 8 * 3;
			u = height / 4 * 0.5F;
			v = height / 4 * 4.5F;
			context.blit(texture, x, y, u, v, w, h, fw, fh);
			
			// Right Leg
			x = x + width / 4;
			y = y + 0;
			w = width / 4;
			h = height / 8 * 3;
			u = height / 4 * 0.5F;
			v = height / 4 * 2.5F;
			context.blit(texture, x, y, u, v, w, h, fw, fh);
			
			// Right Pants
			x = x + 0;
			y = y + 0;
			w = width / 4;
			h = height / 8 * 3;
			u = height / 4 * 0.5F;
			v = height / 4 * 4.5F;
			context.blit(texture, x, y, u, v, w, h, fw, fh);
			
		}catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void drawAltBack(GuiGraphics context, String name, int x,
		int y, int width, int height)
	{
		try
		{
			ResourceLocation texture = getSkinTexture(name);
			RenderSystem.setShaderColor(1, 1, 1, 1);
			
			boolean slim =
				DefaultPlayerSkin.get(UUIDUtil.createOfflinePlayerUUID(name))
					.model() == PlayerSkin.Model.SLIM;
			
			// Face
			x = x + width / 4;
			y = y + 0;
			int w = width / 2;
			int h = height / 4;
			int fw = height * 2;
			int fh = height * 2;
			float u = height / 4 * 3;
			float v = height / 4;
			context.blit(texture, x, y, u, v, w, h, fw, fh);
			
			// Hat
			x = x + 0;
			y = y + 0;
			w = width / 2;
			h = height / 4;
			u = height / 4 * 7;
			v = height / 4;
			context.blit(texture, x, y, u, v, w, h, fw, fh);
			
			// Chest
			x = x + 0;
			y = y + height / 4;
			w = width / 2;
			h = height / 8 * 3;
			u = height / 4 * 4;
			v = height / 4 * 2.5F;
			context.blit(texture, x, y, u, v, w, h, fw, fh);
			
			// Jacket
			x = x + 0;
			y = y + 0;
			w = width / 2;
			h = height / 8 * 3;
			u = height / 4 * 4;
			v = height / 4 * 4.5F;
			context.blit(texture, x, y, u, v, w, h, fw, fh);
			
			// Left Arm
			x = x - width / 16 * (slim ? 3 : 4);
			y = y + (slim ? height / 32 : 0);
			w = width / 16 * (slim ? 3 : 4);
			h = height / 8 * 3;
			u = height / 4 * (slim ? 6.375F : 6.5F);
			v = height / 4 * 2.5F;
			context.blit(texture, x, y, u, v, w, h, fw, fh);
			
			// Left Sleeve
			x = x + 0;
			y = y + 0;
			w = width / 16 * (slim ? 3 : 4);
			h = height / 8 * 3;
			u = height / 4 * (slim ? 6.375F : 6.5F);
			v = height / 4 * 4.5F;
			context.blit(texture, x, y, u, v, w, h, fw, fh);
			
			// Right Arm
			x = x + width / 16 * (slim ? 11 : 12);
			y = y + 0;
			w = width / 16 * (slim ? 3 : 4);
			h = height / 8 * 3;
			u = height / 4 * (slim ? 6.375F : 6.5F);
			v = height / 4 * 2.5F;
			context.blit(texture, x, y, u, v, w, h, fw, fh);
			
			// Right Sleeve
			x = x + 0;
			y = y + 0;
			w = width / 16 * (slim ? 3 : 4);
			h = height / 8 * 3;
			u = height / 4 * (slim ? 6.375F : 6.5F);
			v = height / 4 * 4.5F;
			context.blit(texture, x, y, u, v, w, h, fw, fh);
			
			// Left Leg
			x = x - width / 2;
			y = y + height / 32 * (slim ? 11 : 12);
			w = width / 4;
			h = height / 8 * 3;
			u = height / 4 * 1.5F;
			v = height / 4 * 2.5F;
			context.blit(texture, x, y, u, v, w, h, fw, fh);
			
			// Left Pants
			x = x + 0;
			y = y + 0;
			w = width / 4;
			h = height / 8 * 3;
			u = height / 4 * 1.5F;
			v = height / 4 * 4.5F;
			context.blit(texture, x, y, u, v, w, h, fw, fh);
			
			// Right Leg
			x = x + width / 4;
			y = y + 0;
			w = width / 4;
			h = height / 8 * 3;
			u = height / 4 * 1.5F;
			v = height / 4 * 2.5F;
			context.blit(texture, x, y, u, v, w, h, fw, fh);
			
			// Right Pants
			x = x + 0;
			y = y + 0;
			w = width / 4;
			h = height / 8 * 3;
			u = height / 4 * 1.5F;
			v = height / 4 * 4.5F;
			context.blit(texture, x, y, u, v, w, h, fw, fh);
			
		}catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
