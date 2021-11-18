/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.util.math.MatrixStack;
import net.wurstclient.Category;
import net.wurstclient.events.PacketInputListener;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.BlockMatchHack;
import net.wurstclient.settings.BlockSetting;

public final class SearchHack extends BlockMatchHack
	implements UpdateListener, PacketInputListener, RenderListener
{
	private final BlockSetting block = new BlockSetting("Block",
		"The type of block to search for.", "minecraft:diamond_ore", this::reset, false);
	
	public SearchHack()
	{
		super("Search");
		setCategory(Category.RENDER);
		addSetting(block);
	}
	
	@Override
	public String getRenderName()
	{
		return getName() + " [" + block.getBlockName().replace("minecraft:", "")
			+ "]";
	}
	
	@Override
	public void onEnable()
	{
		super.onEnable();

		setBlockMatcher(b -> b == block.getBlock());
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
		float[] rainbow = WURST.getGui().getRainbowColor();
		render(matrixStack, rainbow[0], rainbow[1], rainbow[2], 0.5F);
	}
}
