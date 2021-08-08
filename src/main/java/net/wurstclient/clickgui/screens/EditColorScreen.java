package net.wurstclient.clickgui.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import javax.imageio.ImageIO;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.wurstclient.WurstClient;
import net.wurstclient.settings.ColorSetting;
import net.wurstclient.util.ColorUtils;
import org.lwjgl.glfw.GLFW;

public final class EditColorScreen extends Screen
{
    private final MinecraftClient MC = WurstClient.MC;
    private final Screen prevScreen;
    private final ColorSetting colorSetting;

    private TextFieldWidget redValueField;
    private TextFieldWidget greenValueField;
    private TextFieldWidget blueValueField;

    private ButtonWidget doneButton;

    private final Identifier paletteIdentifier = new Identifier("wurst", "colorscheme.png");
    private BufferedImage paletteAsBufferedImage;
    private int paletteX = 0;
    private int paletteY = 0;
    private final int paletteWidth = 213;
    private final int paletteHeight = 79;

    private int fieldsX = 0;
    private int fieldsY = 0;

    public EditColorScreen(Screen prevScreen, ColorSetting colorSetting)
    {
        super(new LiteralText(""));
        this.prevScreen = prevScreen;
        this.colorSetting = colorSetting;
    }

    public void setColor(Color color)
    {
        redValueField.setText("" + color.getRed());
        greenValueField.setText("" + color.getGreen());
        blueValueField.setText("" + color.getBlue());
    }

    public Color parseColor()
    {
        String red = redValueField.getText();
        String green = greenValueField.getText();
        String blue = blueValueField.getText();

        try
        {
            return ColorUtils.parse(red + "," + green + "," + blue);
        }
        catch (Exception ignored)
        {
            return Color.BLACK;
        }
    }

    @Override
    public void init()
    {

        // Cache color palette
        try
        {
            InputStream stream = MC.getResourceManager().getResource(paletteIdentifier).getInputStream();
            paletteAsBufferedImage = ImageIO.read(stream);
        }
        catch (IOException e)
        {
            paletteAsBufferedImage = null;
            e.printStackTrace();
        }

        paletteX = width / 2 - 106;
        paletteY = 50;
        fieldsX = width / 2 - 100;
        fieldsY = paletteY + 79 + 5;
        
        int y2 = height - 30;
        
        TextRenderer tr = client.textRenderer;
        Color color = colorSetting.getColor();

        String redString = "" + color.getRed();
        String greenString = "" + color.getGreen();
        String blueString = "" + color.getBlue();

        // RGB fields
        redValueField = new TextFieldWidget(tr, fieldsX, fieldsY, 200, 20, new LiteralText(""));
        redValueField.setText(redString);
        
        greenValueField = new TextFieldWidget(tr, fieldsX, fieldsY + 25, 200, 20, new LiteralText(""));
        greenValueField.setText(greenString);
        
        blueValueField = new TextFieldWidget(tr, fieldsX, fieldsY + 50, 200, 20, new LiteralText(""));
        blueValueField.setText(blueString);
        
        addSelectableChild(redValueField);
        addSelectableChild(greenValueField);
        addSelectableChild(blueValueField);
        
        setInitialFocus(redValueField);
        redValueField.setTextFieldFocused(true);
        
        doneButton = new ButtonWidget(fieldsX, y2, 200, 20, new LiteralText("Done"), (b) -> done());
        addDrawableChild(doneButton);
    }


    private void done()
    {
        colorSetting.setColor(parseColor());
        client.setScreen(prevScreen);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int int_3)
    {
        switch(keyCode)
        {
            case GLFW.GLFW_KEY_ENTER:
                done();
                break;

            case GLFW.GLFW_KEY_ESCAPE:
                client.setScreen(prevScreen);
                break;
        }

        return super.keyPressed(keyCode, scanCode, int_3);
    }

    @Override
    public void tick()
    {
        redValueField.tick();
        greenValueField.tick();
        blueValueField.tick();
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
        TextRenderer tr = MC.textRenderer;

        renderBackground(matrixStack);
        drawCenteredText(matrixStack, client.textRenderer, colorSetting.getName(), width / 2, 5, 0xF0F0F0);

        // Draw palette
        RenderSystem.setShaderTexture(0, paletteIdentifier);
        int x = paletteX;
        int y = paletteY;
        int w = paletteWidth;
        int h = paletteHeight;
        int fw = paletteWidth;
        int fh = paletteHeight;
        float u = 0F;
        float v = 0F;
        drawTexture(matrixStack, x, y, u, v, w, h, fw, fh);

        // RGB letters
        tr.draw(matrixStack, "R:", (float)(fieldsX - 3 - tr.getWidth("R:")), (float)(fieldsY + 5),      0xFF0000);
        tr.draw(matrixStack, "G:", (float)(fieldsX - 3 - tr.getWidth("G:")), (float)(fieldsY + 5 + 25), 0x00FF00);
        tr.draw(matrixStack, "B:", (float)(fieldsX - 3 - tr.getWidth("B:")), (float)(fieldsY + 5 + 50), 0x0000FF);

        /** Color preview **/

        int borderSize = 2;
        int boxWidth = 100;
        int boxHeight = 15;
        int boxX = width / 2 - boxWidth / 2;
        int boxY = paletteY - 5 - boxHeight;

        Color color = parseColor();

        // Border
        fill(
            matrixStack,
            boxX - borderSize,
            boxY - borderSize,
            boxX + boxWidth + borderSize,
            boxY + boxHeight + borderSize,
            color.getRGB() > (new Color(63, 63, 63)).getRGB() ?
                color.darker().darker().getRGB() :
                color.brighter().brighter().getRGB()
        );

        // Color box
        fill(
            matrixStack,
            boxX,
            boxY,
            boxX + boxWidth,
            boxY + boxHeight,
            color.getRGB()
        );

        // Hex string
        String value = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
        int valueWidth = tr.getWidth(value);
        tr.draw(matrixStack, value, (float)(boxX + boxWidth / 2 - valueWidth / 2), (float)(boxY - 12), 0xF0F0F0);

        redValueField.render(matrixStack, mouseX, mouseY, partialTicks);
        greenValueField.render(matrixStack, mouseX, mouseY, partialTicks);
        blueValueField.render(matrixStack, mouseX, mouseY, partialTicks);

        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    @Override
    public void resize(MinecraftClient client, int width, int height)
    {
        String r = redValueField.getText();
        String g = greenValueField.getText();
        String b = blueValueField.getText();

        init(client, width, height);

        redValueField.setText(r);
        greenValueField.setText(g);
        blueValueField.setText(b);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc()
    {
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (mouseX >= paletteX &&
            mouseX <= paletteX + paletteWidth &&
            mouseY >= paletteY &&
            mouseY <= paletteY + paletteHeight)
        {
            if (paletteAsBufferedImage == null)
                return super.mouseClicked(mouseX, mouseY, button);

            int x = (int) Math.round((mouseX - paletteX) / paletteWidth * paletteAsBufferedImage.getWidth());
            int y = (int) Math.round((mouseY - paletteY) / paletteHeight * paletteAsBufferedImage.getHeight());

            if (x > 0 && y > 0 && x < paletteAsBufferedImage.getWidth() && y < paletteAsBufferedImage.getHeight())
            {
                int rgb = paletteAsBufferedImage.getRGB(x, y);
                Color color = new Color(rgb, true);

                // Set color if pixel has no alpha
                if (color.getAlpha() >= 255)
                {
                    setColor(color);
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
}
