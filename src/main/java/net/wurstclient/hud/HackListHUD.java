/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hud;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
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
	
	public void render(MatrixStack matrixStack, float partialTicks)
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
			textColor = 0x04ffffff;
		
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		
		// YesCheat+ mode indicator
		// YesCheatSpf yesCheatSpf = WurstClient.INSTANCE.special.yesCheatSpf;
		// if(yesCheatSpf.modeIndicator.isChecked())
		// drawString("YesCheat+: " + yesCheatSpf.getProfile().getName());
		
		int height = posY + activeHax.size() * 9;
		Window sr = WurstClient.MC.getWindow();
		
		if(otf.getMode() == Mode.COUNT || height > sr.getScaledHeight())
			drawCounter(matrixStack);
		else
			drawHackList(matrixStack, partialTicks);
	}
	
	private void drawCounter(MatrixStack matrixStack)
	{
		long size = activeHax.stream().filter(e -> e.hack.isEnabled()).count();
		String s = size + " hack" + (size != 1 ? "s" : "") + " active";
		drawString(matrixStack, s);
	}
	
	private void drawHackList(MatrixStack matrixStack, float partialTicks)
	{
		if(otf.isAnimations())
			for(HackListEntry e : activeHax)
				drawWithOffset(matrixStack, e, partialTicks);
		else
			for(HackListEntry e : activeHax)
				drawString(matrixStack, e.hack.getRenderName());
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
			Collections.sort(activeHax);
			
		}else if(!otf.isAnimations())
			activeHax.remove(entry);
	}
	
	@Override
	public void onUpdate()
	{
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
	
	private void drawString(MatrixStack matrixStack, String s)
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
		
		tr.draw(matrixStack, s, posX + 1, posY + 1, 0xff000000);
		tr.draw(matrixStack, s, posX, posY, textColor | 0xff000000);
		
		posY += 9;
	}
	
	private void drawWithOffset(MatrixStack matrixStack, HackListEntry e,
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
		tr.draw(matrixStack, s, posX + 1, posY + 1, 0x04000000 | alpha);
		tr.draw(matrixStack, s, posX, posY, textColor | alpha);
		
		posY += 9;
	}
	
	private static final class HackListEntry
		implements Comparable<HackListEntry>
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
		public int compareTo(HackListEntry o)
		{
			return hack.getRenderName()
				.compareToIgnoreCase(o.hack.getRenderName());
		}
		
		@Override
		public boolean equals(Object obj)
		{
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
