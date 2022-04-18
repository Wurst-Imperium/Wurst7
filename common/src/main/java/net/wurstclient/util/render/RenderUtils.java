package net.wurstclient.util.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;

import static net.minecraft.client.gui.DrawableHelper.drawTexture;

public class RenderUtils {

    public static void renderWurstLogo(MatrixStack matrixStack, int x, int y){
        renderWurstLogo(matrixStack, x, y, 46, 14);
    }

    public static void renderWurstLogo(MatrixStack matrixStack, int x, int y, int width, int height){
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        Identifier wurstTexture = new Identifier("wurst", "wurst_128.png");
        RenderSystem.setShaderTexture(0, wurstTexture);
        drawTexture(matrixStack, x, y, 0.0F, 0.0F, width, height, width, height);
    }
}
