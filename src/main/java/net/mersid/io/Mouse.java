package net.mersid.io;

import org.lwjgl.glfw.GLFW;

import net.mersid.events.LeftUpEventListener.LeftUpEvent;
import net.minecraft.client.MinecraftClient;
import net.wurstclient.WurstClient;
import net.wurstclient.events.UpdateListener;


/**
 * Useful utilities to get the current mouse state anywhere on screen. <br>
 * Does not replace LeftClickListener, as this allows constant polling. <br>
 * However, an instance of this must be initialized first.
 * @author Mersid
 *
 */
public class Mouse implements UpdateListener {
	
	private static long HANDLE = -1;
	private static boolean tracker;
	
	public static boolean isLeftDown()
	{
		GLFW.glfwPollEvents();
		return GLFW.glfwGetMouseButton(HANDLE, GLFW.GLFW_MOUSE_BUTTON_LEFT) != 0;
	}
	
	public static boolean isRightDown()
	{
		GLFW.glfwPollEvents();
		return GLFW.glfwGetMouseButton(HANDLE, GLFW.GLFW_MOUSE_BUTTON_RIGHT) != 0;
	}
	
	public static boolean isMiddleDown()
	{
		GLFW.glfwPollEvents();
		return GLFW.glfwGetMouseButton(HANDLE, GLFW.GLFW_MOUSE_BUTTON_MIDDLE) != 0;
	}

	
	@Override
	public void onUpdate()
	{
		// Initialize HANDLE if needed.
		if (HANDLE == -1) HANDLE = MinecraftClient.getInstance().window.getHandle();
		
		if (isLeftDown())
		{
			tracker = true;
		}
		
		if (!isLeftDown() && tracker)
		{
			tracker = false;
			WurstClient.INSTANCE.getEventManager().fire(new LeftUpEvent());
		}
	}
	
}
