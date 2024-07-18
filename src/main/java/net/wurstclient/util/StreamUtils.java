/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public enum StreamUtils
{
	;
	
	public static ArrayList<String> readAllLines(InputStream input)
		throws IOException
	{
		try(BufferedReader br =
			new BufferedReader(new InputStreamReader(input)))
		{
			ArrayList<String> lines = new ArrayList<>();
			String line;
			
			while((line = br.readLine()) != null)
				lines.add(line);
			
			return lines;
		}
	}
}
