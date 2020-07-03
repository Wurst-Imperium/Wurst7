package net.wurstclient.clickgui.components;

import net.minecraft.client.util.math.MatrixStack;
import net.wurstclient.clickgui.ClickGui;
import net.wurstclient.clickgui.Component;
import net.wurstclient.clickgui.WaypointWindow;
import net.wurstclient.clickgui.Window;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.WaypointsHack;
import net.wurstclient.waypoints.Waypoint;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import org.lwjgl.opengl.GL11;

public class WaypointComponent extends Component
{
    private final Waypoint point;
    private final WaypointsHack waypoints = WurstClient.INSTANCE.getHax().waypointsHack;
    private final ClickGui GUI = WurstClient.INSTANCE.getGui();
    private final MinecraftClient MC = WurstClient.MC;
    private final boolean hasSettings = false;

    public WaypointComponent(Waypoint point) {
        this.point = point;
        setWidth(getDefaultWidth());
        setHeight(getDefaultHeight());
    }

    @Override
    public void handleMouseClick(double mouseX, double mouseY, int mouseButton) {
        if(mouseButton == 0)
            waypoints.selectedWaypoint = this.point;
        if(mouseButton == 1) {
            waypoints.removeWaypoint(point);
        }
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
        int x1 = getX();
        int x2 = x1 + getWidth();
        int x3 = hasSettings ? x2 - 11 : x2;
        int y1 = getY();
        int y2 = y1 + getHeight();

        boolean hovering = isHovering(mouseX, mouseY, x1, x2, y1, y2);
        boolean hHack = hovering && mouseX < x3;
        boolean hSettings = hovering && mouseX >= x3;

        drawButtonBackground(x1, x3, y1, y2, hHack);

        drawOutline(x1, x2, y1, y2);

        drawName(matrixStack, x1, x3, y1);

    }

    private boolean isHovering(int mouseX, int mouseY, int x1, int x2, int y1, int y2) {
        WaypointWindow parent = (WaypointWindow)getParent();
        boolean scrollEnabled = parent.isScrollingEnabled();
        int scroll = scrollEnabled ? parent.getScrollOffset() : 0;

        return mouseX >= x1 && mouseY >= y1 && mouseX < x2 && mouseY < y2
                && mouseY >= -scroll && mouseY < parent.getHeight() - 13 - scroll;
    }

    private void drawButtonBackground(int x1, int x3, int y1, int y2,
                                      boolean hPoint)
    {
        float[] bgColor = GUI.getBgColor();
        float opacity = GUI.getOpacity();

        GL11.glBegin(GL11.GL_QUADS);

        if(waypoints.selectedWaypoint == this.point) {
            GL11.glColor4f(0, 1, 0, hPoint ? opacity * 1.5F : opacity);
        }
        else
            GL11.glColor4f(bgColor[0], bgColor[1], bgColor[2],
                    hPoint ? opacity * 1.5F : opacity);

        GL11.glVertex2i(x1, y1);
        GL11.glVertex2i(x1, y2);
        GL11.glVertex2i(x3, y2);
        GL11.glVertex2i(x3, y1);

        GL11.glEnd();
    }

    private void drawOutline(int x1, int x2, int y1, int y2)
    {
        float[] acColor = GUI.getAcColor();

        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glColor4f(acColor[0], acColor[1], acColor[2], 0.5F);
        GL11.glVertex2i(x1, y1);
        GL11.glVertex2i(x1, y2);
        GL11.glVertex2i(x2, y2);
        GL11.glVertex2i(x2, y1);
        GL11.glEnd();
    }

    private void drawSeparator(int x3, int y1, int y2)
    {
        // separator
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2i(x3, y1);
        GL11.glVertex2i(x3, y2);
        GL11.glEnd();
    }

    private void drawName(MatrixStack matrixStack, int x1, int x3, int y1)
    {
        GL11.glColor4f(1, 1, 1, 1);
        GL11.glEnable(GL11.GL_TEXTURE_2D);

        TextRenderer tr = MC.textRenderer;
        String name = point.getName();
        int nameWidth = tr.getWidth(name);
        int tx = x1 + (x3 - x1 - nameWidth) / 2;
        int ty = y1 + 2;

        tr.draw(matrixStack, name, tx, ty, 0xF0F0F0);

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
    }

    @Override
    public int getDefaultWidth()
    {
        String name = point.getName();
        TextRenderer tr = WurstClient.MC.textRenderer;
        int width = tr.getWidth(name) + 4;
        return width;
    }

    @Override
    public int getDefaultHeight()
    {
        return 11;
    }
}
