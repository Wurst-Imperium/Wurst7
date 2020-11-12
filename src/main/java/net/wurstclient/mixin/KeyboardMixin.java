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
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Keyboard;
import net.wurstclient.WurstClient;
import net.wurstclient.events.KeyPressListener.KeyPressEvent;

@Mixin(Keyboard.class)
public class KeyboardMixin
{
	@Inject(at = @At("HEAD"), method = "onKey(JIIII)V")
	private void onOnKey(long windowHandle, int keyCode, int scanCode,
		int action, int modifiers, CallbackInfo ci)
	{
		KeyPressEvent event =
			new KeyPressEvent(keyCode, scanCode, action, modifiers);
		
		WurstClient.INSTANCE.getEventManager().fire(event);
	}
}
