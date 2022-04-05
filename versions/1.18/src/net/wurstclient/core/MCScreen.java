package net.wurstclient.core;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

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

}
