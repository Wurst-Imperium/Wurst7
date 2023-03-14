/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.render.entity.MobEntityRenderer;
import net.wurstclient.WurstClient;

@Mixin(MobEntityRenderer.class)
public abstract class MobEntityRendererMixin
{
	/**
	 * Makes name-tagged mobs always show their name tags.
	 */
	@Inject(at = @At(value = "FIELD",
		target = "Lnet/minecraft/client/render/entity/EntityRenderDispatcher;targetedEntity:Lnet/minecraft/entity/Entity;",
		opcode = Opcodes.GETFIELD,
		ordinal = 0),
		method = "hasLabel(Lnet/minecraft/entity/mob/MobEntity;)Z",
		cancellable = true)
	private void shouldForceLabel(CallbackInfoReturnable<Boolean> cir)
	{
		// return true if mob has custom name
		if(WurstClient.INSTANCE.getHax().nameTagsHack.shouldForceMobNametags())
			cir.setReturnValue(true);
	}
}
