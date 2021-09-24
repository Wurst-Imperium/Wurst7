/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.other_features;

import net.wurstclient.DontBlock;
import net.wurstclient.SearchTags;
import net.wurstclient.other_feature.OtherFeature;

@SearchTags({"last server"})
@DontBlock
public final class LastServerOtf extends OtherFeature
{
	public LastServerOtf()
	{
		super("LastServer",
			"Wurst 在服务器选择屏幕上添加了一个“上一个服务器”按钮，该按钮会自动将您带回上次玩的服务器.\n\n"
				+ "当你被踢和/或有很多服务器时很有用.");
	}
}
