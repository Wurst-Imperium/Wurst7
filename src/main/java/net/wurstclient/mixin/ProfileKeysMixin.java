/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.client.util.ProfileKeysImpl;

@Mixin(ProfileKeysImpl.class)
public class ProfileKeysMixin
{
	// FIXME
	// @Inject(at = @At("HEAD"),
	// method = "getSigner()Lnet/minecraft/network/encryption/Signer;",
	// cancellable = true)
	// private void onGetSigner(CallbackInfoReturnable<Signer> cir)
	// {
	// if(WurstClient.INSTANCE.getOtfs().noChatReportsOtf.isActive())
	// cir.setReturnValue(null);
	// }
	//
	// @Inject(at = @At("HEAD"),
	// method = "getPublicKey()Ljava/util/Optional;",
	// cancellable = true)
	// private void onGetPublicKey(
	// CallbackInfoReturnable<Optional<PlayerPublicKey>> cir)
	// {
	// if(WurstClient.INSTANCE.getOtfs().noChatReportsOtf.isActive())
	// cir.setReturnValue(Optional.empty());
	// }
	//
	// @Inject(at = @At("HEAD"),
	// method = "refresh()Ljava/util/concurrent/CompletableFuture;",
	// cancellable = true)
	// private void onGetPublicKeyData(
	// CallbackInfoReturnable<CompletableFuture<Optional<PlayerPublicKey.PublicKeyData>>>
	// cir)
	// {
	// if(WurstClient.INSTANCE.getOtfs().noChatReportsOtf.isActive())
	// cir.setReturnValue(CompletableFuture.supplyAsync(Optional::empty));
	// }
}
