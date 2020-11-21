/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
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
public abstract class TextVisitFactoryMixin
{
	@ModifyArg(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/font/TextVisitFactory;visitFormatted(Ljava/lang/String;ILnet/minecraft/text/Style;Lnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z",
		ordinal = 0),
		method = {
			"visitFormatted(Ljava/lang/String;ILnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z"},
		index = 0)
	private static String adjustText(String text)
	{
		return WurstClient.INSTANCE.getHax().nameProtectHack.protect(text);
	}
}
