/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import java.util.ArrayList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.google.common.collect.Lists;

import net.minecraft.client.resource.language.LanguageDefinition;
import net.minecraft.client.resource.language.LanguageManager;
import net.minecraft.client.resource.language.TranslationStorage;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SynchronousResourceReloader;
import net.wurstclient.mixinterface.ILanguageManager;

@Mixin(LanguageManager.class)
public abstract class LanguageManagerMixin
	implements SynchronousResourceReloader, ILanguageManager
{
	private TranslationStorage english;
	
	@Inject(at = {@At("HEAD")},
		method = {"reload(Lnet/minecraft/resource/ResourceManager;)V"})
	private void onReload(ResourceManager manager, CallbackInfo ci)
	{
		LanguageDefinition englishDef =
			new LanguageDefinition("en_us", "US", "English", false);
		ArrayList<LanguageDefinition> englishDefList =
			Lists.newArrayList(englishDef);
		
		english = TranslationStorage.load(manager, englishDefList);
	}
	
	@Override
	public TranslationStorage getEnglish()
	{
		return english;
	}
}
