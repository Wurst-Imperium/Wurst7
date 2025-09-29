/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.ai;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public final class OpenAIClient
{
	private static final HttpClient HTTP = HttpClient.newHttpClient();
	private static final String URL =
		"https://api.openai.com/v1/chat/completions";
	private static final String MODEL = "gpt-4o-mini"; // change if needed
	
	public static String completeChat(String apiKey, String prompt)
		throws IOException, InterruptedException
	{
		// Minimal JSON string (avoid external JSON libs if you prefer)
		String body =
			"""
				{
				"model": "%s",
				"messages": [
					{"role":"system","content":"You are a helpful assistant in a Minecraft chat. Keep replies short."},
					{"role":"user","content":%s}
				],
				"temperature": 0.7,
				"max_tokens": 180
				}
				"""
				.formatted(MODEL, jsonEscape(prompt));
		
		HttpRequest req = HttpRequest.newBuilder(URI.create(URL))
			.header("Authorization", "Bearer " + apiKey)
			.header("Content-Type", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString(body)).build();
		
		HttpResponse<String> res =
			HTTP.send(req, HttpResponse.BodyHandlers.ofString());
		
		if(res.statusCode() / 100 != 2)
		{
			System.err
				.println("[ChatGptResponder] OpenAI HTTP " + res.statusCode());
			System.err.println("[ChatGptResponder] Body: " + res.body());
			return null;
		}
		
		// Naive extraction of the first message.content (avoid adding Gson)
		// It’s safe enough for our tiny use-case; if you want, you can wire
		// Gson later.
		String json = res.body();
		int i = json.indexOf("\"message\"");
		if(i < 0)
			return null;
		i = json.indexOf("\"content\"", i);
		if(i < 0)
			return null;
		i = json.indexOf(':', i);
		if(i < 0)
			return null;
		int start = json.indexOf('"', i + 1);
		if(start < 0)
			return null;
		StringBuilder out = new StringBuilder();
		boolean esc = false;
		for(int k = start + 1; k < json.length(); k++)
		{
			char c = json.charAt(k);
			if(esc)
			{
				// basic unescape for \" and \\ and \n
				if(c == '"' || c == '\\')
					out.append(c);
				else if(c == 'n')
					out.append(' ');
				else
					out.append(c);
				esc = false;
			}else if(c == '\\')
			{
				esc = true;
			}else if(c == '"')
			{
				break;
			}else
			{
				out.append(c);
			}
		}
		String content = out.toString().trim();
		return content.isEmpty() ? null : content;
	}
	
	private static String jsonEscape(String s)
	{
		StringBuilder sb = new StringBuilder("\"");
		for(int i = 0; i < s.length(); i++)
		{
			char c = s.charAt(i);
			switch(c)
			{
				case '"' -> sb.append("\\\"");
				case '\\' -> sb.append("\\\\");
				case '\n' -> sb.append("\\n");
				case '\r' -> sb.append("\\r");
				case '\t' -> sb.append("\\t");
				default ->
				{
					if(c < 0x20)
						sb.append(' ');
					else
						sb.append(c);
				}
			}
		}
		sb.append('"');
		return sb.toString();
	}
	
	private OpenAIClient()
	{}
}
