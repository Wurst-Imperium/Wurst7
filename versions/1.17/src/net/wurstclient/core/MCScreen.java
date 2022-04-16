package net.wurstclient.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Matrix4f;

public class MCScreen extends Screen{

    protected MCScreen(Text title) {
        super(title);
    }

    @Override
    public void onClose()
    {
        super.onClose();
    }

    public void close(){
        onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public boolean shouldPause()
    {
        return false;
    }

    public static void MCSetScreen(MinecraftClient MC, Screen screen) {
        MC.openScreen(screen);
    }
}
