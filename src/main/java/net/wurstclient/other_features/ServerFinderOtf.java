/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.other_features;

import net.wurstclient.SearchTags;
import net.wurstclient.other_feature.OtherFeature;

@SearchTags({"Server Finder"})
public final class ServerFinderOtf extends OtherFeature
{
	public ServerFinderOtf()
	{
		super("ServerFinder",
			"Allows you to find easy-to-grief Minecraft servers quickly\n"
				+ "and easily. To use it, press the 'Server Finder' button\n"
				+ "on the server selection screen.");
	}
}
