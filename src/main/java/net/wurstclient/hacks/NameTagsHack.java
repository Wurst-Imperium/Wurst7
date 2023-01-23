/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;

@SearchTags({"name tags"})
public final class NameTagsHack extends Hack
{
	private final CheckboxSetting alwaysVisible = new CheckboxSetting(
		"Always See NameTags", false);	
	private final CheckboxSetting unlimited = new CheckboxSetting(
		"Unlimited Range Nametags", false);

	public NameTagsHack()
	{
		super("NameTags");
		setCategory(Category.RENDER);
		addSetting(alwaysVisible);	
		addSetting(unlimited);
	}

	public boolean alwaysVisibleNametags()
	{
		return isEnabled() && alwaysVisible.isChecked();
	}

	public boolean isUnlimitedRange()
	{
		return isEnabled() && unlimited.isChecked();
	}
	
	// See EntityRendererMixin.wurstRenderLabelIfPresent()
}
