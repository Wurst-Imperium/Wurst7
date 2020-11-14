/*
 * MIT License
 *
 * Copyright (c) 2019 Fudge
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * https://github.com/natanfudge/Not-Enough-Crashes
 */
package net.wurstclient.sentry;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;
import com.google.common.net.UrlEscapers;

import net.fabricmc.mapping.reader.v2.MappingGetter;
import net.fabricmc.mapping.reader.v2.TinyMetadata;
import net.fabricmc.mapping.reader.v2.TinyV2Factory;
import net.fabricmc.mapping.reader.v2.TinyVisitor;
import net.minecraft.MinecraftVersion;

public final class StacktraceDeobfuscator
{
	
	private static final String MAPPINGS_JAR_LOCATION =
		"mappings/mappings.tiny";
	private static final String NAMESPACE_FROM = "intermediary";
	private static final String NAMESPACE_TO = "named";
	private static final Path CACHED_MAPPINGS = NotEnoughCrashes.DIRECTORY
		.resolve("mappings-" + MinecraftVersion.create().getName() + ".tiny");
	
	private static Map<String, String> mappings = null;
	
	private static void downloadAndCacheMappings()
	{
		String yarnVersion;
		try
		{
			yarnVersion = YarnVersion.getLatestBuildForCurrentVersion();
		}catch(IOException e)
		{
			NotEnoughCrashes.LOGGER
				.error("Could not get latest yarn build for version", e);
			return;
		}
		
		NotEnoughCrashes.LOGGER.info("Downloading deobfuscation mappings: "
			+ yarnVersion + " for the first launch");
		
		String encodedYarnVersion =
			UrlEscapers.urlFragmentEscaper().escape(yarnVersion);
		// Download V2 jar
		String artifactUrl = "https://maven.fabricmc.net/net/fabricmc/yarn/"
			+ encodedYarnVersion + "/yarn-" + encodedYarnVersion + "-v2.jar";
		
		try
		{
			Files.createDirectories(NotEnoughCrashes.DIRECTORY);
		}catch(IOException e)
		{
			NotEnoughCrashes.LOGGER.error(
				"Could not create " + NotEnoughCrashes.NAME + " directory!", e);
			return;
		}
		
		File jarFile =
			NotEnoughCrashes.DIRECTORY.resolve("yarn-mappings.jar").toFile();
		jarFile.deleteOnExit();
		try
		{
			FileUtils.copyURLToFile(new URL(artifactUrl), jarFile);
		}catch(IOException e)
		{
			NotEnoughCrashes.LOGGER.error("Failed to downloads mappings!", e);
			return;
		}
		
		try(FileSystem jar = FileSystems.newFileSystem(jarFile.toPath(), null))
		{
			NotEnoughCrashes.ensureDirectoryExists();
			Files.copy(jar.getPath(MAPPINGS_JAR_LOCATION), CACHED_MAPPINGS,
				StandardCopyOption.REPLACE_EXISTING);
		}catch(IOException e)
		{
			NotEnoughCrashes.LOGGER.error("Failed to extract mappings!", e);
		}
	}
	
	public static void init()
	{
		if(!Files.exists(CACHED_MAPPINGS))
			downloadAndCacheMappings();
	}
	
	private static void loadMappings()
	{
		if(!Files.exists(CACHED_MAPPINGS))
		{
			NotEnoughCrashes.LOGGER.warn(
				"Could not download mappings, stack trace won't be deobfuscated");
			return;
		}
		
		Map<String, String> mappings = new HashMap<>();
		
		try(BufferedReader mappingReader =
			Files.newBufferedReader(CACHED_MAPPINGS))
		{
			TinyV2Factory.visit(mappingReader, new TinyVisitor()
			{
				private final Map<String, Integer> namespaceStringToColumn =
					new HashMap<>();
				
				private void addMappings(MappingGetter name)
				{
					mappings.put(
						name.get(namespaceStringToColumn.get(NAMESPACE_FROM))
							.replace('/', '.'),
						name.get(namespaceStringToColumn.get(NAMESPACE_TO))
							.replace('/', '.'));
				}
				
				@Override
				public void start(TinyMetadata metadata)
				{
					namespaceStringToColumn.put(NAMESPACE_FROM,
						metadata.index(NAMESPACE_FROM));
					namespaceStringToColumn.put(NAMESPACE_TO,
						metadata.index(NAMESPACE_TO));
				}
				
				@Override
				public void pushClass(MappingGetter name)
				{
					addMappings(name);
				}
				
				@Override
				public void pushMethod(MappingGetter name, String descriptor)
				{
					addMappings(name);
				}
				
				@Override
				public void pushField(MappingGetter name, String descriptor)
				{
					addMappings(name);
				}
			});
			
		}catch(IOException e)
		{
			NotEnoughCrashes.LOGGER.error("Could not load mappings", e);
		}
		
		StacktraceDeobfuscator.mappings = mappings;
	}
	
	public static String deobfuscateClass(String input)
	{
		if(mappings == null)
			loadMappings();
		if(mappings == null)
			return input;
		
		String output = mappings.get(input);
		return output != null ? output : input;
	}
	
	public static void deobfuscateThrowable(Throwable t)
	{
		Deque<Throwable> queue = new ArrayDeque<>();
		queue.add(t);
		boolean firstLoop = true;
		while(!queue.isEmpty())
		{
			t = queue.remove();
			t.setStackTrace(
				deobfuscateStacktrace(t.getStackTrace(), firstLoop));
			if(t.getCause() != null)
				queue.add(t.getCause());
			Collections.addAll(queue, t.getSuppressed());
			
			firstLoop = false;
		}
	}
	
	private static final List<String> filteredClasses = Arrays.asList(
		"io.github.giantnuker.fabric.loadcatcher.EntrypointCatcher$LoaderClientReplacement",
		"io.github.giantnuker.fabric.loadcatcher.EntrypointCatcher");
	
	// No need to insert multiple watermarks in one exception
	public static StackTraceElement[] deobfuscateStacktrace(
		StackTraceElement[] stackTrace, boolean insertWatermark)
	{
		if(stackTrace.length == 0)
			return stackTrace;
		
		// Make the stack trace nicer by removing entrypoint catcher's cruft
		List<StackTraceElement> stackTraceList =
			NotEnoughCrashes.FILTER_ENTRYPOINT_CATCHER
				? Arrays.stream(stackTrace)
					.filter(element -> !filteredClasses
						.contains(element.getClassName()))
					.collect(Collectors.toList())
				: Lists.newArrayList(stackTrace);
		if(NotEnoughCrashes.ENABLE_DEOBF
			// Check it wasn't deobfuscated already. This can happen when this
			// is called both by DeobfuscatingRewritePolicy
			// and then CrashReport mixin. They don't cover all cases alone
			// though so we need both.
			&& !StringUtils.startsWith(stackTrace[0].getClassName(),
				NotEnoughCrashes.NAME))
		{
			if(mappings == null)
				loadMappings();
			if(mappings == null)
				return stackTrace;
				
			// Removed the watermark because it confuses Sentry.
			// if(insertWatermark)
			// {
			// try
			// {
			// stackTraceList.add(0, new StackTraceElement(
			// NotEnoughCrashes.NAME + " deobfuscated stack trace", "",
			// YarnVersion.getLatestBuildForCurrentVersion(), -1));
			// }catch(IOException e)
			// {
			// NotEnoughCrashes.LOGGER
			// .error("Could not get used yarn version", e);
			// return stackTrace;
			// }
			// }
			
			int index = 0;
			
			for(StackTraceElement el : stackTraceList)
			{
				String remappedClass = mappings.get(el.getClassName());
				String remappedMethod = mappings.get(el.getMethodName());
				stackTraceList.set(index, new StackTraceElement(
					remappedClass != null ? remappedClass : el.getClassName(),
					remappedMethod != null ? remappedMethod
						: el.getMethodName(),
					remappedClass != null ? getFileName(remappedClass)
						: el.getFileName(),
					el.getLineNumber()));
				index++;
			}
		}
		
		return stackTraceList.toArray(new StackTraceElement[]{});
	}
	
	private static String getFileName(String className)
	{
		if(className.isEmpty())
			return className;
		
		int lastDot = className.lastIndexOf('.');
		if(lastDot != -1)
			className = className.substring(lastDot + 1);
		
		return className.split("\\$", 2)[0];
	}
	
	// For testing
	public static void main(String[] args)
	{
		init();
		for(Map.Entry<String, String> entry : mappings.entrySet())
			System.out.println(entry.getKey() + " <=> " + entry.getValue());
	}
}
