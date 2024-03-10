package net.wurstclient;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import com.google.gson.*;

import net.wurstclient.util.json.JsonException;
import net.wurstclient.util.json.JsonUtils;

public final class LocalesGenerator
{
	public static final class Settings
	{
		public static final String DEFAULT_UNTRANSLATED_VALUE =
			"WURST_UNTRANSLATED_STRING";
		public static final String DEFAULT_LC = "en_us";
		public static final List<String> SUPPORTED_LCS =
			Arrays.asList("cs_cz", "de_de", "fr_fr", "it_it", "ja_jp", "ko_kr",
				"pl_pl", "ro_ro", "ru_ru", "uk_ua", "zh_cn", "zh_hk", "zh_tw");
	}
	
	private static final Path DEFAULT_LC_PATH =
		Paths.get(System.getProperty("user.dir"), "src", "main", "resources",
			"assets", "wurst", "lang");
	private static final File DEFAULT_LC_FILE = new File(DEFAULT_LC_PATH
		.resolve(Settings.DEFAULT_LC + ".json").toAbsolutePath().toString());
	
	private static void writeLinesToFile(File f, List<String> l)
		throws IOException
	{
		try(FileWriter fw = new FileWriter(f, false))
		{
			fw.write(String.join("\n", l));
		}
	}
	
	public static void main(String[] args) throws IOException, JsonException
	{
		if(!DEFAULT_LC_FILE.isFile())
			throw new FileNotFoundException(DEFAULT_LC_FILE.getAbsolutePath());
		
		ArrayList<Integer> blankLines = new ArrayList<>();
		Scanner sc = new Scanner(DEFAULT_LC_FILE);
		for(int l = 0; sc.hasNextLine(); l++)
			if(sc.nextLine().isBlank())
				blankLines.add(l);
			
		JsonObject defaultJson =
			JsonUtils.parseFile(DEFAULT_LC_FILE.toPath()).getAsJsonObject();
		
		for(String lc : Settings.SUPPORTED_LCS)
		{
			File f = new File(DEFAULT_LC_PATH.resolve(lc + ".json")
				.toAbsolutePath().toString());
			JsonObject defaultJsonCopy = defaultJson.deepCopy();
			
			if(!f.isFile())
			{
				defaultJsonCopy.keySet().forEach(k -> defaultJsonCopy
					.addProperty(k, Settings.DEFAULT_UNTRANSLATED_VALUE));
				
				String prettyJson = new GsonBuilder().setPrettyPrinting()
					.disableHtmlEscaping().create().toJson(defaultJsonCopy);
				List<String> lines =
					new ArrayList<>(prettyJson.lines().toList());
				blankLines.forEach(blank -> lines.add(blank, ""));
				
				writeLinesToFile(f, lines);
				
				System.out.printf(
					"%s.json: new locale file was created with default values set to %s%n",
					lc, Settings.DEFAULT_UNTRANSLATED_VALUE);
			}else
			{
				JsonObject parsedJson =
					JsonUtils.parseFile(f.toPath()).getAsJsonObject();
				JsonObject filledJson = new JsonObject();
				
				ArrayList<String> defaultKeySet =
					new ArrayList<>(defaultJsonCopy.keySet().stream().toList());
				ArrayList<String> keySet =
					new ArrayList<>(parsedJson.keySet().stream().toList());
				
				ArrayList<String> missingKeys =
					defaultKeySet.stream().filter(k -> !keySet.contains(k))
						.collect(Collectors.toCollection(ArrayList::new));
				ArrayList<String> unknownKeys =
					keySet.stream().filter(k -> !defaultKeySet.contains(k))
						.collect(Collectors.toCollection(ArrayList::new));
				
				boolean missingKeysStatus = false, unknownKeysStatus = false;
				
				if(!missingKeys.isEmpty())
				{
					missingKeysStatus = true;
					missingKeys.forEach(k -> parsedJson.addProperty(k,
						Settings.DEFAULT_UNTRANSLATED_VALUE));
				}
				
				defaultKeySet.forEach(k -> filledJson.addProperty(k,
					parsedJson.asMap().get(k).getAsString()));
				
				if(!unknownKeys.isEmpty())
				{
					unknownKeysStatus = true;
					unknownKeys.forEach(k -> filledJson.addProperty(k,
						parsedJson.asMap().get(k).getAsString()));
				}
				
				String prettyJson = new GsonBuilder().setPrettyPrinting()
					.disableHtmlEscaping().create().toJson(filledJson);
				List<String> lines =
					new ArrayList<>(prettyJson.lines().toList());
				
				ArrayList<Integer> finalBlankLines =
					new ArrayList<>(blankLines);
				
				if(!unknownKeys.isEmpty())
				{
					// "-1" means the last line before line with closing curved
					// bracket, i.e. latest JSON key-value pair
					int unknownValuesSeparatorIndex = lines.size()
						+ blankLines.size() - unknownKeys.size() - 1;
					for(int i = 0; i < 3; i++)
						finalBlankLines.add(unknownValuesSeparatorIndex + i);
				}
				
				finalBlankLines.forEach(lineIndex -> lines.add(lineIndex, ""));
				
				writeLinesToFile(f, lines);
				
				System.out.printf(
					"%s.json: missing keys: %b, unknown keys: %b%n", lc,
					missingKeysStatus, unknownKeysStatus);
				if(missingKeysStatus)
					System.out.printf(
						"\tmissing keys (were added to locale file with default values set to %s): %n\t\t%s%n",
						Settings.DEFAULT_UNTRANSLATED_VALUE,
						String.join(String.format("%n\t\t"), missingKeys));
				if(unknownKeysStatus)
					System.out.printf(
						"\tunknown keys (were added to the end of locale file): %n\t\t%s%n",
						String.join(String.format("%n\t\t"), unknownKeys));
			}
		}
	}
}
