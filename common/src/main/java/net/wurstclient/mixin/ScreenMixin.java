/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.AbstractParentElement;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.wurstclient.WurstClient;
import net.wurstclient.mixinterface.IScreen;

@Mixin(Screen.class)
public abstract class ScreenMixin extends AbstractParentElement
		implements Drawable, IScreen
{
	@Shadow
	@Final
	private List<Drawable> drawables;

	@Inject(at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/network/ClientPlayerEntity;sendChatMessage(Ljava/lang/String;)V",
			ordinal = 0),
			method = {"sendMessage(Ljava/lang/String;Z)V"},
			cancellable = true)
	private void onSendChatMessage(String message, boolean toHud,
								   CallbackInfo ci)
	{
		if(toHud)
			return;

		ChatMessageC2SPacket packet = new ChatMessageC2SPacket(message);
		WurstClient.MC.getNetworkHandler().sendPacket(packet);
		ci.cancel();
	}

	@Inject(at = {@At("HEAD")},
			method = {
					"renderBackground(Lnet/minecraft/client/util/math/MatrixStack;)V"},
			cancellable = true)
	public void onRenderBackground(MatrixStack matrices, CallbackInfo ci)
	{
		if(WurstClient.INSTANCE.getHackRegistry().noBackgroundHack.isEnabled() && WurstClient.isInGame)
			ci.cancel();
	}

	@Override
	public List<Drawable> getButtons()
	{
		return drawables;
	}
}
