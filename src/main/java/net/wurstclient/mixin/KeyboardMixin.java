/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
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

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.KeyPressListener.KeyPressEvent;

@Mixin(KeyboardHandler.class)
public class KeyboardMixin
{
	@Inject(at = @At("HEAD"),
		method = "keyPress(JILnet/minecraft/client/input/KeyEvent;)V")
	private void onOnKey(long windowHandle, int action, KeyEvent arg,
		CallbackInfo ci)
	{
		EventManager.fire(new KeyPressEvent(arg.key(), arg.scancode(), action,
			arg.modifiers()));
	}
}
