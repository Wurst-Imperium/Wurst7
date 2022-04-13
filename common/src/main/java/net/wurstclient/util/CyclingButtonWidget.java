package net.wurstclient.util;

import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Optional;

public class CyclingButtonWidget extends PressableWidget {

    private CycleButtonEntry selectedValue;
    private final ArrayList<CycleButtonEntry> entries;

    public CyclingButtonWidget(int x, int y, int width, int height, ArrayList<CycleButtonEntry> entries, Optional<CycleButtonEntry> defaultValue) {
        super(x, y, width, height, Text.of(""));
        assert !entries.isEmpty();
        this.entries = entries;
        this.selectedValue = defaultValue.orElseGet(() -> this.entries.get(0));
        this.setMessage(this.getDisplayText());
    }

    @Override
    public void onPress() {
        int index = this.entries.indexOf(this.selectedValue);
        if (index == this.entries.size() - 1){
            index = 0;
        } else {
            index += 1;
        }
        this.selectedValue = this.entries.get(index);
        this.setMessage(this.getDisplayText());
    }

    @Override
    public void appendNarrations(NarrationMessageBuilder builder) {

    }

    public Text getDisplayText(){
        return Text.of(this.selectedValue.name);
    }

    public String getSelectedValue(){
        return this.selectedValue.value;
    }

    public void addEntry(CycleButtonEntry entry){
        this.entries.add(entry);
    }

    public boolean isEntry(CycleButtonEntry entry){
        return this.entries.contains(entry);
    }


    public static class CycleButtonEntry{
        private final String name;
        private final String value;

        public CycleButtonEntry(String name, String value){
            this.name = name;
            this.value = value;
        }
    }

}
