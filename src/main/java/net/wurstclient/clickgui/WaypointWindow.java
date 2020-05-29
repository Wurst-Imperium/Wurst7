package net.wurstclient.clickgui;

import java.util.ArrayList;
import java.util.Objects;

public class WaypointWindow extends Window
{
    private static WaypointWindow INSTANCE = null;

    public WaypointWindow(String title)
    {
        super(title);
        INSTANCE = this;
        this.scrollingEnabled = true;
    }

    public final void repopulate(ArrayList<Component> components) {
        children.clear();
        components.forEach(c -> this.add(c));
        invalidate();
    }

    public static WaypointWindow getWindow() {
        if(INSTANCE == null)
            return new WaypointWindow("Waypoints");
        else
            return Objects.requireNonNull(INSTANCE);
    }
}
