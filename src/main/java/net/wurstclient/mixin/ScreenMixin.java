/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import java.util.List;

import net.minecraft.client.MinecraftClient;
import net.wurstclient.hacks.IngameBackgroundHack;
import org.jetbrains.annotations.Nullable;
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

	@Shadow @Nullable protected MinecraftClient client;

	@Shadow public abstract void renderBackgroundTexture(int vOffset);

	@Shadow public int width;

	@Shadow public int height;

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

	/**
	 * @author EnZaXD (Florian Michael)
	 */
	@Inject(method = {
			"renderBackground(Lnet/minecraft/client/util/math/MatrixStack;I)V"
			},
			at = @At("HEAD"),
	cancellable = true)
	public void onRenderBackground(MatrixStack matrices, int vOffset, CallbackInfo ci)
	{
		//Minecraft Code
		if (WurstClient.INSTANCE.getHax().ingameBackgroundHack.isEnabled())
		{
			final IngameBackgroundHack hack = WurstClient.INSTANCE.getHax().ingameBackgroundHack;

			if (this.client.world != null) {
				if (!hack.remove.isChecked()) {
					this.fillGradient(matrices, 0, 0, this.width, this.height, hack.firstColor(), hack.secondColor());
				}
			} else {
				this.renderBackgroundTexture(vOffset);
			}
			ci.cancel();
		}
	}

	@Override
	public List<Drawable> getButtons()
	{
		return drawables;
	}
}
