/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
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
		"OpenAI model",
		"The model to use for OpenAI API calls.\nRecommended models:\n\n"
			+ "\u00a7lGPT-4o-2024-05-13\u00a7r is the world's smartest model at"
			+ " the time of writing and will often produce the best completions."
			+ " However, it's meant to be an assistant rather than an"
			+ " auto-completion system, so you will see it produce some odd"
			+ " completions at times.\n\n"
			+ "\u00a7lGPT-3.5-Turbo-Instruct\u00a7r is an older, non-chat model"
			+ " based on GPT-3.5 that works well for auto-completion tasks.",
		OpenAiModel.values(), OpenAiModel.GPT_4O_2024_05_13);
	
	public enum OpenAiModel
	{
		GPT_4O_2024_05_13("gpt-4o-2024-05-13", true),
		GPT_4_TURBO_2024_04_09("gpt-4-turbo-2024-04-09", true),
		GPT_4_0125_PREVIEW("gpt-4-0125-preview", true),
		GPT_4_1106_PREVIEW("gpt-4-1106-preview", true),
		GPT_4_0613("gpt-4-0613", true),
		GPT_4_0314("gpt-4-0314", true),
		GPT_4_32K_0613("gpt-4-32k-0613", true),
		GPT_3_5_TURBO_0125("gpt-3.5-turbo-0125", true),
		GPT_3_5_TURBO_1106("gpt-3.5-turbo-1106", true),
		GPT_3_5_TURBO_0613("gpt-3.5-turbo-0613", true),
		GPT_3_5_TURBO_16K_0613("gpt-3.5-turbo-16k-0613", true),
		GPT_3_5_TURBO_0301("gpt-3.5-turbo-0301", true),
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
		0.7, 0, 2, 0.01, ValueDisplay.DECIMAL);
	
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
				+ " model to repeat the same word over and over again.\n\n"
				+ "Only works with OpenAI models.",
			0, -2, 2, 0.01, ValueDisplay.DECIMAL);
	
	public final SliderSetting frequencyPenalty =
		new SliderSetting("Frequency penalty",
			"Similar to presence penalty, but based on how often the token"
				+ " appears in the chat history.\n\n"
				+ "Positive values encourage the model to use synonyms and"
				+ " talk about different topics. Negative values encourage the"
				+ " model to repeat existing chat messages.\n\n"
				+ "Only works with OpenAI models.",
			0.6, -2, 2, 0.01, ValueDisplay.DECIMAL);
	
	public final SliderSetting repetitionPenalty =
		new SliderSetting("Repetition penalty",
			"Similar to presence penalty, but uses a different algorithm.\n\n"
				+ "1.0 means no penalty, negative values are not possible and"
				+ " 1.5 is the maximum value.\n\n"
				+ "Only works with the oobabooga web UI.",
			1, 1, 1.5, 0.01, ValueDisplay.DECIMAL);
	
	public final SliderSetting encoderRepetitionPenalty =
		new SliderSetting("Encoder repetition penalty",
			"Similar to frequency penalty, but uses a different algorithm.\n\n"
				+ "1.0 means no penalty, 0.8 behaves like a negative value and"
				+ " 1.5 is the maximum value.\n\n"
				+ "Only works with the oobabooga web UI.",
			1, 0.8, 1.5, 0.01, ValueDisplay.DECIMAL);
	
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
			+ " (for OpenAI API users) or RAM usage (for oobabooga users).",
		10, 0, 100, 1, ValueDisplay.INTEGER);
	
	public final CheckboxSetting filterServerMessages =
		new CheckboxSetting("Filter server messages",
			"Only shows player-made chat messages to the model.\n\n"
				+ "This can help you save tokens and get more out of a low"
				+ " context length, but it also means that the model will have"
				+ " no idea about events like players joining, leaving, dying,"
				+ " etc.",
			false);
	
	public final TextFieldSetting openaiChatEndpoint = new TextFieldSetting(
		"OpenAI chat endpoint", "Endpoint for OpenAI's chat completion API.",
		"https://api.openai.com/v1/chat/completions");
	
	public final TextFieldSetting openaiLegacyEndpoint =
		new TextFieldSetting("OpenAI legacy endpoint",
			"Endpoint for OpenAI's legacy completion API.",
			"https://api.openai.com/v1/completions");
	
	public final TextFieldSetting oobaboogaEndpoint =
		new TextFieldSetting("Oobabooga endpoint",
			"Endpoint for your Oobabooga web UI instance.\n"
				+ "Remember to start the Oobabooga server with the"
				+ " \u00a7e--extensions api\u00a7r flag.",
			"http://127.0.0.1:5000/api/v1/generate");
	
	private final List<Setting> settings =
		Collections.unmodifiableList(Arrays.asList(openAiModel, maxTokens,
			temperature, topP, presencePenalty, frequencyPenalty,
			repetitionPenalty, encoderRepetitionPenalty, stopSequence,
			contextLength, filterServerMessages, openaiChatEndpoint,
			openaiLegacyEndpoint, oobaboogaEndpoint));
	
	public void forEach(Consumer<Setting> action)
	{
		settings.forEach(action);
	}
}
