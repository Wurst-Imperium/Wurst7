/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.autocomplete;

import net.wurstclient.settings.EnumSetting;

public final class ApiProviderSetting
	extends EnumSetting<ApiProviderSetting.ApiProvider>
{
	public ApiProviderSetting()
	{
		super("API provider",
			"\u00a7lOpenAI\u00a7r lets you use models like ChatGPT, but requires an"
				+ " account with API access, costs money to use and sends your chat"
				+ " history to their servers. The name is a lie - it's closed"
				+ " source.\n\n"
				+ "\u00a7loobabooga\u00a7r lets you use models like LLaMA and many"
				+ " others. It's a true open source alternative to OpenAI that you"
				+ " can run locally on your own computer. It's free to use and does"
				+ " not send your chat history to any servers.",
			ApiProvider.values(), ApiProvider.OOBABOOGA);
	}
	
	public enum ApiProvider
	{
		OPENAI("OpenAI"),
		OOBABOOGA("oobabooga");
		
		private final String name;
		
		private ApiProvider(String name)
		{
			this.name = name;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}
