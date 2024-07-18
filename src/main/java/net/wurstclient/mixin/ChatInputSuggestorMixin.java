/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import java.util.concurrent.CompletableFuture;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.brigadier.suggestion.Suggestions;

import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.AutoCompleteHack;

@Mixin(ChatInputSuggestor.class)
public abstract class ChatInputSuggestorMixin
{
	@Shadow
	@Final
	private TextFieldWidget textField;
	@Shadow
	private CompletableFuture<Suggestions> pendingSuggestions;
	
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
			show(false);
		});
	}
	
	@Shadow
	public abstract void show(boolean narrateFirstSuggestion);
}
