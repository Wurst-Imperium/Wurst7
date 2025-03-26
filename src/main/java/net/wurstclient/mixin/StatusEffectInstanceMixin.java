/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
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

import net.minecraft.entity.effect.StatusEffectInstance;
import net.wurstclient.WurstClient;

@Mixin(StatusEffectInstance.class)
public abstract class StatusEffectInstanceMixin
	implements Comparable<StatusEffectInstance>
{
	@Inject(at = @At("HEAD"), method = "updateDuration()V", cancellable = true)
	private void onUpdateDuration(CallbackInfo ci)
	{
		if(WurstClient.INSTANCE.getHax().potionSaverHack.isFrozen())
			ci.cancel();
	}
}
