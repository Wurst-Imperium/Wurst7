/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.text.KeybindTextContent;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;

@Mixin(KeybindTextContent.class)
public abstract class KeybindTextContentMixin implements TextContent
{
	@Shadow
	@Final
	private String key;
	
	/**
	 * Ensures that any chat messages, written books, signs, etc. cannot resolve
	 * Wurst-related keybinds.
	 *
	 * <p>
	 * Fixes at least one security vulnerability affecting Minecraft 1.20 and
	 * later versions, where the server can detect the presence of Wurst by
	 * abusing Minecraft's sign editing feature. When a player edits a sign, any
	 * translated text and keybind text components on that sign are resolved by
	 * the client and sent back to the server as plain text. This allows the
	 * server to detect the presence of non-vanilla keybinds, such as Wurst's
	 * zoom keybind.
	 *
	 * <p>
	 * It is likely that similar vulnerabilities exist or will exist in other
	 * parts of the game, such as chat messages and written books. Mojang has a
	 * long history of failing to properly secure their text component system
	 * (see BookHack, OP-Sign, BookDupe). Therefore it's best to cut off this
	 * entire attack vector at the source.
	 */
	@Inject(at = @At("RETURN"),
		method = "getTranslated()Lnet/minecraft/text/Text;",
		cancellable = true)
	private void onGetTranslated(CallbackInfoReturnable<Text> cir)
	{
		if(key != null && key.contains("wurst"))
			cir.setReturnValue(Text.literal(key));
	}
}
