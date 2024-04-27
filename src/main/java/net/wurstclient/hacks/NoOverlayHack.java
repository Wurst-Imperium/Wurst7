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
import net.wurstclient.SearchTags;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.SliderSetting;

@SearchTags({"no overlay", "NoWaterOverlay", "no water overlay",
	"no fire overlay", "no shield overlay"})
public final class NoOverlayHack extends Hack
{
	private final SliderSetting fireOffSet =
		new SliderSetting("Offset", "The amount to lower the fire overlay.",
			0.6, 0.01, 0.6, 0.01, SliderSetting.ValueDisplay.DECIMAL);
	
	public final SliderSetting blockingOffset =
		new SliderSetting("Blocking offset",
			"The amount to lower the shield overlay by when blocking.", 0.5, 0,
			0.8, 0.01, SliderSetting.ValueDisplay.DECIMAL);
	
	public final SliderSetting nonBlockingOffset =
		new SliderSetting("Non-blocking offset",
			"The amount to lower the shield overlay when not blocking.", 0.2, 0,
			0.5, 0.01, SliderSetting.ValueDisplay.DECIMAL);
	
	public NoOverlayHack()
	{
		super("NoOverlay");
		setCategory(Category.RENDER);
		addSetting(this.fireOffSet);
		addSetting(this.blockingOffset);
		addSetting(this.nonBlockingOffset);
	}
	
	// See CameraMixin.onGetSubmersionType() and
	// InGameOverlayRendererMixin.onRenderUnderwaterOverlay()
	
	public float getOverlayOffset()
	{
		return isEnabled() ? this.fireOffSet.getValueF() : 0;
	}
	
	public void adjustShieldPosition(MatrixStack matrixStack, boolean blocking)
	{
		if(!isEnabled())
			return;
		
		if(blocking)
			matrixStack.translate(0, -this.blockingOffset.getValue(), 0);
		else
			matrixStack.translate(0, -this.nonBlockingOffset.getValue(), 0);
	}
}
