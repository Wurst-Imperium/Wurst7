package net.wurstclient.util;

import net.minecraft.entity.LivingEntity;
import net.wurstclient.util.TrajectoryRenderer.Style;

/**
 * Data structure to hold request for rendering trajectory for a single entity's weapon, in a single frame.
 * @author Admin
 *
 */
public class TrajectoryRenderPost {
	public LivingEntity entity;
	Style style;
	public long defaultcolor;
	long bvrcolor;
	Long interceptcolor;
	
	public TrajectoryRenderPost(LivingEntity entity, Style style, long defaultcolor, long bvrcolor, Long interceptcolor)
	{
		this.entity = entity;
		this.style = style;
		this.defaultcolor = defaultcolor;
		this.bvrcolor = bvrcolor;
		this.interceptcolor = interceptcolor;
	}
}
