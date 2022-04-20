package net.wurstclient.util.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.wurstclient.util.ListWidget;
import net.wurstclient.util.profiles.ManagedProfile;

public abstract class ManagedProfileListGui extends ListWidget
{
    private int selected = -1;
    private final ManagedProfile profile;
    public ManagedProfileListGui(ManagedProfile profile, MinecraftClient client, int width, int height, int top, int bottom, int itemHeight) {
        super(client, width, height, top, bottom, itemHeight);
        this.profile = profile;
    }

    @Override
    protected boolean isSelectedItem(int index)
    {
        return selected == index;
    }

    @Override
    public int getItemCount()
    {
        return profile.size();
    }

    @Override
    protected boolean selectItem(int index, int int_2, double var3,
                                 double var4)
    {
        if(index >= 0 && index < getItemCount())
            selected = index;

        return true;
    }

    @Override
    protected void renderBackground()
    {

    }

    public int getSelected(){
        return selected;
    }

    public void setSelected(int value){
        selected = value;
    }


    protected abstract void renderItem(MatrixStack matrixStack, int index, int x,
                              int y, int slotHeight, int mouseX, int mouseY, float partialTicks);
}