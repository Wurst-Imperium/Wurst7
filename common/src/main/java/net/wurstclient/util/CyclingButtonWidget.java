package net.wurstclient.util;

import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;
import net.wurstclient.WurstClient;

import java.util.Objects;


public class CyclingButtonWidget<T extends Enum<T>> extends PressableWidget {

    private final T[] values;
    private T selected;
    private final T defaultSelected;


    public CyclingButtonWidget(int x, int y, int width, int height, T[] values, T selected) {
        super(x, y, width, height, Text.of(""));
        this.values = Objects.requireNonNull(values);
        this.selected = Objects.requireNonNull(selected);
        defaultSelected = selected;
        this.setMessage(this.getDisplayText());
    }

    @Override
    public void onPress() {
        this.selectNext();
        this.setMessage(this.getDisplayText());
    }

    @Override
    public void appendNarrations(NarrationMessageBuilder builder) {

    }

    public Text getDisplayText(){
        return Text.of(this.getSelected().toString());
    }

    public T[] getValues()
    {
        return values;
    }

    public T getSelected()
    {
        return selected;
    }

    public T getDefaultSelected()
    {
        return defaultSelected;
    }

    public void setSelected(T selected)
    {
        this.selected = Objects.requireNonNull(selected);
        WurstClient.INSTANCE.saveSettings();
    }

    public boolean setSelected(String selected)
    {
        for(T value : values)
        {
            if(!value.toString().equalsIgnoreCase(selected))
                continue;

            setSelected(value);
            return true;
        }

        return false;
    }

    public void selectNext()
    {
        int next = selected.ordinal() + 1;
        if(next >= values.length)
            next = 0;

        setSelected(values[next]);
    }

    public void selectPrev()
    {
        int prev = selected.ordinal() - 1;
        if(prev < 0)
            prev = values.length - 1;

        setSelected(values[prev]);
    }
}
