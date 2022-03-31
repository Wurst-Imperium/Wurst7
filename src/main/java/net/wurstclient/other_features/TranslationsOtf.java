/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.other_features;

import net.wurstclient.DontBlock;
import net.wurstclient.SearchTags;
import net.wurstclient.other_feature.OtherFeature;
import net.wurstclient.settings.CheckboxSetting;

@SearchTags({"languages", "localizations", "localisations",
	"internationalization", "internationalisation", "i18n", "sprachen",
	"übersetzungen", "force english"})
@DontBlock
public final class TranslationsOtf extends OtherFeature
{
	private final CheckboxSetting forceEnglish =
		new CheckboxSetting("Force English",
			"Displays the Wurst Client in English,\n"
				+ "even if Minecraft is set to a\n" + "different language.",
			true);
	
	public TranslationsOtf()
	{
		super("Translations",
			"Localization settings.\n\n"
				+ "§cThis is an experimental feature!\n"
				+ "We don't have many translations yet. If you\n"
				+ "speak both English and some other language,\n"
				+ "please help us by adding more translations.");
		addSetting(forceEnglish);
	}
	
	public CheckboxSetting getForceEnglish()
	{
		return forceEnglish;
	}
}
