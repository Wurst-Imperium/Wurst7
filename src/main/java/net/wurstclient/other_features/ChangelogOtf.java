/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.other_features;

import net.minecraft.util.Util;
import net.wurstclient.SearchTags;
import net.wurstclient.WurstClient;
import net.wurstclient.other_feature.OtherFeature;
import net.wurstclient.update.Version;

@SearchTags({"change log", "wurst update", "release notes", "what's new",
	"what is new", "new features", "recently added features"})
public final class ChangelogOtf extends OtherFeature
{
	public ChangelogOtf()
	{
		super("Changelog", "Opens the changelog in your browser.");
	}
	
	@Override
	public String getPrimaryAction()
	{
		return "View Changelog";
	}
	
	@Override
	public void doPrimaryAction()
	{
		String link = new Version(WurstClient.VERSION).getChangelogLink();
		Util.getOperatingSystem().open(link);
	}
}
