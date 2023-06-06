/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import java.util.concurrent.CompletableFuture;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestions;

import net.minecraft.client.gui.screen.CommandSuggestor;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.command.CommandSource;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.AutoCompleteHack;

@Mixin(CommandSuggestor.class)
public abstract class ChatInputSuggestorMixin
{
	@Shadow
	private TextFieldWidget textField;
	@Shadow
	private CompletableFuture<Suggestions> pendingSuggestions;
	@Shadow
	private ParseResults<CommandSource> parse;
	
	@Inject(at = @At("TAIL"), method = "refresh()V")
	private void onRefresh(CallbackInfo ci)
	{
		AutoCompleteHack autoComplete =
			WurstClient.INSTANCE.getHax().autoCompleteHack;
		if(!autoComplete.isEnabled())
			return;
		
		String draftMessage =
			textField.getText().substring(0, textField.getCursor());
		autoComplete.onRefresh(draftMessage, (builder, suggestion) -> {
			textField.setSuggestion(suggestion);
			pendingSuggestions = builder.buildFuture();
			
			// needed to stop show() from crashing
			ClientPlayNetworkHandler netHandler =
				WurstClient.MC.player.networkHandler;
			parse = netHandler.getCommandDispatcher()
				.parse(new StringReader(""), netHandler.getCommandSource());
			parse.getExceptions().clear();
			
			show();
		});
	}
	
	@Shadow
	public abstract void show();
}
