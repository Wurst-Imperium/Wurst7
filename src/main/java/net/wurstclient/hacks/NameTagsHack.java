/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
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
import net.wurstclient.settings.SliderSetting;

@SearchTags({"name tags"})
public final class NameTagsHack extends Hack
{
	private final SliderSetting scale =
		new SliderSetting("Scale", "How large the nametags should be.", 1, 0.05,
			5, 0.05, SliderSetting.ValueDisplay.PERCENTAGE);
	
	private final CheckboxSetting unlimitedRange =
		new CheckboxSetting("Unlimited range",
			"Removes the 64 block distance limit for nametags.", true);
	
	private final CheckboxSetting seeThrough = new CheckboxSetting(
		"See-through mode",
		"Renders nametags on the see-through text layer. This makes them"
			+ " easier to read behind walls, but causes some graphical glitches"
			+ " with water and other transparent things.",
		false);
	
	private final CheckboxSetting forceMobNametags = new CheckboxSetting(
		"Always show named mobs", "Displays the nametags of named mobs even"
			+ " when you are not looking directly at them.",
		true);
	
	private final CheckboxSetting forcePlayerNametags =
		new CheckboxSetting("Always show player names",
			"Displays your own nametag as well as any player names that would"
				+ " normally be disabled by scoreboard team settings.",
			false);
	
	public NameTagsHack()
	{
		super("NameTags");
		setCategory(Category.RENDER);
		addSetting(scale);
		addSetting(unlimitedRange);
		addSetting(seeThrough);
		addSetting(forceMobNametags);
		addSetting(forcePlayerNametags);
	}
	
	public float getScale()
	{
		return scale.getValueF();
	}
	
	public boolean isUnlimitedRange()
	{
		return isEnabled() && unlimitedRange.isChecked();
	}
	
	public boolean isSeeThrough()
	{
		return isEnabled() && seeThrough.isChecked();
	}
	
	public boolean shouldForceMobNametags()
	{
		return isEnabled() && forceMobNametags.isChecked();
	}
	
	public boolean shouldForcePlayerNametags()
	{
		return isEnabled() && forcePlayerNametags.isChecked();
	}
	
	// See EntityRendererMixin.wurstRenderLabelIfPresent(),
	// LivingEntityRendererMixin, MobEntityRendererMixin
}
