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
import net.wurstclient.hack.Hack;
import net.wurstclient.util.RotationUtils;
import net.wurstclient.util.text.WText;

public final class FaceTargetSetting
	extends EnumSetting<FaceTargetSetting.FaceTarget>
{
	private static final WurstClient WURST = WurstClient.INSTANCE;
	private static final WText FULL_DESCRIPTION_SUFFIX =
		buildDescriptionSuffix(true);
	private static final WText REDUCED_DESCRIPTION_SUFFIX =
		buildDescriptionSuffix(false);
	
	private FaceTargetSetting(WText description, FaceTarget[] values,
		FaceTarget selected)
	{
		super("Face target", description, values, selected);
	}
	
	public static FaceTargetSetting withPacketSpam(Hack hack,
		FaceTarget selected)
	{
		return withPacketSpam(hackDescription(hack), selected);
	}
	
	public static FaceTargetSetting withPacketSpam(WText description,
		FaceTarget selected)
	{
		return new FaceTargetSetting(
			description.append(FULL_DESCRIPTION_SUFFIX), FaceTarget.values(),
			selected);
	}
	
	public static FaceTargetSetting withoutPacketSpam(Hack hack,
		FaceTarget selected)
	{
		return withoutPacketSpam(hackDescription(hack), selected);
	}
	
	public static FaceTargetSetting withoutPacketSpam(WText description,
		FaceTarget selected)
	{
		FaceTarget[] values =
			{FaceTarget.OFF, FaceTarget.SERVER, FaceTarget.CLIENT};
		return new FaceTargetSetting(
			description.append(REDUCED_DESCRIPTION_SUFFIX), values, selected);
	}
	
	private static WText hackDescription(Hack hack)
	{
		return WText.translated("description.wurst.setting."
			+ hack.getName().toLowerCase() + ".face_target");
	}
	
	public void face(Vec3d v)
	{
		getSelected().face(v);
	}
	
	private static WText buildDescriptionSuffix(boolean includePacketSpam)
	{
		WText text = WText.literal("\n\n");
		FaceTarget[] values =
			includePacketSpam ? FaceTarget.values() : new FaceTarget[]{
				FaceTarget.OFF, FaceTarget.SERVER, FaceTarget.CLIENT};
		
		for(FaceTarget value : values)
			text.append("\u00a7l" + value.name + "\u00a7r - ")
				.append(value.description).append("\n\n");
		
		return text;
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
		
		private static final String TRANSLATION_KEY_PREFIX =
			"description.wurst.setting.generic.face_target.";
		
		private final String name;
		private final WText description;
		private final Consumer<Vec3d> face;
		
		private FaceTarget(String name, Consumer<Vec3d> face)
		{
			this.name = name;
			description =
				WText.translated(TRANSLATION_KEY_PREFIX + name().toLowerCase());
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
