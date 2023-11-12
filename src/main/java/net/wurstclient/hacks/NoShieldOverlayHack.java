package net.wurstclient.hacks;

import net.minecraft.client.util.math.MatrixStack;
import net.wurstclient.Category;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

public final class NoShieldOverlayHack extends Hack
{
	public final SliderSetting nonBlockingOffset =
		new SliderSetting("Non-Blocking Offset", "The amount to lower the shield overlay when not blocking.",
			0.2, 0, 0.5, 0.01, ValueDisplay.DECIMAL);
	public final SliderSetting blockingOffset =
		new SliderSetting("Offset", "The amount to lower the shield overlay by when blocking.",
			0.35, 0, 0.6, 0.01, ValueDisplay.DECIMAL);

	public NoShieldOverlayHack()
	{
		super("NoShieldOverlay");
		setCategory(Category.RENDER);
		addSetting(nonBlockingOffset);
		addSetting(blockingOffset);
	}

	public void adjustShieldPosition(MatrixStack matrices, boolean blocking)
	{
		if(!isEnabled())
			return;

		if(blocking)
			matrices.translate(0, -blockingOffset.getValue(), 0);
		else
			matrices.translate(0, -nonBlockingOffset.getValue(), 0);
	}
	// See HeldItemRendererMixin
}
