/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.gui.screen.ConnectScreen;

import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

import net.wurstclient.WurstClient;
import net.wurstclient.mixinterface.IScreen;
import net.wurstclient.options.WurstOptionsScreen;
import net.wurstclient.util.render.RenderUtils;

import static net.wurstclient.util.ModMenuUtils.isModMenuPresent;


@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin extends Screen
{
	private ButtonWidget wurstOptionsButton;
	private final ServerInfo serverEntry = new ServerInfo("", "", false);
	private GameMenuScreenMixin(WurstClient wurst, Text text_1)
	{
		super(text_1);
	}


	@Inject(at = {@At("TAIL")}, method = {"initWidgets()V"})
	private void onInitWidgets(CallbackInfo ci)
	{
		if(!WurstClient.INSTANCE.isEnabled() || isModMenuPresent()){
			if (isModMenuPresent())
				return;
			addDrawableChild(new ButtonWidget(
					0, 0, 1, 1, new LiteralText(""),
					b -> {
						WurstClient.INSTANCE.setEnabled(true);
						WurstClient.setScreen(this);
					}));
			return;
		}

		addWurstOptionsButton();
		addWurstConnectButton();
		removeFeedbackAndBugReportButtons();
	}

	private void addWurstOptionsButton()
	{
		wurstOptionsButton = new ButtonWidget(width / 2 - 102, height / 4 + 56,
				98, 20, new LiteralText("          Options"),
				b -> openWurstOptions());
		addDrawableChild(wurstOptionsButton);
	}

	private void addWurstConnectButton()
	{
		ButtonWidget wurstConnectButton = new ButtonWidget(width / 2 + 4, height / 4 + 56,
				98, 20, new LiteralText("Direct Connect"),
				b -> WurstClient.setScreen(new DirectConnectScreen(this, this::directConnect, serverEntry)));
		addDrawableChild(wurstConnectButton);
	}

	private void openWurstOptions()
	{
		WurstClient.setScreen(new WurstOptionsScreen(this));
	}

	private void removeFeedbackAndBugReportButtons()
	{
		((IScreen)this).getButtons()
				.removeIf(this::isFeedbackOrBugReportButton);
		children().removeIf(this::isFeedbackOrBugReportButton);
	}

	private boolean isFeedbackOrBugReportButton(Object element)
	{
		if(element == null || !(element instanceof ClickableWidget))
			return false;

		ClickableWidget button = (ClickableWidget)element;
		String message = button.getMessage().getString();

		return message != null
				&& (message.equals(I18n.translate("menu.sendFeedback"))
				|| message.equals(I18n.translate("menu.reportBugs")));
	}

	@Inject(at = {@At("TAIL")},
			method = {"render(Lnet/minecraft/client/util/math/MatrixStack;IIF)V"})
	private void onRender(MatrixStack matrixStack, int mouseX, int mouseY,
						  float partialTicks, CallbackInfo ci)
	{
		if(WurstClient.INSTANCE.isEnabled() && !isModMenuPresent())
			RenderUtils.renderWurstLogo(matrixStack, wurstOptionsButton.x + 2, wurstOptionsButton.y + 3);
	}

	private void directConnect(boolean confirmedAction)
	{
		if(confirmedAction)
			connect(serverEntry);
		else
			WurstClient.setScreen(this);
	}

	private void connect(ServerInfo entry)
	{
		if(WurstClient.MC.world != null)
			WurstClient.MC.world.disconnect();
		if(WurstClient.MC.isInSingleplayer())
			WurstClient.MC.disconnect();
		else
			WurstClient.MC.disconnect();
		ConnectScreen.connect(this, WurstClient.MC, ServerAddress.parse(entry.address), entry);
	}
}
