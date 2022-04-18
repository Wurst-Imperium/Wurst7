/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import net.minecraft.client.util.math.MatrixStack;
import net.wurstclient.options.WurstOptionsScreen;
import net.wurstclient.util.render.RenderUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.wurstclient.WurstClient;
import net.wurstclient.altmanager.screens.AltManagerScreen;
import net.wurstclient.mixinterface.IScreen;
import static net.wurstclient.util.ModMenuUtils.isModMenuPresent;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    private ClickableWidget vanillaRealmsButton = null;
    private ClickableWidget vanillaOptionsButton = null;
    private ButtonWidget altsButton;
    private ButtonWidget wurstOptionsButton;
    private TitleScreenMixin(WurstClient wurst, Text text_1) {
        super(text_1);
    }


    @Inject(at = {@At("RETURN")}, method = {"init()V"})
    private void onInitWidgetsNormal(CallbackInfo ci) {
        if (!WurstClient.INSTANCE.isEnabled() && !isModMenuPresent()) {
            addDrawableChild(new ButtonWidget(
                    0, 0, 1, 1, new LiteralText(""),
                    b -> {
                        WurstClient.INSTANCE.setEnabled(true);
                        WurstClient.setScreen(this);
                    }));
            return;
        }
        ClickableWidget vanillaMultiplayerButton = null;
        ClickableWidget vanillaQuitButton = null;
        ClickableWidget vanillaSingleplayerButton = null;
        for (Drawable d : ((IScreen) this).getButtons()) {
            if (!(d instanceof ClickableWidget button))
                continue;

            if (button.getMessage().getString().equals(I18n.translate("menu.online"))) {
                vanillaRealmsButton = button;
            } else if (button.getMessage().getString().equals(I18n.translate("menu.options"))) {
                vanillaOptionsButton = button;
            } else if (button.getMessage().getString().equals(I18n.translate("menu.singleplayer"))) {
                vanillaSingleplayerButton = button;
            } else if (button.getMessage().getString().equals(I18n.translate("menu.multiplayer"))) {
                vanillaMultiplayerButton = button;
            } else if (button.getMessage().getString().equals(I18n.translate("menu.quit"))) {
                vanillaQuitButton = button;
            }
        }

        if (vanillaRealmsButton == null)
            throw new IllegalStateException("Couldn't find realms button!");

        if (vanillaSingleplayerButton == null)
            throw new IllegalStateException("Couldn't find Singleplayer button!");
        if (vanillaMultiplayerButton == null)
            throw new IllegalStateException("Couldn't find Multiplayer button!");
        if (vanillaOptionsButton == null)
            throw new IllegalStateException("Couldn't find options button!");
        if (vanillaQuitButton == null)
            throw new IllegalStateException("Couldn't find Quit Game button!");

        // make Realms button smaller
        vanillaRealmsButton.setWidth(98);

        int horizontalSpacing = vanillaQuitButton.x - vanillaOptionsButton.x;
        // add AltManager button
        addDrawableChild(altsButton = new ButtonWidget(vanillaRealmsButton.x,
                vanillaRealmsButton.y, vanillaOptionsButton.getWidth(), vanillaRealmsButton.getHeight(), new LiteralText("Alt Manager"),
                b -> WurstClient.setScreen(new AltManagerScreen(this,
                        WurstClient.INSTANCE.getAltManager()))));
        vanillaRealmsButton.x = altsButton.x + horizontalSpacing;
        if (isModMenuPresent())
            return;
        // add Options button
        int verticalSpacing = vanillaMultiplayerButton.y - vanillaSingleplayerButton.y;
        wurstOptionsButton = new ButtonWidget(vanillaOptionsButton.x,
                vanillaRealmsButton.y + verticalSpacing,
                vanillaOptionsButton.getWidth(),
                vanillaOptionsButton.getHeight(),
                new LiteralText("         Options"),
                b -> WurstClient.setScreen(new WurstOptionsScreen(this))
        );
        addDrawableChild(wurstOptionsButton);
        vanillaOptionsButton.y = wurstOptionsButton.y + verticalSpacing;
        vanillaQuitButton.y = vanillaOptionsButton.y;
    }

    @Inject(at = {@At("RETURN")}, method = {"tick()V"})
    private void onTick(CallbackInfo ci) {
        if (vanillaRealmsButton == null || altsButton == null)
            return;

        // adjust AltManager button if Realms button has been moved
        // happens when ModMenu is installed
        altsButton.y = vanillaRealmsButton.y;
    }

    @Inject(at = {@At("TAIL")},
            method = {"render(Lnet/minecraft/client/util/math/MatrixStack;IIF)V"})
    private void onRender(MatrixStack matrixStack, int mouseX, int mouseY,
                          float partialTicks, CallbackInfo ci)
    {
        if(WurstClient.INSTANCE.isEnabled() && !isModMenuPresent())
            RenderUtils.renderWurstLogo(matrixStack, wurstOptionsButton.x + 2, wurstOptionsButton.y + 3);
    }
}
