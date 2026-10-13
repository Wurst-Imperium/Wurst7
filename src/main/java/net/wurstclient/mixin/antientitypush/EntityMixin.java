/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.antientitypush;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.Entity;
import net.wurstclient.WurstClient;

@Mixin(Entity.class)
public abstract class EntityMixin
{
	/**
	 * Prevents entity collisions from pushing the local player while
	 * AntiEntityPush is enabled.
	 */
	@Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V",
		at = @At("HEAD"),
		cancellable = true)
	private void onPush(Entity entity, CallbackInfo ci)
	{
		if((Object)this == WurstClient.MC.player
			&& WurstClient.INSTANCE.getHax().antiEntityPushHack.isEnabled())
			ci.cancel();
	}
}
