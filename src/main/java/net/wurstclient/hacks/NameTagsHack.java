/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
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
	private final CheckboxSetting unlimitedRange =
		new CheckboxSetting("Unlimited range",
			"Removes the 64 block distance limit for nametags.", true);
	
	private final CheckboxSetting seeThrough = new CheckboxSetting(
		"See-through mode",
		"Renders nametags on the see-through text layer. This makes them"
			+ " easier to read behind walls.",
		true);
	
	private final CheckboxSetting forceNametags = new CheckboxSetting(
		"Force nametags",
		"Forces nametags of all players to be visible, even your own.", false);
	
	private final CheckboxSetting forceMobNametags = new CheckboxSetting(
		"Force mob nametags",
		"Forces nametags of all mobs to be visible.", false);
	
	public NameTagsHack()
	{
		super("NameTags");
		setCategory(Category.RENDER);
		addSetting(unlimitedRange);
		addSetting(seeThrough);
		addSetting(forceNametags);
		addSetting(forceMobNametags);
	}
	
	public boolean isUnlimitedRange()
	{
		return isEnabled() && unlimitedRange.isChecked();
	}
	
	public boolean isSeeThrough()
	{
		return isEnabled() && seeThrough.isChecked();
	}
	
	public boolean shouldForceNametags()
	{
		return isEnabled() && forceNametags.isChecked();
	}
	
	public boolean shouldForceMobNametags()
	{
		return isEnabled() && forceMobNametags.isChecked();
	}
	
	// See LivingEntityRendererMixin, MobEntityRendererMixin and
	// EntityRendererMixin.wurstRenderLabelIfPresent()
}
