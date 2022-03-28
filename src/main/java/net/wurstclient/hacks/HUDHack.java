package net.wurstclient.hacks;

import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;

@SearchTags({"HUD"})
public final class HUDHack extends Hack 
{

	public HUDHack() {
		super("HUD");
		setCategory(Category.OTHER);
		CheckboxSetting fps = new CheckboxSetting("FPS", "Display your current FPS", true);
		addSetting(fps);
		CheckboxSetting speed = new CheckboxSetting("Speed", "Display your current speed", true);
		addSetting(speed);
		CheckboxSetting coords = new CheckboxSetting("Coordinates", "Display your current coordinates", true);
		addSetting(coords);
		CheckboxSetting dim_coord = new CheckboxSetting("Dimension Coordinates", "Display your Nether coordinates in the Owerworld or your Owerworld coordinates in the Nether", true);
		addSetting(dim_coord);
	}
}