package net.wurstclient.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;

public class GoogleTranslateService
{
	
	public static enum Language
	{
		
		AUTO_DETECT("AUTO_DETECT", "auto"),
		ARABIC("ARABIC", "ar"),
		CHINESE_SIMPLIFIED("CHINESE_SIMPLIFIED", "zh-CN"),
		CHINESE_TRADITIONAL("CHINESE_TRADITIONAL", "zh-TW"),
		ENGLISH("ENGLISH", "en"),
		FILIPINO("FILIPINO", "tl"),
		FRENCH("FRENCH", "fr"),
		GERMAN("GERMAN", "de"),
		GREEK("GREEK", "el"),
		INDONESIAN("INDONESIAN", "id"),
		IRISH("IRISH", "ga"),
		ITALIAN("ITALIAN", "it"),
		JAPANESE("JAPANESE", "ja"),
		JAVANESE("JAVANESE", "jw"),
		KOREAN("KOREAN", "ko"),
		LATIN("LATIN", "la"),
		POLISH("POLISH", "pl"),
		PORTUGUESE("PORTUGUESE", "pt"),
		RUSSIAN("RUSSIAN", "ru"),
		SPANISH("SPANISH", "es"),
		SWEDISH("SWEDISH", "sv"),
		THAI("THAI", "th"),
		VIETNAMESE("VIETNAMESE", "vi");
		
		public final String name;
		public final String value;
		
		private Language(String name, String value)
		{
			this.name = name;
			this.value = value;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
	
	public String translate(String textToTranslate, String translateFrom,
		String translateTo) throws Exception
	{
		try
		{
			String pageSource =
				getPageSource(textToTranslate, translateFrom, translateTo);
			final String regex = "class=\"t0\">([^<]*)<\\/div>";
			final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
			final Matcher matcher = pattern.matcher(pageSource);
			matcher.find();
			String match = matcher.group(1);
			if(match != null && !match.isEmpty() && !StringEscapeUtils
				.unescapeHtml4(match).contains(textToTranslate))
				return match;
			else
				return null;
		}catch(Exception e)
		{
			return null;
		}
	}
	
	private static String getPageSource(String textToTranslate,
		String translateFrom, String translateTo) throws Exception
	{
		String pageUrl = String.format(
			"https://translate.google.com/m?hl=en&sl=%s&tl=%s&ie=UTF-8&prev=_m&q=%s",
			translateFrom, translateTo,
			URLEncoder.encode(textToTranslate.trim(), "UTF-8"));
		URL url = new URL(pageUrl);
		HttpURLConnection connection = null;
		BufferedReader bufferedReader = null;
		StringBuilder pageSource = new StringBuilder();
		try
		{
			connection = (HttpURLConnection)url.openConnection();
			connection.setConnectTimeout(5000);
			connection.setRequestProperty("User-Agent",
				"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
			bufferedReader = new BufferedReader(
				new InputStreamReader(connection.getInputStream(), "UTF-8"));
			String line;
			while((line = bufferedReader.readLine()) != null)
				pageSource.append(line + System.lineSeparator());
			String clean = pageSource.toString();
			return clean;
		}catch(Exception e)
		{
			// e.printStackTrace();
		}finally
		{
			if(connection != null)
				connection.disconnect();
			if(bufferedReader != null)
				bufferedReader.close();
		}
		return null;
	}
	
}
