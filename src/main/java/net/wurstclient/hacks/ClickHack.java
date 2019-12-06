package net.wurstclient.hacks;

import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.util.ErrorUtil;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Method;

@SearchTags({"AutoClicker", "auto clicker"})
public final class ClickHack extends Hack implements UpdateListener
{
    private final EnumSetting<Mode> modeSetting = new EnumSetting<>("Mode", Mode.values(), Mode.RIGHT_CLICK);

    private final SliderSetting speedSetting = new SliderSetting("Click Speed",
            "Allows you to set the rate in which you are clicking in clicks per second.",
            1, 0.1, 20, 0.1, SliderSetting.ValueDisplay.DECIMAL);

    private Method mouseTriggerMethod;
    private int timeSinceLastClick;

    public ClickHack()
    {
        super("Click", "Right- or leftclick with your client automatically.");
        setCategory(Category.OTHER);

        addSetting(modeSetting);
        addSetting(speedSetting);
    }

    @Override
    protected void onEnable()
    {
        EVENTS.add(UpdateListener.class, this);
    }

    @Override
    protected void onDisable()
    {
        EVENTS.remove(UpdateListener.class, this);

        timeSinceLastClick = 0;
    }

    @Override
    public void onUpdate()
    {
        int ticksPerClick = 2000 / (int) (speedSetting.getValue() * 100);

        if (timeSinceLastClick >= ticksPerClick)
        {
            if (mouseTriggerMethod == null)
            {
                ErrorUtil.runUnchecked(() -> {
                    this.mouseTriggerMethod = MC.mouse.getClass().getDeclaredMethod("onMouseButton",
                            Long.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE);
                    this.mouseTriggerMethod.setAccessible(true);
                }, "Could not locate onMouseButton event on the minecraft mouse class!");
            }

            InputUtil.KeyCode keyCode = modeSetting.getSelected().getKeyCode();

            KeyBinding.setKeyPressed(keyCode, true);
            KeyBinding.onKeyPressed(keyCode);
            KeyBinding.setKeyPressed(keyCode, false);

            timeSinceLastClick = 0;
        }

        timeSinceLastClick++;
    }

    private enum Mode
    {
        LEFT_CLICK("Left Click", GLFW.GLFW_MOUSE_BUTTON_LEFT),
        RIGHT_CLICK("Right Click", GLFW.GLFW_MOUSE_BUTTON_RIGHT);

        private final String name;
        private final InputUtil.KeyCode keyCode;

        Mode(String name, int mouseButton)
        {
            this.name = name;
            this.keyCode = InputUtil.Type.MOUSE.createFromCode(mouseButton);
        }

        public InputUtil.KeyCode getKeyCode()
        {
            return this.keyCode;
        }

        @Override
        public String toString()
        {
            return this.name;
        }
    }
}
