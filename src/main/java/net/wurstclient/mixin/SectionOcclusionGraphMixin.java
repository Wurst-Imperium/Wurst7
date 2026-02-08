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
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.core.Direction;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.VisGraphListener.VisGraphEvent;

@Mixin(SectionOcclusionGraph.class)
public class SectionOcclusionGraphMixin
{
	@Redirect(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/renderer/chunk/SectionMesh;facesCanSeeEachother(Lnet/minecraft/core/Direction;Lnet/minecraft/core/Direction;)Z"),
		method = "runUpdates")
	private boolean onFacesCanSeeEachother(SectionMesh mesh, Direction from,
		Direction to)
	{
		VisGraphEvent event = new VisGraphEvent();
		EventManager.fire(event);
		
		if(event.isCancelled())
			return true;
		
		return mesh.facesCanSeeEachother(from, to);
	}
}
