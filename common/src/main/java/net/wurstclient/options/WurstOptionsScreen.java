/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.options;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.Util.OperatingSystem;
import net.wurstclient.WurstClient;
import net.wurstclient.analytics.WurstAnalytics;
import net.wurstclient.commands.FriendsCmd;
import net.wurstclient.hacks.XRayHack;
import net.wurstclient.mixinterface.IScreen;
import net.wurstclient.other_features.VanillaSpoofOtf;
import net.wurstclient.settings.CheckboxSetting;

import static net.wurstclient.util.ModMenuUtils.isModMenuPresent;

public class WurstOptionsScreen extends Screen {
    private final Screen prevScreen;

    public WurstOptionsScreen(Screen prevScreen) {
        super(new LiteralText(""));
        this.prevScreen = prevScreen;
    }

    @Override
    public void init() {
        addDrawableChild(
                new ButtonWidget(width / 2 - 100, height / 4 + 144 - 16, 200, 20,
                        new LiteralText("Back"), b -> WurstClient.setScreen(prevScreen)));

        addSettingButtons();
        addManagerButtons();
        addLinkButtons();
    }

    private void addSettingButtons() {
        WurstClient wurst = WurstClient.INSTANCE;
        FriendsCmd friendsCmd = wurst.getCmds().friendsCmd;
        CheckboxSetting middleClickFriends = friendsCmd.getMiddleClickFriends();
        WurstAnalytics analytics = wurst.getAnalytics();
        VanillaSpoofOtf vanillaSpoofOtf = wurst.getOtfs().vanillaSpoofOtf;
        CheckboxSetting forceEnglish =
                wurst.getOtfs().translationsOtf.getForceEnglish();
        int verticalSpacing = 24;

        new WurstOptionsButton(-154, verticalSpacing,
                () -> "Click Friends: "
                        + (middleClickFriends.isChecked() ? "ON" : "OFF"),
                middleClickFriends.getWrappedDescription(200),
                b -> middleClickFriends
                        .setChecked(!middleClickFriends.isChecked()));

        new WurstOptionsButton(-154, verticalSpacing * 2,
                () -> "Count Users",
                "DISABLED, I DON'T CARE",
                b -> analytics.setEnabled(false));

        new WurstOptionsButton(-154, verticalSpacing * 3,
                () -> "Spoof Vanilla: "
                        + (vanillaSpoofOtf.isEnabled() ? "ON" : "OFF"),
                vanillaSpoofOtf.getWrappedDescription(200),
                b -> vanillaSpoofOtf.doPrimaryAction());

        new WurstOptionsButton(-154, verticalSpacing * 4,
                () -> "Translations: " + (!forceEnglish.isChecked() ? "ON" : "OFF"),
                "§cThis is an experimental feature!\n"
                        + "We don't have many translations yet. If you\n"
                        + "speak both English and some other language,\n"
                        + "please help us by adding more translations.",
                b -> forceEnglish.setChecked(!forceEnglish.isChecked()));

        new WurstOptionsButton(-154, verticalSpacing * 5,
                () -> (!WurstClient.INSTANCE.isEnabled() ? "Enable" : "Disable") + " Wurst",
                (isModMenuPresent() ? "Open settings from Mods menu" : "Click the top left corner of the Title Screen\n"
                        + "or Game Menu") + " to turn back on.",
                b -> {
                    WurstClient.INSTANCE.setEnabled(!WurstClient.INSTANCE.isEnabled());
                    WurstClient.setScreen(prevScreen);
                }
        );
    }

    private void addManagerButtons() {
        new WurstOptionsButton(-50, 24, () -> "Enabled Hacks",
                "Profiles to Enable or Disable Hacks",
                b -> WurstClient.setScreen(new ManageProfilesScreen(this, WurstClient.INSTANCE.getHax())));
        new WurstOptionsButton(-50, 48, () -> "Keybinds",
                "Keybinds allow you to toggle any hack\n"
                        + "or command by simply pressing a\n" + "button.",
                b -> WurstClient.setScreen(new ManageProfilesScreen(this, WurstClient.INSTANCE.getKeybinds()) {
                }));

        new WurstOptionsButton(-50, 72, () -> "X-Ray Blocks",
                "Manager for the blocks\n" + "that X-Ray will show.",
                b -> WurstClient.INSTANCE.getHax().xRayHack.openBlockListEditor(this));

        new WurstOptionsButton(-50, 96, () -> "Zoom",
                "The Zoom Manager allows you to\n"
                        + "change the zoom key, how far it\n"
                        + "will zoom in and more.",
                b -> WurstClient.setScreen(new ZoomManagerScreen(this)));
    }

    private void addLinkButtons() {
        OperatingSystem os = Util.getOperatingSystem();

        new WurstOptionsButton(54, 24, () -> "Official Website",
                "WurstClient.net", b -> os.open("https://www.wurstclient.net/"));

        new WurstOptionsButton(54, 48, () -> "Wurst Wiki",
                "Wiki.WurstClient.net",
                b -> os.open("https://wiki.wurstclient.net/"));

        new WurstOptionsButton(54, 72, () -> "Twitter", "@Wurst_Imperium",
                b -> os.open("https://twitter.com/Wurst_Imperium"));

        new WurstOptionsButton(54, 96, () -> "Reddit", "r/WurstClient",
                b -> os.open("https://www.reddit.com/r/WurstClient/"));

        new WurstOptionsButton(54, 120, () -> "Source Code",
                "https://github.com/TheGrandCurator/Cheddar-BratWurst7/",
                b -> os.open("https://github.com/TheGrandCurator/Cheddar-BratWurst7/"));
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY,
                       float partialTicks) {
        renderBackground(matrixStack);
        renderTitles(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        renderButtonTooltip(matrixStack, mouseX, mouseY);
    }

    private void renderTitles(MatrixStack matrixStack) {
        TextRenderer tr = client.textRenderer;
        int middleX = width / 2;
        int y1 = 40;
        int y2 = height / 4 + 24 - 28;

        drawCenteredText(matrixStack, tr, "CheddarBrat-Wurst " + WurstClient.VERSION + " Options", middleX, y1,
                0xffffff);

        drawCenteredText(matrixStack, tr, "Settings", middleX - 104, y2,
                0xcccccc);
        drawCenteredText(matrixStack, tr, "Managers", middleX, y2, 0xcccccc);
        drawCenteredText(matrixStack, tr, "Links", middleX + 104, y2, 0xcccccc);
    }

    private void renderButtonTooltip(MatrixStack matrixStack, int mouseX,
                                     int mouseY) {
        for (Drawable d : ((IScreen) this).getButtons()) {
            if (!(d instanceof ClickableWidget))
                continue;

            ClickableWidget button = (ClickableWidget) d;

            if (!button.isHovered() || !(button instanceof WurstOptionsButton))
                continue;

            WurstOptionsButton woButton = (WurstOptionsButton) button;

            if (woButton.tooltip.isEmpty())
                continue;

            renderTooltip(matrixStack, woButton.tooltip, mouseX, mouseY);
            break;
        }
    }

    private final class WurstOptionsButton extends ButtonWidget {
        private final Supplier<String> messageSupplier;
        private final List<Text> tooltip;

        public WurstOptionsButton(int xOffset, int yOffset,
                                  Supplier<String> messageSupplier, String tooltip,
                                  PressAction pressAction) {
            super(WurstOptionsScreen.this.width / 2 + xOffset,
                    WurstOptionsScreen.this.height / 4 - 16 + yOffset, 100, 20,
                    new LiteralText(messageSupplier.get()), pressAction);

            this.messageSupplier = messageSupplier;

            if (tooltip.isEmpty())
                this.tooltip = Arrays.asList();
            else {
                String[] lines = tooltip.split("\n");

                LiteralText[] lines2 = new LiteralText[lines.length];
                for (int i = 0; i < lines.length; i++)
                    lines2[i] = new LiteralText(lines[i]);

                this.tooltip = Arrays.asList(lines2);
            }

            addDrawableChild(this);
        }

        @Override
        public void onPress() {
            super.onPress();
            setMessage(new LiteralText(messageSupplier.get()));
        }
    }
}
