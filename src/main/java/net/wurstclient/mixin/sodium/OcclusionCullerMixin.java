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
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.wurstclient.event.EventManager;
import net.wurstclient.events.VisGraphListener.VisGraphEvent;

/**
 * Last updated for <a href=
 * "https://github.com/CaffeineMC/sodium/tree/1d2942a247f87a3cb1ae268166190b6ec267046e">Sodium
 * mc1.21.11-0.8.4-fabric</a>.
 */
@Pseudo
@Mixin(
	targets = "net.caffeinemc.mods.sodium.client.render.chunk.occlusion.OcclusionCuller")
public class OcclusionCullerMixin
{
	/**
	 * Makes VisGraphEvent work when Sodium is installed. Sodium replaces
	 * vanilla's SectionOcclusionGraph with its own OcclusionCuller.
	 */
	@ModifyVariable(
		method = "findVisible(Lnet/caffeinemc/mods/sodium/client/render/chunk/lists/RenderSectionVisitor;Lnet/caffeinemc/mods/sodium/client/render/viewport/Viewport;FZI)V",
		at = @At("HEAD"),
		argsOnly = true,
		ordinal = 0,
		require = 0,
		remap = false)
	private boolean onFindVisible(boolean useOcclusionCulling)
	{
		VisGraphEvent event = new VisGraphEvent();
		EventManager.fire(event);
		
		if(event.isCancelled())
			return false;
		
		return useOcclusionCulling;
	}
}
