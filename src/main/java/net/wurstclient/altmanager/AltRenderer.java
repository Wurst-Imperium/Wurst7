/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.altmanager;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.yggdrasil.ProfileResult;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.player.PlayerSkinType;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import net.wurstclient.WurstClient;

public final class AltRenderer
{
	private static final ExecutorService BACKGROUND_THREAD =
		Executors.newSingleThreadExecutor();
	
	private static final ConcurrentHashMap<String, Identifier> onlineSkins =
		new ConcurrentHashMap<>();
	
	private static final HashMap<String, Identifier> offlineSkins =
		new HashMap<>();
	
	private static Identifier getSkinTexture(String name)
	{
		if(name.isEmpty())
			name = "Steve";
		
		Identifier offlineSkin = offlineSkins.get(name);
		if(offlineSkin == null)
		{
			queueOnlineSkinLoading(name);
			offlineSkin = loadOfflineSkin(name);
		}
		
		Identifier onlineSkin = onlineSkins.get(name);
		return onlineSkin != null ? onlineSkin : offlineSkin;
	}
	
	private static Identifier loadOfflineSkin(String name)
	{
		UUID uuid = Uuids.getOfflinePlayerUuid(name);
		GameProfile profile = new GameProfile(uuid, name);
		PlayerListEntry entry = new PlayerListEntry(profile, false);
		Identifier texture = entry.getSkinTextures().body().texturePath();
		offlineSkins.put(name, texture);
		return texture;
	}
	
	private static void queueOnlineSkinLoading(String name)
	{
		MinecraftClient mc = WurstClient.MC;
		
		CompletableFuture.supplyAsync(() -> {
			
			UUID uuid = SkinStealer.getUUIDOrNull(name);
			ProfileResult result =
				mc.getApiServices().sessionService().fetchProfile(uuid, false);
			
			return result == null ? null : result.profile();
			
		}, BACKGROUND_THREAD).thenComposeAsync(profile -> {
			
			if(profile == null)
				return CompletableFuture.completedFuture(null);
			
			CompletableFuture<Optional<SkinTextures>> skinFuture =
				mc.getSkinProvider().fetchSkinTextures(profile);
			
			return skinFuture.thenApplyAsync(opt -> opt.orElse(null));
			
		}, BACKGROUND_THREAD).thenAcceptAsync(skinTextures -> {
			
			if(skinTextures != null)
				onlineSkins.put(name, skinTextures.body().texturePath());
			
		}, BACKGROUND_THREAD);
	}
	
	public static void drawAltFace(DrawContext context, String name, int x,
		int y, int w, int h, boolean selected)
	{
		try
		{
			Identifier texture = getSkinTexture(name);
			int color = selected ? 0xFFFFFFFF : 0xFFE0E0E0;
			
			// Face
			int fw = 192;
			int fh = 192;
			float u = 24;
			float v = 24;
			context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, u,
				v, w, h, fw, fh, color);
			
			// Hat
			fw = 192;
			fh = 192;
			u = 120;
			v = 24;
			context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, u,
				v, w, h, fw, fh, color);
			
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
			Identifier texture = getSkinTexture(name);
			
			boolean slim = DefaultSkinHelper
				.getSkinTextures(Uuids.getOfflinePlayerUuid(name))
				.model() == PlayerSkinType.SLIM;
			
			// Face
			x = x + width / 4;
			y = y + 0;
			int w = width / 2;
			int h = height / 4;
			int fw = height * 2;
			int fh = height * 2;
			float u = height / 4;
			float v = height / 4;
			context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, u,
				v, w, h, fw, fh);
			
			// Hat
			x = x + 0;
			y = y + 0;
			w = width / 2;
			h = height / 4;
			u = height / 4 * 5;
			v = height / 4;
			context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, u,
				v, w, h, fw, fh);
			
			// Chest
			x = x + 0;
			y = y + height / 4;
			w = width / 2;
			h = height / 8 * 3;
			u = height / 4 * 2.5F;
			v = height / 4 * 2.5F;
			context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, u,
				v, w, h, fw, fh);
			
			// Jacket
			x = x + 0;
			y = y + 0;
			w = width / 2;
			h = height / 8 * 3;
			u = height / 4 * 2.5F;
			v = height / 4 * 4.5F;
			context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, u,
				v, w, h, fw, fh);
			
			// Left Arm
			x = x - width / 16 * (slim ? 3 : 4);
			y = y + (slim ? height / 32 : 0);
			w = width / 16 * (slim ? 3 : 4);
			h = height / 8 * 3;
			u = height / 4 * 5.5F;
			v = height / 4 * 2.5F;
			context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, u,
				v, w, h, fw, fh);
			
			// Left Sleeve
			x = x + 0;
			y = y + 0;
			w = width / 16 * (slim ? 3 : 4);
			h = height / 8 * 3;
			u = height / 4 * 5.5F;
			v = height / 4 * 4.5F;
			context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, u,
				v, w, h, fw, fh);
			
			// Right Arm
			x = x + width / 16 * (slim ? 11 : 12);
			y = y + 0;
			w = width / 16 * (slim ? 3 : 4);
			h = height / 8 * 3;
			u = height / 4 * 5.5F;
			v = height / 4 * 2.5F;
			context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, u,
				v, w, h, fw, fh);
			
			// Right Sleeve
			x = x + 0;
			y = y + 0;
			w = width / 16 * (slim ? 3 : 4);
			h = height / 8 * 3;
			u = height / 4 * 5.5F;
			v = height / 4 * 4.5F;
			context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, u,
				v, w, h, fw, fh);
			
			// Left Leg
			x = x - width / 2;
			y = y + height / 32 * (slim ? 11 : 12);
			w = width / 4;
			h = height / 8 * 3;
			u = height / 4 * 0.5F;
			v = height / 4 * 2.5F;
			context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, u,
				v, w, h, fw, fh);
			
			// Left Pants
			x = x + 0;
			y = y + 0;
			w = width / 4;
			h = height / 8 * 3;
			u = height / 4 * 0.5F;
			v = height / 4 * 4.5F;
			context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, u,
				v, w, h, fw, fh);
			
			// Right Leg
			x = x + width / 4;
			y = y + 0;
			w = width / 4;
			h = height / 8 * 3;
			u = height / 4 * 0.5F;
			v = height / 4 * 2.5F;
			context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, u,
				v, w, h, fw, fh);
			
			// Right Pants
			x = x + 0;
			y = y + 0;
			w = width / 4;
			h = height / 8 * 3;
			u = height / 4 * 0.5F;
			v = height / 4 * 4.5F;
			context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, u,
				v, w, h, fw, fh);
			
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
			Identifier texture = getSkinTexture(name);
			
			boolean slim = DefaultSkinHelper
				.getSkinTextures(Uuids.getOfflinePlayerUuid(name))
				.model() == PlayerSkinType.SLIM;
			
			// Face
			x = x + width / 4;
			y = y + 0;
			int w = width / 2;
			int h = height / 4;
			int fw = height * 2;
			int fh = height * 2;
			float u = height / 4 * 3;
			float v = height / 4;
			context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, u,
				v, w, h, fw, fh);
			
			// Hat
			x = x + 0;
			y = y + 0;
			w = width / 2;
			h = height / 4;
			u = height / 4 * 7;
			v = height / 4;
			context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, u,
				v, w, h, fw, fh);
			
			// Chest
			x = x + 0;
			y = y + height / 4;
			w = width / 2;
			h = height / 8 * 3;
			u = height / 4 * 4;
			v = height / 4 * 2.5F;
			context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, u,
				v, w, h, fw, fh);
			
			// Jacket
			x = x + 0;
			y = y + 0;
			w = width / 2;
			h = height / 8 * 3;
			u = height / 4 * 4;
			v = height / 4 * 4.5F;
			context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, u,
				v, w, h, fw, fh);
			
			// Left Arm
			x = x - width / 16 * (slim ? 3 : 4);
			y = y + (slim ? height / 32 : 0);
			w = width / 16 * (slim ? 3 : 4);
			h = height / 8 * 3;
			u = height / 4 * (slim ? 6.375F : 6.5F);
			v = height / 4 * 2.5F;
			context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, u,
				v, w, h, fw, fh);
			
			// Left Sleeve
			x = x + 0;
			y = y + 0;
			w = width / 16 * (slim ? 3 : 4);
			h = height / 8 * 3;
			u = height / 4 * (slim ? 6.375F : 6.5F);
			v = height / 4 * 4.5F;
			context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, u,
				v, w, h, fw, fh);
			
			// Right Arm
			x = x + width / 16 * (slim ? 11 : 12);
			y = y + 0;
			w = width / 16 * (slim ? 3 : 4);
			h = height / 8 * 3;
			u = height / 4 * (slim ? 6.375F : 6.5F);
			v = height / 4 * 2.5F;
			context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, u,
				v, w, h, fw, fh);
			
			// Right Sleeve
			x = x + 0;
			y = y + 0;
			w = width / 16 * (slim ? 3 : 4);
			h = height / 8 * 3;
			u = height / 4 * (slim ? 6.375F : 6.5F);
			v = height / 4 * 4.5F;
			context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, u,
				v, w, h, fw, fh);
			
			// Left Leg
			x = x - width / 2;
			y = y + height / 32 * (slim ? 11 : 12);
			w = width / 4;
			h = height / 8 * 3;
			u = height / 4 * 1.5F;
			v = height / 4 * 2.5F;
			context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, u,
				v, w, h, fw, fh);
			
			// Left Pants
			x = x + 0;
			y = y + 0;
			w = width / 4;
			h = height / 8 * 3;
			u = height / 4 * 1.5F;
			v = height / 4 * 4.5F;
			context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, u,
				v, w, h, fw, fh);
			
			// Right Leg
			x = x + width / 4;
			y = y + 0;
			w = width / 4;
			h = height / 8 * 3;
			u = height / 4 * 1.5F;
			v = height / 4 * 2.5F;
			context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, u,
				v, w, h, fw, fh);
			
			// Right Pants
			x = x + 0;
			y = y + 0;
			w = width / 4;
			h = height / 8 * 3;
			u = height / 4 * 1.5F;
			v = height / 4 * 4.5F;
			context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, u,
				v, w, h, fw, fh);
			
		}catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
