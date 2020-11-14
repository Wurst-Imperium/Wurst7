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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.fabricmc.loader.api.FabricLoader;

public class NotEnoughCrashes
{
	public static final Path DIRECTORY = Paths.get(
		FabricLoader.getInstance().getGameDir().toAbsolutePath().toString(),
		"not-enough-crashes");
	public static final String NAME = "Not Enough Crashes (Wurst)";
	
	public static final Logger LOGGER = LogManager.getLogger(NAME);
	
	public static final boolean FILTER_ENTRYPOINT_CATCHER = true;
	
	// No need to deobf in dev
	public static final boolean ENABLE_DEOBF =
		!FabricLoader.getInstance().isDevelopmentEnvironment();
	
	public static final boolean ENABLE_ENTRYPOINT_CATCHING =
		!FabricLoader.getInstance().isDevelopmentEnvironment();
	
	public static void ensureDirectoryExists() throws IOException
	{
		Files.createDirectories(DIRECTORY);
	}
	
	public void initStacktraceDeobfuscator()
	{
		if(!ENABLE_DEOBF)
			return;
		
		LOGGER.info("Initializing StacktraceDeobfuscator");
		
		try
		{
			StacktraceDeobfuscator.init();
			
		}catch(Exception e)
		{
			LOGGER.error("Failed to load mappings!", e);
		}
		
		LOGGER.info("Done initializing StacktraceDeobfuscator");
	}
}
