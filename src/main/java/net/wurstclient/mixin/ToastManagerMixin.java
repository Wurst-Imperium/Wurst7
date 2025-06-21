/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import net.minecraft.client.toast.*;
import net.wurstclient.WurstClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ToastManager.class)
public class ToastManagerMixin
{
	
	@Inject(method = "add", at = @At("HEAD"), cancellable = true)
	private void onAdd(Toast toast, CallbackInfo ci)
	{
		if(!WurstClient.INSTANCE.getHax().noToastsHack.isEnabled())
			return;
		
		if(toast == null)
			return;
		
		if(toast instanceof AdvancementToast || toast instanceof NowPlayingToast
			|| toast instanceof RecipeToast || toast instanceof TutorialToast)
			ci.cancel();
	}
}
