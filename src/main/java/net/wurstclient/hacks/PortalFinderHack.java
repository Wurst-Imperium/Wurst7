/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.block.Block;
import net.minecraft.client.util.math.MatrixStack;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.BlockMatchHack;
import net.wurstclient.util.BlockUtils;

@SearchTags({"portal finder"})
public final class PortalFinderHack extends BlockMatchHack
	implements UpdateListener, RenderListener
{
	public PortalFinderHack()
	{
		super("PortalFinder");
		setCategory(Category.RENDER);
		Block portal = BlockUtils.getBlockFromName("minecraft:nether_portal");
		setBlockMatcher(b -> b == portal);
		setDisplayStyle(DisplayStyle.BOTH);
	}

	@Override
	public void onEnable()
	{
		super.onEnable();

		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(RenderListener.class, this);
	}

	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(RenderListener.class, this);

		super.onDisable();
	}

	@Override
	public void onUpdate()
	{
		updateSearch();
	}

	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		render(matrixStack, 0.9F, 0.15F, 1.F, 0.5F);
	}
}
