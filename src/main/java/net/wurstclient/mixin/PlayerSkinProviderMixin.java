/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;

import net.minecraft.client.texture.PlayerSkinProvider;
import net.minecraft.client.texture.PlayerSkinProvider.Textures;
import net.minecraft.client.util.SkinTextures;
import net.wurstclient.util.json.JsonUtils;
import net.wurstclient.util.json.WsonObject;

@Mixin(PlayerSkinProvider.class)
public abstract class PlayerSkinProviderMixin
{
	private static HashMap<String, String> capes;
	private MinecraftProfileTexture currentCape;
	
	@Inject(at = @At("HEAD"),
		method = "fetchSkinTextures(Lcom/mojang/authlib/GameProfile;Lnet/minecraft/client/texture/PlayerSkinProvider$Textures;)Ljava/util/concurrent/CompletableFuture;")
	private void onFetchSkinTextures(GameProfile profile, Textures textures,
		CallbackInfoReturnable<CompletableFuture<SkinTextures>> cir)
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
				currentCape = new MinecraftProfileTexture(capeURL, null);
				
			}else if(capes.containsKey(uuid))
			{
				String capeURL = capes.get(uuid);
				currentCape = new MinecraftProfileTexture(capeURL, null);
				
			}else
				currentCape = null;
			
		}catch(Exception e)
		{
			System.err.println("[Wurst] Failed to load cape for '" + name
				+ "' (" + uuid + ")");
			
			e.printStackTrace();
		}
	}
	
	@ModifyVariable(at = @At("STORE"),
		method = "fetchSkinTextures(Lcom/mojang/authlib/GameProfile;Lnet/minecraft/client/texture/PlayerSkinProvider$Textures;)Ljava/util/concurrent/CompletableFuture;",
		ordinal = 1,
		name = "minecraftProfileTexture2")
	private MinecraftProfileTexture modifyCapeTexture(
		MinecraftProfileTexture old)
	{
		if(currentCape == null)
			return old;
		
		MinecraftProfileTexture result = currentCape;
		currentCape = null;
		return result;
	}
	
	private void setupWurstCapes()
	{
		try
		{
			// download cape list from wurstclient.net
			WsonObject rawCapes = JsonUtils.parseURLToObject(
				"https://www.wurstclient.net/api/v1/capes.json");
			
			capes = rawCapes.getAllStrings();
			
		}catch(Exception e)
		{
			System.err
				.println("[Wurst] Failed to load capes from wurstclient.net!");
			
			e.printStackTrace();
		}
	}
}
