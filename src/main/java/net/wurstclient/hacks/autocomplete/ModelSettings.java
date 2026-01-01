/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.autocomplete;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.Setting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.settings.TextFieldSetting;

public final class ModelSettings
{
	public final EnumSetting<OpenAiModel> openAiModel = new EnumSetting<>(
		"OpenAI model", "The model to use for OpenAI API calls.",
		OpenAiModel.values(), OpenAiModel.GPT_4O_2024_08_06);
	
	public enum OpenAiModel
	{
		GPT_4O_2024_08_06("gpt-4o-2024-08-06", true),
		GPT_4O_2024_05_13("gpt-4o-2024-05-13", true),
		GPT_4O_MINI_2024_07_18("gpt-4o-mini-2024-07-18", true),
		GPT_4_TURBO_2024_04_09("gpt-4-turbo-2024-04-09", true),
		GPT_4_0125_PREVIEW("gpt-4-0125-preview", true),
		GPT_4_1106_PREVIEW("gpt-4-1106-preview", true),
		GPT_4_0613("gpt-4-0613", true),
		GPT_3_5_TURBO_0125("gpt-3.5-turbo-0125", true),
		GPT_3_5_TURBO_1106("gpt-3.5-turbo-1106", true),
		GPT_3_5_TURBO_INSTRUCT("gpt-3.5-turbo-instruct", false),
		DAVINCI_002("davinci-002", false),
		BABBAGE_002("babbage-002", false);
		
		private final String name;
		private final boolean chat;
		
		private OpenAiModel(String name, boolean chat)
		{
			this.name = name;
			this.chat = chat;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
		
		public boolean isChatModel()
		{
			return chat;
		}
	}
	
	public final SliderSetting maxTokens = new SliderSetting("Max tokens",
		"The maximum number of tokens that the model can generate.\n\n"
			+ "Higher values allow the model to predict longer chat messages,"
			+ " but also increase the time it takes to generate predictions.\n\n"
			+ "The default value of 16 is fine for most use cases.",
		16, 1, 100, 1, ValueDisplay.INTEGER);
	
	public final SliderSetting temperature = new SliderSetting("Temperature",
		"Controls the model's creativity and randomness. A higher value will"
			+ " result in more creative and sometimes nonsensical completions,"
			+ " while a lower value will result in more boring completions.",
		1, 0, 2, 0.01, ValueDisplay.DECIMAL);
	
	public final SliderSetting topP = new SliderSetting("Top P",
		"An alternative to temperature. Makes the model less random by only"
			+ " letting it choose from the most likely tokens.\n\n"
			+ "A value of 100% disables this feature by letting the model"
			+ " choose from all tokens.",
		1, 0, 1, 0.01, ValueDisplay.PERCENTAGE);
	
	public final SliderSetting presencePenalty =
		new SliderSetting("Presence penalty",
			"Penalty for choosing tokens that already appear in the chat"
				+ " history.\n\n"
				+ "Positive values encourage the model to use synonyms and"
				+ " talk about different topics. Negative values encourage the"
				+ " model to repeat the same word over and over again.",
			0, -2, 2, 0.01, ValueDisplay.DECIMAL);
	
	public final SliderSetting frequencyPenalty =
		new SliderSetting("Frequency penalty",
			"Similar to presence penalty, but based on how often the token"
				+ " appears in the chat history.\n\n"
				+ "Positive values encourage the model to use synonyms and"
				+ " talk about different topics. Negative values encourage the"
				+ " model to repeat existing chat messages.",
			0, -2, 2, 0.01, ValueDisplay.DECIMAL);
	
	public final EnumSetting<StopSequence> stopSequence = new EnumSetting<>(
		"Stop sequence",
		"Controls how AutoComplete detects the end of a chat message.\n\n"
			+ "\u00a7lLine Break\u00a7r is the default value and is recommended"
			+ " for most language models.\n\n"
			+ "\u00a7lNext Message\u00a7r works better with certain"
			+ " code-optimized language models, which have a tendency to insert"
			+ " line breaks in the middle of a chat message.",
		StopSequence.values(), StopSequence.LINE_BREAK);
	
	public enum StopSequence
	{
		LINE_BREAK("Line Break", "\n"),
		NEXT_MESSAGE("Next Message", "\n<");
		
		private final String name;
		private final String sequence;
		
		private StopSequence(String name, String sequence)
		{
			this.name = name;
			this.sequence = sequence;
		}
		
		public String getSequence()
		{
			return sequence;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
	
	public final SliderSetting contextLength = new SliderSetting(
		"Context length",
		"Controls how many messages from the chat history are used to generate"
			+ " predictions.\n\n"
			+ "Higher values improve the quality of predictions, but also"
			+ " increase the time it takes to generate them, as well as cost"
			+ " (for APIs like OpenAI) or RAM usage (for self-hosted models).",
		10, 0, 100, 1, ValueDisplay.INTEGER);
	
	public final CheckboxSetting filterServerMessages =
		new CheckboxSetting("Filter server messages",
			"Only shows player-made chat messages to the model.\n\n"
				+ "This can help you save tokens and get more out of a low"
				+ " context length, but it also means that the model will have"
				+ " no idea about events like players joining, leaving, dying,"
				+ " etc.",
			false);
	
	public final TextFieldSetting customModel = new TextFieldSetting(
		"Custom model",
		"If set, this model will be used instead of the one specified in the"
			+ " \"OpenAI model\" setting.\n\n"
			+ "Use this if you have a fine-tuned OpenAI model or if you are"
			+ " using a custom endpoint that is OpenAI-compatible but offers"
			+ " different models.",
		"");
	
	public final EnumSetting<CustomModelType> customModelType =
		new EnumSetting<>("Custom model type", "Whether the custom"
			+ " model should use the chat endpoint or the legacy endpoint.\n\n"
			+ "If \"Custom model\" is left blank, this setting is ignored.",
			CustomModelType.values(), CustomModelType.CHAT);
	
	public enum CustomModelType
	{
		CHAT("Chat", true),
		LEGACY("Legacy", false);
		
		private final String name;
		private final boolean chat;
		
		private CustomModelType(String name, boolean chat)
		{
			this.name = name;
			this.chat = chat;
		}
		
		public boolean isChat()
		{
			return chat;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
	
	public final TextFieldSetting openaiChatEndpoint = new TextFieldSetting(
		"OpenAI chat endpoint", "Endpoint for OpenAI's chat completion API.",
		"https://api.openai.com/v1/chat/completions");
	
	public final TextFieldSetting openaiLegacyEndpoint =
		new TextFieldSetting("OpenAI legacy endpoint",
			"Endpoint for OpenAI's legacy completion API.",
			"https://api.openai.com/v1/completions");
	
	private final List<Setting> settings =
		Collections.unmodifiableList(Arrays.asList(openAiModel, maxTokens,
			temperature, topP, presencePenalty, frequencyPenalty, stopSequence,
			contextLength, filterServerMessages, customModel, customModelType,
			openaiChatEndpoint, openaiLegacyEndpoint));
	
	public void forEach(Consumer<Setting> action)
	{
		settings.forEach(action);
	}
}
