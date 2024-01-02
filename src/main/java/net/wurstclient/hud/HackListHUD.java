/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hud;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
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
	
	public void render(DrawContext context, float partialTicks)
	{
		if(otf.getMode() == Mode.HIDDEN)
			return;
		
		if(otf.getPosition() == Position.LEFT
			&& WurstClient.INSTANCE.getOtfs().wurstLogoOtf.isVisible())
			posY = 22;
		else
			posY = 2;
		
		// color
		if(WurstClient.INSTANCE.getHax().rainbowUiHack.isEnabled())
		{
			float[] acColor = WurstClient.INSTANCE.getGui().getAcColor();
			textColor = 0x04 << 24 | (int)(acColor[0] * 256) << 16
				| (int)(acColor[1] * 256) << 8 | (int)(acColor[2] * 256);
			
		}else
			textColor = 0x04000000 | otf.getColor();
		
		int height = posY + activeHax.size() * 9;
		Window sr = WurstClient.MC.getWindow();
		
		if(otf.getMode() == Mode.COUNT || height > sr.getScaledHeight())
			drawCounter(context);
		else
			drawHackList(context, partialTicks);
	}
	
	private void drawCounter(DrawContext context)
	{
		long size = activeHax.stream().filter(e -> e.hack.isEnabled()).count();
		String s = size + " hack" + (size != 1 ? "s" : "") + " active";
		drawString(context, s);
	}
	
	private void drawHackList(DrawContext context, float partialTicks)
	{
		if(otf.isAnimations())
			for(HackListEntry e : activeHax)
				drawWithOffset(context, e, partialTicks);
		else
			for(HackListEntry e : activeHax)
				drawString(context, e.hack.getRenderName());
	}
	
	public void updateState(Hack hack)
	{
		int offset = otf.isAnimations() ? 4 : 0;
		HackListEntry entry = new HackListEntry(hack, offset);
		
		if(hack.isEnabled())
		{
			if(activeHax.contains(entry))
				return;
			
			activeHax.add(entry);
			sort();
			
		}else if(!otf.isAnimations())
			activeHax.remove(entry);
	}
	
	private void sort()
	{
		Comparator<HackListEntry> comparator =
			Comparator.comparing(hle -> hle.hack, otf.getComparator());
		Collections.sort(activeHax, comparator);
	}
	
	@Override
	public void onUpdate()
	{
		if(otf.shouldSort())
			sort();
		
		if(!otf.isAnimations())
			return;
		
		for(Iterator<HackListEntry> itr = activeHax.iterator(); itr.hasNext();)
		{
			HackListEntry e = itr.next();
			boolean enabled = e.hack.isEnabled();
			e.prevOffset = e.offset;
			
			if(enabled && e.offset > 0)
				e.offset--;
			else if(!enabled && e.offset < 4)
				e.offset++;
			else if(!enabled && e.offset >= 4)
				itr.remove();
		}
	}
	
	private void drawString(DrawContext context, String s)
	{
		TextRenderer tr = WurstClient.MC.textRenderer;
		int posX;
		
		if(otf.getPosition() == Position.LEFT)
			posX = 2;
		else
		{
			int screenWidth = WurstClient.MC.getWindow().getScaledWidth();
			int stringWidth = tr.getWidth(s);
			
			posX = screenWidth - stringWidth - 2;
		}
		
		context.drawText(tr, s, posX + 1, posY + 1, 0xff000000, false);
		context.drawText(tr, s, posX, posY, textColor | 0xff000000, false);
		
		posY += 9;
	}
	
	private void drawWithOffset(DrawContext context, HackListEntry e,
		float partialTicks)
	{
		TextRenderer tr = WurstClient.MC.textRenderer;
		String s = e.hack.getRenderName();
		
		float offset =
			e.offset * partialTicks + e.prevOffset * (1 - partialTicks);
		
		float posX;
		if(otf.getPosition() == Position.LEFT)
			posX = 2 - 5 * offset;
		else
		{
			int screenWidth = WurstClient.MC.getWindow().getScaledWidth();
			int stringWidth = tr.getWidth(s);
			
			posX = screenWidth - stringWidth - 2 + 5 * offset;
		}
		
		int alpha = (int)(255 * (1 - offset / 4)) << 24;
		context.drawText(tr, s, (int)posX + 1, posY + 1, 0x04000000 | alpha,
			false);
		context.drawText(tr, s, (int)posX, posY, textColor | alpha, false);
		
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
		
		@Override
		public boolean equals(Object obj)
		{
			// do not use Java 16 syntax here,
			// it breaks Eclipse's Clean Up feature
			if(!(obj instanceof HackListEntry))
				return false;
			
			HackListEntry other = (HackListEntry)obj;
			return hack == other.hack;
		}
		
		@Override
		public int hashCode()
		{
			return hack.hashCode();
		}
	}
}
