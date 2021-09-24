/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
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

public class WurstOptionsScreen extends Screen
{
	private Screen prevScreen;
	
	public WurstOptionsScreen(Screen prevScreen)
	{
		super(new LiteralText(""));
		this.prevScreen = prevScreen;
	}
	
	@Override
	public void init()
	{
		addDrawableChild(
			new ButtonWidget(width / 2 - 100, height / 4 + 144 - 16, 200, 20,
				new LiteralText("返回"), b -> client.setScreen(prevScreen)));
		
		addSettingButtons();
		addManagerButtons();
		addLinkButtons();
	}
	
	private void addSettingButtons()
	{
		WurstClient wurst = WurstClient.INSTANCE;
		FriendsCmd friendsCmd = wurst.getCmds().friendsCmd;
		CheckboxSetting middleClickFriends = friendsCmd.getMiddleClickFriends();
		WurstAnalytics analytics = wurst.getAnalytics();
		VanillaSpoofOtf vanillaSpoofOtf = wurst.getOtfs().vanillaSpoofOtf;
		
		new WurstOptionsButton(-154, 24,
			() -> "点击朋友: "
				+ (middleClickFriends.isChecked() ? "开启" : "关闭"),
			middleClickFriends.getDescription(), b -> middleClickFriends
				.setChecked(!middleClickFriends.isChecked()));
		
		new WurstOptionsButton(-154, 48,
			() -> "统计用户: " + (analytics.isEnabled() ? "开启" : "关闭"),
			"统计有多少人在使用Wurst哪个版本最受欢迎.\n我们使用这个数据来决定何时停止\n支持旧的Minecraft版本.\n我们使用一个随机ID将用户分开\n这样这个数据就永远无法链接到您的Minecraft帐户.\n随机ID为′每3天改变一次，以确保额外确信您保持匿名.",
			b -> analytics.setEnabled(!analytics.isEnabled()));
		
		new WurstOptionsButton(-154, 72,
			() -> "欺骗香草: "
				+ (vanillaSpoofOtf.isEnabled() ? "开启" : "关闭"),
			vanillaSpoofOtf.getDescription(),
			b -> vanillaSpoofOtf.doPrimaryAction());
	}
	
	private void addManagerButtons()
	{
		XRayHack xRayHack = WurstClient.INSTANCE.getHax().xRayHack;
		
		new WurstOptionsButton(-50, 24, () -> "热键绑定",
			"Keybinds允许您只需按一个按钮就可以切换任何hack或命令.",
			b -> client.setScreen(new KeybindManagerScreen(this)));
		
		new WurstOptionsButton(-50, 48, () -> "X-Ray Blocks",
			"X-Ray将要显示的块的管理器",
			b -> xRayHack.openBlockListEditor(this));
		
		new WurstOptionsButton(-50, 72, () -> "Zoom",
			"Zoom Manager允许您更改缩放键，它会放大多远等.",
			b -> client.setScreen(new ZoomManagerScreen(this)));
	}
	
	private void addLinkButtons()
	{
		OperatingSystem os = Util.getOperatingSystem();
		
		new WurstOptionsButton(54, 24, () -> "官方网站",
			"WurstClient.net", b -> os.open("https://www.wurstclient.net/"));
		new WurstOptionsButton(54, 48, () -> "捐款 求捐款",
			"qq/微信/支付宝/支付",
			b -> os.open("https://docs.qq.com/doc/DYWJKZ2ZtdmVPZmVY"));
	}
	
	@Override
	public void render(MatrixStack matrixStack, int mouseX, int mouseY,
		float partialTicks)
	{
		renderBackground(matrixStack);
		renderTitles(matrixStack);
		super.render(matrixStack, mouseX, mouseY, partialTicks);
		renderButtonTooltip(matrixStack, mouseX, mouseY);
	}
	
	private void renderTitles(MatrixStack matrixStack)
	{
		TextRenderer tr = client.textRenderer;
		int middleX = width / 2;
		int y1 = 40;
		int y2 = height / 4 + 24 - 28;
		
		drawCenteredText(matrixStack, tr, "Wurst选择,作者id:watermelon_GG,qq:750215287,感谢逆向燃烧帮忙汉化", middleX, y1,
			0xffffff);
		
		drawCenteredText(matrixStack, tr, "设置选项", middleX - 104, y2,
			0xcccccc);
		drawCenteredText(matrixStack, tr, "管理者", middleX, y2, 0xcccccc);
		drawCenteredText(matrixStack, tr, "链接", middleX + 104, y2, 0xcccccc);
	}
	
	private void renderButtonTooltip(MatrixStack matrixStack, int mouseX,
		int mouseY)
	{
		for(Drawable d : ((IScreen)this).getButtons())
		{
			if(!(d instanceof ClickableWidget button))
				continue;
			
			if(!button.isHovered()
				|| !(button instanceof WurstOptionsButton woButton))
				continue;
			
			if(woButton.tooltip.isEmpty())
				continue;
			
			renderTooltip(matrixStack, woButton.tooltip, mouseX, mouseY);
			break;
		}
	}
	
	private final class WurstOptionsButton extends ButtonWidget
	{
		private final Supplier<String> messageSupplier;
		private final List<Text> tooltip;
		
		public WurstOptionsButton(int xOffset, int yOffset,
			Supplier<String> messageSupplier, String tooltip,
			PressAction pressAction)
		{
			super(WurstOptionsScreen.this.width / 2 + xOffset,
				WurstOptionsScreen.this.height / 4 - 16 + yOffset, 100, 20,
				new LiteralText(messageSupplier.get()), pressAction);
			
			this.messageSupplier = messageSupplier;
			
			if(tooltip.isEmpty())
				this.tooltip = Arrays.asList();
			else
			{
				String[] lines = tooltip.split("\n");
				
				LiteralText[] lines2 = new LiteralText[lines.length];
				for(int i = 0; i < lines.length; i++)
					lines2[i] = new LiteralText(lines[i]);
				
				this.tooltip = Arrays.asList(lines2);
			}
			
			addDrawableChild(this);
		}
		
		@Override
		public void onPress()
		{
			super.onPress();
			setMessage(new LiteralText(messageSupplier.get()));
		}
	}
}
