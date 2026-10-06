/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.nametags;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.client.gui.screens.social.EntityPortraitWidget;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.wurstclient.WurstClient;

@Mixin(EntityPortraitWidget.class)
public abstract class EntityPortraitWidgetMixin
{
	/**
	 * Keeps forced player names out of entity portraits without affecting
	 * nametags in the world.
	 */
	@ModifyReturnValue(
		method = "extractRenderState(Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;",
		at = @At("RETURN"))
	private static EntityRenderState hidePlayerNameInPortrait(
		EntityRenderState state, Entity entity)
	{
		if(entity instanceof Player
			&& WurstClient.INSTANCE.getHax().nameTagsHack
				.shouldForcePlayerNametags())
		{
			state.nameTag = null;
			state.scoreText = null;
		}
		
		return state;
	}
}
