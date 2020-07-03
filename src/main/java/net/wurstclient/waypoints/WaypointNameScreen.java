package net.wurstclient.waypoints;

import net.minecraft.client.util.math.MatrixStack;
import net.wurstclient.clickgui.WaypointWindow;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.WaypointsHack;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.waypoints.Waypoint;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.LiteralText;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class WaypointNameScreen extends Screen {

    private static final MinecraftClient MC = MinecraftClient.getInstance();
    private final Screen prevScreen;

        private TextFieldWidget nameField;
        private ButtonWidget doneButton;
        private WaypointsHack waypointsHack;
        private boolean error;
        private String errorPoint;

        public WaypointNameScreen(Screen prevScreen, WaypointsHack waypointsHack)
        {
            super(new LiteralText(""));
            this.prevScreen = prevScreen;
            this.waypointsHack = waypointsHack;
        }

        @Override
        public void init()
        {
            int x1 = width / 2 - 100;
            int y1 = 60;
            int y2 = height / 3 * 2;

            TextRenderer tr = client.textRenderer;

            nameField = new TextFieldWidget(tr, x1, y1, 200, 20, new LiteralText(""));
            nameField.setSelectionStart(0);

            children.add(nameField);
            setInitialFocus(nameField);
            nameField.setSelected(true);

            doneButton = new ButtonWidget(x1, y2, 200, 20, new LiteralText("Done"), b -> done());
            addButton(doneButton);
        }

        private void done()
        {
            String name = this.nameField.getText().trim();
            error = !waypointsHack.addWaypoint(name, MC.player.getPos());
            if(error) {
                errorPoint = name;
                return;
            }
            client.openScreen(prevScreen);
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
                    client.openScreen(prevScreen);
                    break;
            }

            return super.keyPressed(keyCode, scanCode, int_3);
        }

        @Override
        public void tick()
        {
            nameField.tick();
        }

        @Override
        public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks)
        {
            renderBackground(matrixStack);
            drawCenteredString(matrixStack, client.textRenderer, "New Waypoint", width / 2,
                    20, 0xFFFFFF);

            nameField.render(matrixStack, mouseX, mouseY, partialTicks);
            if(error)
            drawCenteredString(matrixStack, client.textRenderer, "Error: Waypoint '" + errorPoint + "' already exists on this world.", width/2, nameField.y - 22, 0xFF4444);

            super.render(matrixStack, mouseX, mouseY, partialTicks);
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
    }
