/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.other_features;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Button.OnPress;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.wurstclient.DontBlock;
import net.wurstclient.SearchTags;
import net.wurstclient.other_feature.OtherFeature;
import net.wurstclient.settings.EnumSetting;

@SearchTags({"wurst options", "settings"})
@DontBlock
public final class WurstOptionsOtf extends OtherFeature
{
	private static final Identifier WURST_TEXTURE =
		Identifier.fromNamespaceAndPath("wurst", "wurst_128.png");
	
	private final EnumSetting<Location> location = new EnumSetting<>("Location",
		"description.wurst.setting.wurstoptions.location", Location.values(),
		Location.GAME_MENU);
	
	public WurstOptionsOtf()
	{
		super("WurstOptions", "description.wurst.other_feature.wurstoptions");
		addSetting(location);
	}
	
	public boolean isVisibleInGameMenu()
	{
		return WURST.isEnabled()
			&& location.getSelected() == Location.GAME_MENU;
	}
	
	public boolean isVisibleInStatistics()
	{
		return WURST.isEnabled()
			&& location.getSelected() == Location.STATISTICS;
	}
	
	public Button.Builder buttonBuilder(OnPress onPress)
	{
		MutableComponent message = Component.literal("            Options");
		
		MutableComponent narration =
			Component.translatable("gui.narrate.button", "Wurst Options");
		
		Tooltip tooltip = Tooltip.create(Component.literal(getDescription()));
		
		return Button.builder(message, onPress)
			.createNarration(sup -> narration).tooltip(tooltip);
	}
	
	public void drawWurstLogoOnButton(GuiGraphics context,
		Button wurstOptionsButton)
	{
		if(wurstOptionsButton == null)
			return;
		
		int x = wurstOptionsButton.getX() + 34;
		int y = wurstOptionsButton.getY() + 2;
		int w = 63;
		int h = 16;
		int fw = 63;
		int fh = 16;
		float u = 0;
		float v = 0;
		context.guiRenderState.up();
		context.blit(RenderPipelines.GUI_TEXTURED, WURST_TEXTURE, x, y, u, v, w,
			h, fw, fh);
	}
	
	private enum Location
	{
		GAME_MENU("Game Menu"),
		STATISTICS("Statistics");
		
		private final String name;
		
		private Location(String name)
		{
			this.name = name;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}
