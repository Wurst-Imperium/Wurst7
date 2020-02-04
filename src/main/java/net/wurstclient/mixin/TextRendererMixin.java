/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import net.minecraft.client.font.TextRenderer;
import net.wurstclient.WurstClient;

@Mixin(TextRenderer.class)
public abstract class TextRendererMixin implements AutoCloseable
{
	@ModifyArg(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/font/TextRenderer;drawInternal(Ljava/lang/String;FFIZLnet/minecraft/client/util/math/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;ZII)I",
		ordinal = 0),
		method = {
			"draw(Ljava/lang/String;FFIZLnet/minecraft/client/util/math/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;ZII)I"},
		index = 0)
	private String adjustText(String text)
	{
		return WurstClient.INSTANCE.getHax().nameProtectHack.protect(text);
	}
}
