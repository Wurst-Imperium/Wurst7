/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.autolibrarian;

import java.util.function.Consumer;

import net.minecraft.util.math.Vec3d;
import net.wurstclient.WurstClient;
import net.wurstclient.settings.EnumSetting;

public final class FacingSetting extends EnumSetting<FacingSetting.Facing>
{
	protected static final WurstClient WURST = WurstClient.INSTANCE;
	
	public FacingSetting()
	{
		super("Facing", "How to face the villager and job site.\n\n"
			+ "\u00a7lOff\u00a7r - Don't face the villager at all. Will be"
			+ " detected by anti-cheat plugins.\n\n"
			+ "\u00a7lServer-side\u00a7r - Face the villager on the"
			+ " server-side, while still letting you move the camera freely on"
			+ " the client-side.\n\n"
			+ "\u00a7lClient-side\u00a7r - Face the villager by moving your"
			+ " camera on the client-side. This is the most legit option, but"
			+ " can be disorienting to look at.", Facing.values(),
			Facing.SERVER);
	}
	
	public enum Facing
	{
		OFF("Off", v -> {}),
		
		SERVER("Server-side",
			v -> WURST.getRotationFaker().faceVectorPacket(v)),
		
		CLIENT("Client-side",
			v -> WURST.getRotationFaker().faceVectorClient(v));
		
		private String name;
		private Consumer<Vec3d> face;
		
		private Facing(String name, Consumer<Vec3d> face)
		{
			this.name = name;
			this.face = face;
		}
		
		public void face(Vec3d v)
		{
			face.accept(v);
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}
