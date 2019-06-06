/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;

public enum JsonUtils
{
	;
	
	public static final Gson GSON = new Gson();
	
	public static final Gson PRETTY_GSON =
		new GsonBuilder().setPrettyPrinting().create();
	
	public static final JsonParser JSON_PARSER = new JsonParser();
}
