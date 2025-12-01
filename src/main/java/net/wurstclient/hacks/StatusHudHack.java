/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.wurstclient.Category;
import net.wurstclient.WurstClient;
import net.wurstclient.events.GUIRenderListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

public final class StatusHudHack extends Hack implements GUIRenderListener
{
	private static final Minecraft MC = WurstClient.MC;
	private static final WurstClient WURST = WurstClient.INSTANCE;
	
	// Position
	private final SliderSetting x = new SliderSetting("X",
		"Horizontal HUD position.", 6, 0, 2000, 1, ValueDisplay.INTEGER);
	private final SliderSetting y = new SliderSetting("Y",
		"Vertical HUD position.", 6, 0, 2000, 1, ValueDisplay.INTEGER);
	
	// Layout
	private final SliderSetting tileSize = new SliderSetting("Tile size",
		"Square tile side length (px).", 22, 16, 32, 1, ValueDisplay.INTEGER);
	private final SliderSetting gap = new SliderSetting("Gap",
		"Space between tiles (px).", 4, 0, 16, 1, ValueDisplay.INTEGER);
	private final SliderSetting columns = new SliderSetting("Columns",
		"How many tiles per row.", 8, 1, 12, 1, ValueDisplay.INTEGER);
	private final CheckboxSetting showLabels =
		new CheckboxSetting("Show labels (tiny)",
			"Optional 2-letter labels; still compact.", false);
	
	// Which Mace hack the Mace tile reflects
	private enum MaceTileSource
	{
		AUTO_MACE("AutoMace"),
		AUTO_MACE_BLATANT("AutoMaceBlatant");
		
		private final String name;
		
		MaceTileSource(String n)
		{
			this.name = n;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
	
	private final EnumSetting<MaceTileSource> maceTileSource =
		new EnumSetting<>("Mace tile shows",
			"Choose which hack the Mace tile tracks.", MaceTileSource.values(),
			MaceTileSource.AUTO_MACE);
	
	public StatusHudHack()
	{
		super("StatusHUD");
		setCategory(Category.RENDER);
		addSetting(x);
		addSetting(y);
		addSetting(tileSize);
		addSetting(gap);
		addSetting(columns);
		addSetting(showLabels);
		addSetting(maceTileSource);
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(GUIRenderListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(GUIRenderListener.class, this);
	}
	
	@Override
	public void onRenderGUI(GuiGraphics ctx, float partialTicks)
	{
		// Mojmap: hideGui instead of hudHidden
		if(MC.player == null || MC.options == null || MC.options.hideGui)
			return;
		
		int baseX = x.getValueI();
		int baseY = y.getValueI();
		int size = tileSize.getValueI();
		int pad = Math.max(1, Math.min(3, size / 10));
		int g = gap.getValueI();
		int cols = Math.max(1, columns.getValueI());
		
		// Statuses
		boolean maceOn = false, preferElytra = false, preferFireworks = false;
		boolean eatOn = false, anchorOn = false, crystalOn = false,
			totemOn = false;
		boolean kaLegitOn = false;
		
		// Read from chosen Mace hack (supports both autoMaceHack and
		// AutoMaceBlatantHack)
		try
		{
			Object maceHack = null;
			switch(maceTileSource.getSelected())
			{
				case AUTO_MACE:
				maceHack = WURST.getHax().AutoMaceHack;
				break;
				case AUTO_MACE_BLATANT:
				maceHack = WURST.getHax().AutoMaceBlatantHack;
				break;
			}
			if(maceHack != null)
			{
				maceOn = safeBoolean(maceHack, "isEnabled", null, false);
				
				// Prefer Elytra / Fireworks — try method names first, then
				// fields
				preferElytra = safeBoolean(maceHack, "isPreferElytraAir",
					"preferElytraAir", false);
				preferFireworks = safeBoolean(maceHack, "isPreferFireworks",
					"preferFireworks", false);
			}
		}catch(Throwable ignored)
		{}
		
		try
		{
			var ae = WURST.getHax().autoEatHack;
			if(ae != null)
				eatOn = ae.isEnabled();
		}catch(Throwable ignored)
		{}
		
		try
		{
			var aa = WURST.getHax().anchorAuraHack;
			if(aa != null)
				anchorOn = aa.isEnabled();
		}catch(Throwable ignored)
		{}
		
		try
		{
			var ca = WURST.getHax().crystalAuraHack;
			if(ca != null)
				crystalOn = ca.isEnabled();
		}catch(Throwable ignored)
		{}
		
		try
		{
			var at = WURST.getHax().autoTotemHack;
			if(at != null)
				totemOn = at.isEnabled();
		}catch(Throwable ignored)
		{}
		
		try
		{
			var kal = WURST.getHax().killauraLegitHack;
			if(kal != null)
				kaLegitOn = kal.isEnabled();
		}catch(Throwable ignored)
		{}
		
		// Tiles
		Tile[] tiles = new Tile[]{
			new Tile(new ItemStack(Items.MACE), maceOn, "MC"),
			new Tile(new ItemStack(Items.ELYTRA), preferElytra, "EL"),
			new Tile(new ItemStack(Items.FIREWORK_ROCKET), preferFireworks,
				"FW"),
			new Tile(new ItemStack(Items.COOKED_BEEF), eatOn, "ET"),
			new Tile(new ItemStack(Items.RESPAWN_ANCHOR), anchorOn, "AN"),
			new Tile(new ItemStack(Items.END_CRYSTAL), crystalOn, "CR"),
			new Tile(new ItemStack(Items.TOTEM_OF_UNDYING), totemOn, "TT"),
			new Tile(new ItemStack(Items.NETHERITE_SWORD), kaLegitOn, "KL")};
		
		// Render grid
		int xIdx = 0, yIdx = 0;
		for(Tile t : tiles)
		{
			int drawX = baseX + xIdx * (size + g);
			int drawY = baseY + yIdx * (size + g);
			drawTile(ctx, drawX, drawY, size, pad, t);
			xIdx++;
			if(xIdx >= cols)
			{
				xIdx = 0;
				yIdx++;
			}
		}
	}
	
	// --- reflection helpers so we can read prefs from either hack variant ---
	private boolean safeBoolean(Object obj, String getterName, String fieldName,
		boolean defVal)
	{
		if(obj == null)
			return defVal;
		try
		{
			// Try no-arg boolean getter
			var m = obj.getClass().getMethod(getterName);
			Object v = m.invoke(obj);
			if(v instanceof Boolean)
				return (Boolean)v;
		}catch(Throwable ignored)
		{}
		if(fieldName != null)
		{
			try
			{
				var f = obj.getClass().getDeclaredField(fieldName);
				f.setAccessible(true);
				Object v = f.get(obj);
				if(v instanceof Boolean)
					return (Boolean)v;
			}catch(Throwable ignored)
			{}
		}
		return defVal;
	}
	
	private static final class Tile
	{
		final ItemStack icon;
		final boolean on;
		final String label2;
		
		Tile(ItemStack icon, boolean on, String label2)
		{
			this.icon = icon;
			this.on = on;
			this.label2 = label2;
		}
	}
	
	private void drawTile(GuiGraphics ctx, int x, int y, int size, int pad,
		Tile t)
	{
		int border = t.on ? 0xA000A040 /* green-ish */ : 0xA0404040 /* gray */;
		int fill = t.on ? 0x6000A040 : 0x50000000;
		
		ctx.fill(x, y, x + size, y + size, border);
		ctx.fill(x + 1, y + 1, x + size - 1, y + size - 1, fill);
		
		int iconX = x + (size - 16) / 2;
		int iconY = y + (size - 16) / 2;
		ctx.renderItem(t.icon, iconX, iconY);
		
		if(showLabels.isChecked() && MC.font != null)
		{
			int col = t.on ? 0xFFFFFF : 0xB0B0B0;
			String s = t.label2;
			int w = MC.font.width(s);
			// Mojmap: drawString(Font, String, x, y, color, dropShadow)
			ctx.drawString(MC.font, s, x + size - w - 2, y + size - 9, col,
				true);
		}
	}
}
