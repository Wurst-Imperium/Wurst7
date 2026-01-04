/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.chattranslator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum GoogleTranslate
{
	;
	
	private static final HashMap<Character, String> simplifyMap;
	static
	{
		simplifyMap = new HashMap<>();
		simplifyMap.put(' ', "");
		simplifyMap.put('\r', "");
		simplifyMap.put('\n', "");
		simplifyMap.put('\t', "");
		simplifyMap.put('ä', "a");
		simplifyMap.put('ö', "o");
		simplifyMap.put('ü', "u");
		simplifyMap.put('á', "a");
		simplifyMap.put('é', "e");
		simplifyMap.put('í', "i");
		simplifyMap.put('ó', "o");
		simplifyMap.put('ú', "u");
		simplifyMap.put('à', "a");
		simplifyMap.put('è', "e");
		simplifyMap.put('ì', "i");
		simplifyMap.put('ò', "o");
		simplifyMap.put('ù', "u");
		simplifyMap.put('â', "a");
		simplifyMap.put('ê', "e");
		simplifyMap.put('î', "i");
		simplifyMap.put('ô', "o");
		simplifyMap.put('û', "u");
		simplifyMap.put('ã', "a");
		simplifyMap.put('õ', "o");
		simplifyMap.put('ñ', "n");
		simplifyMap.put('ç', "c");
	}
	
	public static String translate(String text, String langFrom, String langTo)
	{
		String html = getHTML(text, langFrom, langTo);
		String translated = parseHTML(html);
		
		// Return null if Google Translate just returned the original text,
		// ignoring capitalization changes, whitespace, and broken characters
		if(simplify(text).equals(simplify(translated)))
			return null;
		
		return translated;
	}
	
	private static String getHTML(String text, String langFrom, String langTo)
	{
		URL url = createURL(text, langFrom, langTo);
		
		try
		{
			URLConnection connection = setupConnection(url);
			
			try(BufferedReader br = new BufferedReader(
				new InputStreamReader(connection.getInputStream(), "UTF-8")))
			{
				StringBuilder html = new StringBuilder();
				
				String line;
				while((line = br.readLine()) != null)
					html.append(line + "\n");
				
				return html.toString();
			}
			
		}catch(IOException e)
		{
			return null;
		}
	}
	
	private static URL createURL(String text, String langFrom, String langTo)
	{
		try
		{
			String encodedText = URLEncoder.encode(text.trim(), "UTF-8");
			
			String urlString = String.format(
				"https://translate.google.com/m?hl=en&sl=%s&tl=%s&ie=UTF-8&prev=_m&q=%s",
				langFrom, langTo, encodedText);
			
			return URI.create(urlString).toURL();
			
		}catch(MalformedURLException | UnsupportedEncodingException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	private static URLConnection setupConnection(URL url) throws IOException
	{
		URLConnection connection = url.openConnection();
		
		connection.setConnectTimeout(5000);
		connection.setRequestProperty("User-Agent",
			"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
		
		return connection;
	}
	
	@SuppressWarnings("deprecation")
	private static String parseHTML(String html)
	{
		String regex = "class=\"result-container\">([^<]*)<\\/div>";
		Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
		
		Matcher matcher = pattern.matcher(html);
		if(!matcher.find())
			return null;
		
		String match = matcher.group(1);
		if(match == null || match.isEmpty())
			return null;
			
		// deprecated in favor of org.apache.commons.text.StringEscapeUtils,
		// which isn't bundled with Minecraft
		return org.apache.commons.lang3.StringEscapeUtils.unescapeHtml4(match);
	}
	
	private static String simplify(String text)
	{
		StringBuilder sb = new StringBuilder();
		for(char c : text.toLowerCase().toCharArray())
			sb.append(simplifyMap.getOrDefault(c, String.valueOf(c)));
		
		return sb.toString();
	}
}
