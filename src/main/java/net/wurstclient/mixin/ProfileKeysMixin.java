/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.util.ProfileKeys;
import net.minecraft.network.encryption.PlayerPublicKey;
import net.minecraft.network.encryption.Signer;
import net.wurstclient.WurstClient;

@Mixin(ProfileKeys.class)
public class ProfileKeysMixin
{
	@Inject(at = @At("HEAD"),
		method = "getSigner()Lnet/minecraft/network/encryption/Signer;",
		cancellable = true)
	private void onGetSigner(CallbackInfoReturnable<Signer> cir)
	{
		if(WurstClient.INSTANCE.getOtfs().noChatReportsOtf.isActive())
			cir.setReturnValue(null);
	}
	
	@Inject(at = @At("HEAD"),
		method = "getPublicKey()Ljava/util/Optional;",
		cancellable = true)
	private void onGetPublicKey(
		CallbackInfoReturnable<Optional<PlayerPublicKey>> cir)
	{
		if(WurstClient.INSTANCE.getOtfs().noChatReportsOtf.isActive())
			cir.setReturnValue(Optional.empty());
	}
	
	@Inject(at = @At("HEAD"),
		method = "method_45104()Ljava/util/concurrent/CompletableFuture;",
		cancellable = true)
	private void onGetPublicKeyData(
		CallbackInfoReturnable<CompletableFuture<Optional<PlayerPublicKey.PublicKeyData>>> cir)
	{
		if(WurstClient.INSTANCE.getOtfs().noChatReportsOtf.isActive())
			cir.setReturnValue(CompletableFuture.supplyAsync(Optional::empty));
	}
}
