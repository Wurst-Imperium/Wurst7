package net.wurstclient.clickgui.components;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;
import net.wurstclient.WurstClient;
import net.wurstclient.clickgui.ClickGui;
import net.wurstclient.clickgui.Component;
import net.wurstclient.clickgui.Window;
import net.wurstclient.clickgui.screens.EditColorScreen;
import net.wurstclient.settings.ColorSetting;
import org.lwjgl.opengl.GL11;

public final class ColorComponent extends Component
{
    private final MinecraftClient MC = WurstClient.MC;
    private final ClickGui GUI = WurstClient.INSTANCE.getGui();
    private final ColorSetting setting;

    public ColorComponent(ColorSetting setting)
    {
        this.setting = setting;

        this.setWidth(this.getDefaultWidth());
        this.setHeight(this.getDefaultHeight());
    }

    @Override
    public void handleMouseClick(double mouseX, double mouseY, int mouseButton)
    {
        MC.setScreen(new EditColorScreen(MC.currentScreen, setting));
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
        int x1 = getX();
        int x2 = x1 + getWidth();
        int y1 = getY();
        int y2 = y1 + getHeight();
        int y3 = y1 + Math.floorDiv(getHeight(), 2);

        boolean hovering = isHovering(mouseX, mouseY, x1, x2, y1, y2);
        RenderSystem.setShader(GameRenderer::getPositionShader);

        if (hovering)
        {
            if (mouseY <= y3)
            {
                setTooltip(setting.getDescription());
            } else
            {
                setTooltip("§e[left-click]§r to edit color");
            }
        }

        this.drawBackground(matrixStack, x1, x2, y1, y2);
        this.drawBox(matrixStack, x1, x2, y2, y3, hovering && mouseY >= y3);
        this.drawNameAndValue(matrixStack, x1, x2, y1);
    }

    private boolean isHovering(int mouseX, int mouseY, int x1, int x2, int y1, int y2)
    {
        Window parent = getParent();

        boolean scrollEnabled = parent.isScrollingEnabled();
        int scroll = scrollEnabled ? parent.getScrollOffset() : 0;

        return mouseX >= x1 && mouseY >= y1 && mouseX < x2 && mouseY < y2 && mouseY >= -scroll && mouseY < parent.getHeight() - 13 - scroll;
    }

    private void setTooltip(String tooltip)
    {
        GUI.setTooltip(tooltip);
    }

    private void drawBackground(MatrixStack matrixStack, int x2, int x3, int y1, int y2)
    {
        float[] bgColor = GUI.getBgColor();
        float opacity = GUI.getOpacity();

        Matrix4f matrix = matrixStack.peek().getModel();
        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        RenderSystem.setShaderColor(bgColor[0], bgColor[1], bgColor[2], opacity);

        bufferBuilder.begin(DrawMode.QUADS, VertexFormats.POSITION);
        bufferBuilder.vertex(matrix, x3, y1, 0).next();
        bufferBuilder.vertex(matrix, x3, y2, 0).next();
        bufferBuilder.vertex(matrix, x2, y2, 0).next();
        bufferBuilder.vertex(matrix, x2, y1, 0).next();
        bufferBuilder.end();

        BufferRenderer.draw(bufferBuilder);
    }

    private void drawBox(MatrixStack matrixStack, int x1, int x3, int y1, int y2, boolean hovering)
    {
        Color color = this.setting.getColor();

        float[] acColor = this.GUI.getAcColor();
        float opacity = this.GUI.getOpacity();

        Matrix4f matrix = matrixStack.peek().getModel();
        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        RenderSystem.setShaderColor(color.getRed() / 255F, color.getGreen() / 255F, color.getBlue() / 255F, 
                hovering ? 1F : opacity
        );

        bufferBuilder.begin(DrawMode.QUADS, VertexFormats.POSITION);
        bufferBuilder.vertex(matrix, x1, y1, 0).next();
        bufferBuilder.vertex(matrix, x1, y2, 0).next();
        bufferBuilder.vertex(matrix, x3, y2, 0).next();
        bufferBuilder.vertex(matrix, x3, y1, 0).next();
        bufferBuilder.end();
        BufferRenderer.draw(bufferBuilder);

        RenderSystem.setShaderColor(acColor[0], acColor[1], acColor[2], 0.5F);
        bufferBuilder.begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION);
        bufferBuilder.vertex(matrix, x1, y1, 0).next();
        bufferBuilder.vertex(matrix, x1, y2, 0).next();
        bufferBuilder.vertex(matrix, x3, y2, 0).next();
        bufferBuilder.vertex(matrix, x3, y1, 0).next();
        bufferBuilder.vertex(matrix, x1, y1, 0).next();
        bufferBuilder.end();

        BufferRenderer.draw(bufferBuilder);
    }

    private void drawNameAndValue(MatrixStack matrixStack, int x1, int x2, int y1)
    {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        TextRenderer tr = MC.textRenderer;

        String name = setting.getName();
        Color colorValue = setting.getColor();
        String value = String.format("#%02x%02x%02x", colorValue.getRed(), colorValue.getGreen(), colorValue.getBlue());

        int valueWidth = tr.getWidth(value);
        int color = 0xF0F0F0;

        tr.draw(matrixStack, name, x1, y1 + 2, color);
        tr.draw(matrixStack, value, x2 - valueWidth, y1 + 2, color);

        GL11.glEnable(GL11.GL_BLEND);
    }

    public int getDefaultWidth()
    {
        return MC.textRenderer.getWidth(setting.getName()) + 13;
    }

    public int getDefaultHeight()
    {
        return 22;
    }
}
