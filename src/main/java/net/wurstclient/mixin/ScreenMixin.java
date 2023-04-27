/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
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
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.Screen;
import net.wurstclient.WurstClient;
import net.wurstclient.mixinterface.IScreen;

@Mixin(Screen.class)
public abstract class ScreenMixin extends AbstractParentElement
	implements Drawable, IScreen
{
	@Shadow
	@Final
	private List<Drawable> drawables;
	
	@Inject(at = @At("HEAD"),
		method = "renderBackground(Lnet/minecraft/client/gui/DrawContext;)V",
		cancellable = true)
	public void onRenderBackground(DrawContext context, CallbackInfo ci)
	{
		if(WurstClient.INSTANCE.getHax().noBackgroundHack
			.shouldCancelBackground((Screen)(Object)this))
			ci.cancel();
	}
	
	@Override
	public List<Drawable> getButtons()
	{
		return drawables;
	}
}
