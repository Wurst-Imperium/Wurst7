/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings;

import java.util.function.Consumer;

import net.minecraft.util.math.Vec3d;
import net.wurstclient.WurstClient;
import net.wurstclient.util.RotationUtils;

public final class FaceTargetSetting
	extends EnumSetting<FaceTargetSetting.FaceTarget>
{
	private static final WurstClient WURST = WurstClient.INSTANCE;
	
	private FaceTargetSetting(String name, String description,
		FaceTarget[] values, FaceTarget selected)
	{
		super(name, description, values, selected);
	}
	
	public static FaceTargetSetting withoutPacketSpam(String description)
	{
		return withoutPacketSpam("Face target", description, FaceTarget.SERVER);
	}
	
	public static FaceTargetSetting withoutPacketSpam(String name,
		String description, FaceTarget selected)
	{
		FaceTarget[] values =
			{FaceTarget.OFF, FaceTarget.SERVER, FaceTarget.CLIENT};
		return new FaceTargetSetting(name, description, values, selected);
	}
	
	public static FaceTargetSetting withPacketSpam(String name,
		String description, FaceTarget selected)
	{
		return new FaceTargetSetting(name, description, FaceTarget.values(),
			selected);
	}
	
	public enum FaceTarget
	{
		OFF("Off", v -> {}),
		
		SERVER("Server-side",
			v -> WURST.getRotationFaker().faceVectorPacket(v)),
		
		CLIENT("Client-side",
			v -> WURST.getRotationFaker().faceVectorClient(v)),
		
		SPAM("Packet spam",
			v -> RotationUtils.getNeededRotations(v).sendPlayerLookPacket());
		
		private String name;
		private Consumer<Vec3d> face;
		
		private FaceTarget(String name, Consumer<Vec3d> face)
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
