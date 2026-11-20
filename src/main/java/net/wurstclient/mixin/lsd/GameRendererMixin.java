/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.lsd;

import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.Identifier;
import net.wurstclient.WurstClient;
import net.wurstclient.hack.HackList;
import net.wurstclient.hacks.LsdHack;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin
{
	@Shadow
	@Final
	private List<Identifier> requestedPostEffects;
	
	@Inject(method = "update(Lnet/minecraft/client/DeltaTracker;)V",
		at = @At("TAIL"))
	private void onUpdate(DeltaTracker deltaTracker, CallbackInfo ci)
	{
		HackList hax = WurstClient.INSTANCE.getHax();
		if(hax == null || !hax.lsdHack.isEnabled()
			|| hax.remoteViewHack.isViewingEntity())
			return;
		
		if(!requestedPostEffects.contains(LsdHack.LSD_POST_EFFECT))
			requestedPostEffects.add(LsdHack.LSD_POST_EFFECT);
	}
}
