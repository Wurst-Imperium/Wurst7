/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
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

import net.minecraft.client.network.Address;
import net.minecraft.client.network.AddressResolver;
import net.minecraft.client.network.AllowedAddressResolver;
import net.minecraft.client.network.RedirectResolver;
import net.minecraft.client.network.ServerAddress;
import net.wurstclient.WurstClient;

@Mixin(AllowedAddressResolver.class)
public class AllowedAddressResolverMixin
{
	@Shadow
	@Final
	private AddressResolver addressResolver;
	
	@Shadow
	@Final
	private RedirectResolver redirectResolver;
	
	/**
	 * This mixin allows users to connect to servers that have been shadowbanned
	 * by Mojang, such as CS:GO and GTA clones that are apparently "too
	 * adult-oriented" for having pixelated guns.
	 */
	@Inject(at = @At("HEAD"),
		method = "resolve(Lnet/minecraft/client/network/ServerAddress;)Ljava/util/Optional;",
		cancellable = true)
	public void resolve(ServerAddress address,
		CallbackInfoReturnable<Optional<Address>> cir)
	{
		if(!WurstClient.INSTANCE.isEnabled())
			return;
		
		Optional<Address> optionalAddress = addressResolver.resolve(address);
		Optional<ServerAddress> optionalRedirect =
			redirectResolver.lookupRedirect(address);
		
		if(optionalRedirect.isPresent())
			optionalAddress = addressResolver.resolve(optionalRedirect.get());
		
		cir.setReturnValue(optionalAddress);
	}
}
