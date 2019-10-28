package net.wurstclient.util;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.minecraft.util.math.Vec3d;

/**
 * The path a projectile will take through the air. This consists solely of vectors depicting the coordinates the projectile will likely pass through.
 * Positions are in standard Minecraft coordinates.
 * @author Admin
 *
 */
public class TrajectoryPath implements Iterable<Vec3d> {
	private final List<Vec3d> points = new LinkedList<Vec3d>();
	
	public void addPoint(Vec3d point)
	{
		points.add(point);
	}
	
	public boolean isEmpty()
	{
		return points.size() == 0;
	}

	@Override
	public Iterator<Vec3d> iterator() {
		return points.iterator();
	}
	
	/**
	 * Returns pairs of numbers in the form (x, y).
	 * @return
	 */
	public String plotX()
	{
		StringBuilder sb = new StringBuilder();
		for (Vec3d point : points)
		{
			sb.append("(" + point.x + "," + point.y + "),");
		}
		if (points.size() > 0) sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}
	
	/**
	 * Returns pairs of numbers in the form (z, y).
	 * @return
	 */
	public String plotZ()
	{
		StringBuilder sb = new StringBuilder();
		for (Vec3d point : points)
		{
			sb.append("(" + point.z + "," + point.y + "),");
		}
		if (points.size() > 0) sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}
}
