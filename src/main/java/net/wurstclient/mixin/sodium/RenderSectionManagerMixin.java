/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.sodium;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.Camera;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.VisGraphListener.VisGraphEvent;

/**
 * Last updated for <a href=
 * "https://github.com/CaffeineMC/sodium/blob/836dacd26604e1466e3c69dfaa1a4b9a2017c191/common/src/main/java/net/caffeinemc/mods/sodium/client/render/chunk/RenderSectionManager.java">Sodium
 * mc26.1.2-0.9.1-fabric</a>.
 */
@Pseudo
@Mixin(
	targets = "net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager")
public class RenderSectionManagerMixin
{
	/**
	 * Makes VisGraphEvent work when Sodium is installed. Sodium replaces
	 * vanilla's SectionOcclusionGraph with its own RenderSectionManager.
	 */
	@Inject(method = "shouldUseOcclusionCulling",
		at = @At("HEAD"),
		cancellable = true,
		require = 0,
		remap = false)
	private void onShouldUseOcclusionCulling(Camera camera, boolean spectator,
		CallbackInfoReturnable<Boolean> cir)
	{
		VisGraphEvent event = new VisGraphEvent();
		EventManager.fire(event);
		
		if(event.isCancelled())
			cir.setReturnValue(false);
	}
}
