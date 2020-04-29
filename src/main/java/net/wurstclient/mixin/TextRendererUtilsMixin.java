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

import net.minecraft.client.font.TextVisitFactory;
import net.wurstclient.WurstClient;

@Mixin(TextVisitFactory.class)
public abstract class TextRendererUtilsMixin
{
	@ModifyArg(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/class_5223;method_27473(Ljava/lang/String;ILnet/minecraft/text/Style;Lnet/minecraft/text/Style;Lnet/minecraft/class_5223$class_5224;)Z",
		ordinal = 0),
		method = {
			"method_27472(Ljava/lang/String;ILnet/minecraft/text/Style;Lnet/minecraft/class_5223$class_5224;)Z"},
		index = 0)
	private static String adjustText(String text)
	{
		return WurstClient.INSTANCE.getHax().nameProtectHack.protect(text);
	}
}
