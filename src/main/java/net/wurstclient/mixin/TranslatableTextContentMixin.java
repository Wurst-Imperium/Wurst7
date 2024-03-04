/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.text.TextContent;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Language;

@Mixin(TranslatableTextContent.class)
public abstract class TranslatableTextContentMixin implements TextContent
{
	/**
	 * Ensures that any chat messages, written books, signs, etc. cannot resolve
	 * Wurst-related translation keys.
	 *
	 * <p>
	 * Fixes at least one security vulnerability affecting Minecraft 1.20 and
	 * later versions, where the server can detect the presence of Wurst by
	 * abusing Minecraft's sign editing feature. When a player edits a sign, any
	 * translated text and keybind text components on that sign are resolved by
	 * the client and sent back to the server as plain text. This allows the
	 * server to detect the presence of non-vanilla translation keys.
	 *
	 * <p>
	 * It is likely that similar vulnerabilities exist or will exist in other
	 * parts of the game, such as chat messages and written books. Mojang has a
	 * long history of failing to properly secure their text component system
	 * (see BookHack, OP-Sign, BookDupe). Therefore it's best to cut off this
	 * entire attack vector at the source.
	 */
	@WrapOperation(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/util/Language;get(Ljava/lang/String;)Ljava/lang/String;",
		ordinal = 0), method = "updateTranslations()V")
	private String translate(Language instance, String key,
		Operation<String> original)
	{
		if(key != null && key.contains("wurst"))
			return key;
		
		return original.call(instance, key);
	}
	
	/**
	 * Same as above, but for translatable text components with a fallback.
	 */
	@WrapOperation(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/util/Language;get(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
		ordinal = 0), method = "updateTranslations()V")
	private String translateWithFallback(Language instance, String key,
		String fallback, Operation<String> original)
	{
		if(key != null && key.contains("wurst"))
			return fallback;
		
		return original.call(instance, key, fallback);
	}
}
