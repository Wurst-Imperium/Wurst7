/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import java.util.Optional;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.multiplayer.resolver.ResolvedServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerAddressResolver;
import net.minecraft.client.multiplayer.resolver.ServerNameResolver;
import net.minecraft.client.multiplayer.resolver.ServerRedirectHandler;
import net.wurstclient.WurstClient;

@Mixin(ServerNameResolver.class)
public class AllowedAddressResolverMixin
{
	@Shadow
	@Final
	private ServerAddressResolver resolver;
	
	@Shadow
	@Final
	private ServerRedirectHandler redirectHandler;
	
	/**
	 * This mixin allows users to connect to servers that have been shadowbanned
	 * by Mojang, such as CS:GO and GTA clones that are apparently "too
	 * adult-oriented" for having pixelated guns.
	 */
	@Inject(at = @At("HEAD"),
		method = "resolveAddress(Lnet/minecraft/client/multiplayer/resolver/ServerAddress;)Ljava/util/Optional;",
		cancellable = true)
	public void resolve(ServerAddress address,
		CallbackInfoReturnable<Optional<ResolvedServerAddress>> cir)
	{
		if(!WurstClient.INSTANCE.isEnabled())
			return;
		
		Optional<ResolvedServerAddress> optionalAddress =
			resolver.resolve(address);
		Optional<ServerAddress> optionalRedirect =
			redirectHandler.lookupRedirect(address);
		
		if(optionalRedirect.isPresent())
			optionalAddress = resolver.resolve(optionalRedirect.get());
		
		cir.setReturnValue(optionalAddress);
	}
}
