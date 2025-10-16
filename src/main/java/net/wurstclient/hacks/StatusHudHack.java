/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import net.wurstclient.Category;
import net.wurstclient.WurstClient;
import net.wurstclient.events.GUIRenderListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

public final class StatusHudHack extends Hack implements GUIRenderListener
{
	private static final MinecraftClient MC = WurstClient.MC;
	private static final WurstClient WURST = WurstClient.INSTANCE;
	
	private final SliderSetting x = new SliderSetting("X",
		"Horizontal HUD position.", 6, 0, 500, 1, ValueDisplay.INTEGER);
	private final SliderSetting y = new SliderSetting("Y",
		"Vertical HUD position.", 6, 0, 500, 1, ValueDisplay.INTEGER);
	
	public StatusHudHack()
	{
		super("StatusHUD");
		setCategory(Category.RENDER);
		addSetting(x);
		addSetting(y);
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
	public void onRenderGUI(DrawContext context, float partialTicks)
	{
		if(MC.player == null || MC.options == null || MC.options.hudHidden)
			return;
		
		int baseX = x.getValueI();
		int baseY = y.getValueI();
		int offX = 0;
		
		boolean maceOn = false;
		boolean preferElytra = false;
		boolean preferFireworks = false;
		boolean eatOn = false;
		
		try
		{
			AutoMaceHack am = WURST.getHax().autoMaceHack;
			if(am != null)
			{
				maceOn = am.isEnabled();
				preferElytra = am.isPreferElytraAir();
				preferFireworks = am.isPreferFireworks(); // NEW
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
		
		// AutoMace tile
		drawIconTile(context, baseX + offX, baseY, maceOn);
		context.drawItem(new ItemStack(Items.MACE), baseX + offX + 2,
			baseY + 2);
		context.drawTextWithShadow(MC.textRenderer,
			maceOn ? "Mace:ON" : "Mace:OFF", baseX + offX + 22, baseY + 6,
			0xFFFFFF);
		offX += 90;
		
		// Prefer Elytra/Chest tile
		drawIconTile(context, baseX + offX, baseY, preferElytra);
		context.drawItem(new ItemStack(Items.ELYTRA), baseX + offX + 2,
			baseY + 2);
		context.drawTextWithShadow(MC.textRenderer,
			preferElytra ? "Elytra" : "Chest", baseX + offX + 22, baseY + 6,
			0xFFFFFF);
		offX += 90;
		
		// Prefer Fireworks tile (NEW)
		drawIconTile(context, baseX + offX, baseY, preferFireworks);
		context.drawItem(new ItemStack(Items.FIREWORK_ROCKET), baseX + offX + 2,
			baseY + 2);
		context.drawTextWithShadow(MC.textRenderer,
			preferFireworks ? "Rockets" : "NoRkt", baseX + offX + 22, baseY + 6,
			0xFFFFFF);
		offX += 90;
		
		// AutoEat tile
		drawIconTile(context, baseX + offX, baseY, eatOn);
		context.drawItem(new ItemStack(Items.COOKED_BEEF), baseX + offX + 2,
			baseY + 2);
		context.drawTextWithShadow(MC.textRenderer,
			eatOn ? "Eat:ON" : "Eat:OFF", baseX + offX + 22, baseY + 6,
			0xFFFFFF);
	}
	
	private void drawIconTile(DrawContext ctx, int x, int y,
		boolean highlighted)
	{
		int bg = highlighted ? 0xA0008020 : 0xA0000000;
		int border = 0x40000000;
		ctx.fill(x, y, x + 84, y + 20, border);
		ctx.fill(x + 1, y + 1, x + 83, y + 19, bg);
	}
}
