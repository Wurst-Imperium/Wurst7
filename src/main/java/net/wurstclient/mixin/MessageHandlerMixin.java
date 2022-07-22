/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.network.message.MessageHandler;
import net.minecraft.client.network.message.MessageTrustStatus;
import net.minecraft.network.message.MessageSourceProfile;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.text.Text;
import net.wurstclient.WurstClient;

@Mixin(MessageHandler.class)
public class MessageHandlerMixin
{
	/**
	 * Stops unreportable chat messages from being labeled as "not secure" or
	 * "modified" when NoChatReports is enabled.
	 */
	@Inject(at = @At("HEAD"),
		method = "getStatus(Lnet/minecraft/network/message/MessageSender;Lnet/minecraft/network/message/SignedMessage;Lnet/minecraft/text/Text;Lnet/minecraft/client/network/PlayerListEntry;)Lnet/minecraft/client/network/message/MessageTrustStatus;",
		cancellable = true)
	private void onGetStatus(MessageSourceProfile messageSender, SignedMessage message,
		Text decorated, @Nullable PlayerListEntry senderEntry,
		CallbackInfoReturnable<MessageTrustStatus> cir)
	{
		if(WurstClient.INSTANCE.getOtfs().noChatReportsOtf.isActive())
			cir.setReturnValue(MessageTrustStatus.SECURE);
	}
}
