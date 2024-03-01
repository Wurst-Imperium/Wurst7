package net.wurstclient.settings.filters;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.Hoglin;

public class FilterHoglinsSetting extends EntityFilterCheckbox {
	public FilterHoglinsSetting(String description, boolean checked) {
		super("Filter hoglins", description, checked);
	}

	@Override
	public boolean test(Entity e) {
		return !(e instanceof Hoglin);
	}

	public static FilterHoglinsSetting genericCombat(boolean checked) {
		return new FilterHoglinsSetting(
			"Won't attack hoglins.", checked);
	}

	public static FilterHoglinsSetting genericVision(boolean checked) {
		return new FilterHoglinsSetting(
			"Won't show hoglins.", checked);
	}
}