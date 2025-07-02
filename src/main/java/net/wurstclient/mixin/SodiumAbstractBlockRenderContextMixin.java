/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.ShouldDrawSideListener.ShouldDrawSideEvent;

/**
 * Last updated for <a href=
 * "https://github.com/CaffeineMC/sodium/tree/02253db283e4679228ba5fbc30cfc851d17123c8">Sodium
 * 0.6.13+mc1.21.6</a>
 */
@Pseudo
@Mixin(targets = {
	"net.caffeinemc.mods.sodium.client.render.frapi.render.AbstractBlockRenderContext"})
public class SodiumAbstractBlockRenderContextMixin
{
	@Shadow
	protected BlockState state;
	
	@Shadow
	protected BlockPos pos;
	
	/**
	 * Hides and shows blocks when using X-Ray with Sodium installed.
	 */
	@Inject(at = @At("HEAD"),
		method = "isFaceCulled(Lnet/minecraft/util/math/Direction;)Z",
		cancellable = true,
		require = 0)
	private void onIsFaceCulled(@Nullable Direction face,
		CallbackInfoReturnable<Boolean> cir)
	{
		ShouldDrawSideEvent event = new ShouldDrawSideEvent(state, pos);
		EventManager.fire(event);
		
		if(event.isRendered() != null)
			cir.setReturnValue(!event.isRendered());
	}
}
