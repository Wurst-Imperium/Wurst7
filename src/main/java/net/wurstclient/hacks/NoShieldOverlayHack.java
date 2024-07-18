/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.util.math.MatrixStack;
import net.wurstclient.Category;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

public final class NoShieldOverlayHack extends Hack
{
	public final SliderSetting blockingOffset =
		new SliderSetting("Blocking offset",
			"The amount to lower the shield overlay by when blocking.", 0.5, 0,
			0.8, 0.01, ValueDisplay.DECIMAL);
	
	public final SliderSetting nonBlockingOffset =
		new SliderSetting("Non-blocking offset",
			"The amount to lower the shield overlay when not blocking.", 0.2, 0,
			0.5, 0.01, ValueDisplay.DECIMAL);
	
	public NoShieldOverlayHack()
	{
		super("NoShieldOverlay");
		setCategory(Category.RENDER);
		addSetting(blockingOffset);
		addSetting(nonBlockingOffset);
	}
	
	public void adjustShieldPosition(MatrixStack matrixStack, boolean blocking)
	{
		if(!isEnabled())
			return;
		
		if(blocking)
			matrixStack.translate(0, -blockingOffset.getValue(), 0);
		else
			matrixStack.translate(0, -nonBlockingOffset.getValue(), 0);
	}
	
	// See HeldItemRendererMixin
}
