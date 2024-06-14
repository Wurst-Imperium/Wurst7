/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import java.util.HashMap;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.InsecurePublicKeyException;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.PlayerSkinProvider;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.wurstclient.util.json.JsonUtils;
import net.wurstclient.util.json.WsonObject;

@Mixin(PlayerSkinProvider.class)
public abstract class PlayerSkinProviderMixin
{
	@Shadow
	@Final
	private MinecraftSessionService sessionService;
	
	@Unique
	private static HashMap<String, String> capes;
	
	@Inject(at = @At("HEAD"),
		method = "loadSkin(Lcom/mojang/authlib/GameProfile;Lnet/minecraft/client/texture/PlayerSkinProvider$SkinTextureAvailableCallback;Z)V",
		cancellable = true)
	private void onLoadSkin(GameProfile profile,
		PlayerSkinProvider.SkinTextureAvailableCallback callback,
		boolean requireSecure, CallbackInfo ci)
	{
		// Can't @Inject nicely because everything is wrapped in a lambda.
		// Had to replace the whole method.
		
		Runnable runnable = () -> {
			HashMap<MinecraftProfileTexture.Type, MinecraftProfileTexture> map =
				Maps.newHashMap();
			try
			{
				map.putAll(sessionService.getTextures(profile, requireSecure));
			}catch(InsecurePublicKeyException e)
			{
				// empty catch block
			}
			if(map.isEmpty())
			{
				profile.getProperties().clear();
				if(profile.getId().equals(MinecraftClient.getInstance()
					.getSession().getProfile().getId()))
				{
					profile.getProperties().putAll(
						MinecraftClient.getInstance().getSessionProperties());
					map.putAll(sessionService.getTextures(profile, false));
				}else
				{
					sessionService.fillProfileProperties(profile,
						requireSecure);
					try
					{
						map.putAll(
							sessionService.getTextures(profile, requireSecure));
					}catch(InsecurePublicKeyException e)
					{
						
					}
				}
			}
			
			addWurstCape(profile, map);
			
			MinecraftClient.getInstance().execute(() -> {
				RenderSystem.recordRenderCall(() -> {
					ImmutableList.of(Type.SKIN, Type.CAPE).forEach(type -> {
						if(map.containsKey(type))
							loadSkin(map.get(type), type, callback);
					});
				});
			});
		};
		Util.getMainWorkerExecutor().execute(runnable);
		
		ci.cancel();
	}
	
	private void addWurstCape(GameProfile profile,
		HashMap<MinecraftProfileTexture.Type, MinecraftProfileTexture> map)
	{
		String name = profile.getName();
		String uuid = profile.getId().toString();
		
		try
		{
			if(capes == null)
				setupWurstCapes();
			
			if(capes.containsKey(name))
			{
				String capeURL = capes.get(name);
				map.put(Type.CAPE, new MinecraftProfileTexture(capeURL, null));
				
			}else if(capes.containsKey(uuid))
			{
				String capeURL = capes.get(uuid);
				map.put(Type.CAPE, new MinecraftProfileTexture(capeURL, null));
			}
			
		}catch(Exception e)
		{
			System.err.println("[Wurst] Failed to load cape for '" + name
				+ "' (" + uuid + ")");
			
			e.printStackTrace();
		}
	}
	
	@Unique
	private void setupWurstCapes()
	{
		try
		{
			// assign map first to prevent endless retries if download fails
			capes = new HashMap<>();
			
			// download cape list from wurstclient.net
			WsonObject rawCapes = JsonUtils.parseURLToObject(
				"https://www.wurstclient.net/api/v1/capes.json");
			
			// assign capes
			capes = rawCapes.getAllStrings();
			
		}catch(Exception e)
		{
			System.err
				.println("[Wurst] Failed to load capes from wurstclient.net!");
			
			e.printStackTrace();
		}
	}
	
	@Shadow
	public abstract Identifier loadSkin(MinecraftProfileTexture profileTexture,
		Type type,
		@Nullable PlayerSkinProvider.SkinTextureAvailableCallback callback);
}
