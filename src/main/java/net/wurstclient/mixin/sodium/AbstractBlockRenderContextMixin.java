/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.sodium;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.ShouldDrawSideListener.ShouldDrawSideEvent;

/**
 * Last updated for <a href=
 * "https://github.com/CaffeineMC/sodium/blob/1ddf7faacbf7be60aff3f00b49d90d199fdd706a/common/src/main/java/net/caffeinemc/mods/sodium/client/render/model/AbstractBlockRenderContext.java">Sodium
 * mc1.21.11-0.8.0</a>
 */
@Pseudo
@Mixin(targets = {
	"net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext"})
public class AbstractBlockRenderContextMixin
{
	@Shadow
	protected BlockState state;
	
	@Shadow
	protected BlockPos pos;
	
	/**
	 * Hides and shows blocks when using X-Ray with Sodium installed.
	 */
	@Inject(at = @At("HEAD"),
		method = "isFaceCulled(Lnet/minecraft/class_2350;)Z",
		cancellable = true,
		remap = false,
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
