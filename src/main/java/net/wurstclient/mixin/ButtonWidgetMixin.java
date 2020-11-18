/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.AbstractPressableButtonWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.wurstclient.WurstClient;

@Mixin(ButtonWidget.class)
public abstract class ButtonWidgetMixin extends AbstractPressableButtonWidget
{
	public ButtonWidgetMixin(WurstClient wurst, int i, int j, int k, int l,
		Text text)
	{
		super(i, j, k, l, text);
	}
	
	@Override
	protected void renderBg(MatrixStack matrixStack, MinecraftClient client,
		int mouseX, int mouseY)
	{
		int i = getYImage(isHovered());
		drawTexture(matrixStack, x, y, 0, 46 + i * 20, width / 2, height / 2);
		drawTexture(matrixStack, x + width / 2, y, 200 - width / 2, 46 + i * 20,
			width / 2, height / 2);
		drawTexture(matrixStack, x, y + height / 2, 0,
			46 + i * 20 + 20 - height / 2, width / 2, height / 2);
		drawTexture(matrixStack, x + width / 2, y + height / 2, 200 - width / 2,
			46 + i * 20 + 20 - height / 2, width / 2, height / 2);
	}
}
