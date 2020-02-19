/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.text.Text;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.hack.DontSaveState;
import net.wurstclient.hack.Hack;

@SearchTags({"auto sign"})
@DontSaveState
public final class AutoSignHack extends Hack
{
	private Text[] signText;
	
	public AutoSignHack()
	{
		super("AutoSign",
			"Instantly writes whatever text you want on every sign\n"
				+ "you place. Once activated, you can write normally on\n"
				+ "the first sign to specify the text for all other signs.");
		setCategory(Category.BLOCKS);
	}
	
	@Override
	public void onDisable()
	{
		signText = null;
	}
	
	public Text[] getSignText()
	{
		return signText;
	}
	
	public void setSignText(Text[] signText)
	{
		if(isEnabled() && this.signText == null)
			this.signText = signText;
	}
}
