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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;

import com.google.gson.Gson;

import net.minecraft.MinecraftVersion;

public class YarnVersion
{
	public String gameVersion;
	public String separator;
	public int build;
	public String maven;
	public String version;
	public boolean stable;
	
	private static final String YARN_API_ENTRYPOINT =
		"https://meta.fabricmc.net/v2/versions/yarn/"
			+ MinecraftVersion.create().getName();
	private static final Path VERSION_FILE =
		NotEnoughCrashes.DIRECTORY.resolve("yarn-version.txt");
	private static String versionMemCache = null;
	
	public static String getLatestBuildForCurrentVersion() throws IOException
	{
		if(versionMemCache == null)
			if(!Files.exists(VERSION_FILE))
			{
				URL url = new URL(YARN_API_ENTRYPOINT);
				URLConnection request = url.openConnection();
				request.connect();
				
				YarnVersion[] versions = new Gson().fromJson(
					new InputStreamReader((InputStream)request.getContent()),
					YarnVersion[].class);
				String version = Arrays.stream(versions)
					.max(Comparator.comparingInt(v -> v.build)).get().version;
				NotEnoughCrashes.ensureDirectoryExists();
				Files.write(VERSION_FILE, version.getBytes());
				versionMemCache = version;
			}else
				versionMemCache = new String(Files.readAllBytes(VERSION_FILE));
			
		return versionMemCache;
	}
}
