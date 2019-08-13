/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hud;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.Window;
import net.wurstclient.WurstClient;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.other_features.HackListOtf;
import net.wurstclient.other_features.HackListOtf.Mode;
import net.wurstclient.other_features.HackListOtf.Position;

public final class HackListHUD implements UpdateListener
{
	private final ArrayList<HackListEntry> activeHax = new ArrayList<>();
	private final HackListOtf otf = WurstClient.INSTANCE.getOtfs().hackListOtf;
	private int posY;
	private int textColor;
	
	public HackListHUD()
	{
		WurstClient.INSTANCE.getEventManager().add(UpdateListener.class, this);
	}
	
	public void render(float partialTicks)
	{
		if(otf.getMode() == Mode.HIDDEN)
			return;
		
		if(otf.getPosition() == Position.RIGHT)
			posY = 0;
		else
			posY = 22;
			
		// color
		// if(WurstClient.INSTANCE.getHax().rainbowUiHack.isActive())
		// {
		// float[] acColor = WurstClient.INSTANCE.getGui().getAcColor();
		// textColor = 0x04 << 24 | (int)(acColor[0] * 256) << 16
		// | (int)(acColor[1] * 256) << 8 | (int)(acColor[2] * 256);
		// }else
		textColor = 0x04ffffff;
		
		// YesCheat+ mode indicator
		// YesCheatSpf yesCheatSpf = WurstClient.INSTANCE.special.yesCheatSpf;
		// if(yesCheatSpf.modeIndicator.isChecked())
		// drawString("YesCheat+: " + yesCheatSpf.getProfile().getName());
		
		int height = posY + activeHax.size() * 9;
		Window sr = WurstClient.MC.window;
		
		if(otf.getMode() == Mode.COUNT || height > sr.getScaledHeight())
			// draw counter
			drawString(activeHax.size() == 1 ? "1 mod active"
				: activeHax.size() + " mods active");
		
		else if(otf.isAnimations())
			// draw mod list
			for(HackListEntry e : activeHax)
				drawWithOffset(e, partialTicks);
		else
			for(HackListEntry e : activeHax)
				drawString(e.hack.getRenderName());
	}
	
	public void updateState(Hack mod)
	{
		if(mod.isEnabled())
		{
			for(HackListEntry e : activeHax)
				if(e.hack == mod)
					return;
				
			activeHax.add(new HackListEntry(mod, 4));
			activeHax.sort(Comparator.comparing(e -> e.hack.getName()));
			
		}else if(!otf.isAnimations())
			activeHax.removeIf(e -> e.hack == mod);
	}
	
	@Override
	public void onUpdate()
	{
		if(!otf.isAnimations())
			return;
		
		for(Iterator<HackListEntry> itr = activeHax.iterator(); itr.hasNext();)
		{
			HackListEntry e = itr.next();
			
			if(e.hack.isEnabled())
			{
				e.prevOffset = e.offset;
				if(e.offset > 0)
					e.offset--;
				
			}else if(!e.hack.isEnabled() && e.offset < 4)
			{
				e.prevOffset = e.offset;
				e.offset++;
				
			}else if(!e.hack.isEnabled() && e.offset == 4)
				itr.remove();
		}
	}
	
	private void drawString(String s)
	{
		TextRenderer tr = WurstClient.MC.textRenderer;
		Window sr = WurstClient.MC.window;
		int posX;
		
		if(otf.getPosition() == Position.RIGHT)
			posX = sr.getScaledWidth() - tr.getStringWidth(s) - 2;
		else
			posX = 2;
		
		tr.draw(s, posX + 1, posY + 1, 0xff000000);
		tr.draw(s, posX, posY, textColor | 0xff000000);
		
		posY += 9;
	}
	
	private void drawWithOffset(HackListEntry e, float partialTicks)
	{
		TextRenderer tr = WurstClient.MC.textRenderer;
		Window sr = WurstClient.MC.window;
		
		String s = e.hack.getRenderName();
		float offset =
			e.offset * partialTicks + e.prevOffset * (1 - partialTicks);
		
		float posX;
		if(otf.getPosition() == Position.RIGHT)
			posX = sr.getScaledWidth() - tr.getStringWidth(s) - 2 + 5 * offset;
		else
			posX = 2 - 5 * offset;
		
		int alpha = (int)(255 * (1 - offset / 4)) << 24;
		tr.draw(s, posX + 1, posY + 1, 0x04000000 | alpha);
		tr.draw(s, posX, posY, textColor | alpha);
		
		posY += 9;
	}
	
	private static final class HackListEntry
	{
		private final Hack hack;
		private int offset;
		private int prevOffset;
		
		public HackListEntry(Hack mod, int offset)
		{
			hack = mod;
			this.offset = offset;
			prevOffset = offset;
		}
	}
}
