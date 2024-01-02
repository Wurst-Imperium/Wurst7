/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixinterface;

import net.minecraft.client.resource.language.TranslationStorage;
import net.wurstclient.WurstClient;

public interface ILanguageManager
{
	public TranslationStorage wurst_getEnglish();
	
	public static TranslationStorage getEnglish()
	{
		return ((ILanguageManager)WurstClient.MC.getLanguageManager())
			.wurst_getEnglish();
	}
}
