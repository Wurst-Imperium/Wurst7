/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings;

import java.util.function.Consumer;

import net.minecraft.util.math.Vec3d;
import net.wurstclient.WurstClient;

public final class FacingSetting extends EnumSetting<FacingSetting.Facing>
{
	protected static final WurstClient WURST = WurstClient.INSTANCE;
	
	public FacingSetting(String description)
	{
		super("Facing", description, Facing.values(), Facing.SERVER);
	}
	
	public FacingSetting(String description, Facing selected)
	{
		super("Facing", description, Facing.values(), selected);
	}
	
	public FacingSetting(String name, String description, Facing selected)
	{
		super(name, description, Facing.values(), selected);
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
