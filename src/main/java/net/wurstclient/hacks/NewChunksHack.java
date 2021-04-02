package net.wurstclient.hacks;

import org.lwjgl.opengl.GL11;

import net.wurstclient.events.PacketInputListener;
import net.wurstclient.events.RenderListener;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

import com.mojang.blaze3d.systems.RenderSystem;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.util.math.Box;
import net.wurstclient.Category;
import net.wurstclient.hack.Hack;
import net.wurstclient.SearchTags;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.RenderUtils;

@SearchTags({"NewChunks", "new chunks", "chunk finder", "ChunkFinder"})
public final class NewChunksHack extends Hack implements PacketInputListener, RenderListener {

    //private int renderSettings;
    private Long LastPacketTime = 0L;
    private final CopyOnWriteArrayList<ChunkDataS2CPacket> newChunks = new CopyOnWriteArrayList<>();

    private final CheckboxSetting debug =
		new CheckboxSetting("DebugMode", false);

    private final SliderSetting delaySlider = new SliderSetting("Delay",
    "Delay between recived packets that will be counted as a new chunk.\n"
        + "Changes on Ping, TPS and lots more.",
    150, 10, 10000, 10, v -> (int)v + "ms");

    private final SliderSetting stopCountingSlider = new SliderSetting("Stop counting delay",
    "Any dleay over this number will not be counted as a new Chunk.\n"+
    "NOTE: This delay adds on to the delaySlider\n"
        + "Changes on Ping, TPS and lots more.",
    150, 10, 10000, 10, v -> (int)v + "ms");

    private final SliderSetting renderSlider = new SliderSetting("Render",
    "The Amount of new Chunks to render before deleting them from memory.",
    100, 1, 1000, 1, v -> (int)v + "Chunks");

	public NewChunksHack()
	{
		super("NewChunks", "Highlights new chunks.");
        setCategory(Category.RENDER);
        addSetting(debug);
        addSetting(delaySlider);
        addSetting(stopCountingSlider);
        addSetting(renderSlider);
    }
    
    @Override
	public void onEnable()
	{
        EVENTS.add(PacketInputListener.class, this);
        EVENTS.add(RenderListener.class, this);
        
        //renderSettings = GL11.glGenLists(1);
		//GL11.glNewList(renderSettings, GL11.GL_COMPILE);
		
	}
	
	@Override
	public void onDisable()
	{
        EVENTS.remove(PacketInputListener.class, this);
        EVENTS.remove(RenderListener.class, this);
        
        //GL11.glDeleteLists(renderSettings, 1);
        newChunks.clear();
	}

	@Override
	public void onReceivedPacket(PacketInputEvent event)
	{
        if(event.getPacket() instanceof ChunkDataS2CPacket)
        {
            if(debug.isChecked()){
                ChatUtils.message("Delay from last chunk: "+(System.currentTimeMillis()-LastPacketTime));
            }
            if(System.currentTimeMillis()-LastPacketTime >= delaySlider.getValueI() && System.currentTimeMillis()-LastPacketTime <= delaySlider.getValueI()+stopCountingSlider.getValueI())
            {
                if(newChunks.size()>=renderSlider.getValueI())
                {
                    newChunks.remove(0);
                }
                newChunks.add((ChunkDataS2CPacket)event.getPacket());
                //System.out.println("NewChunkFound? ChunkX: " + ((ChunkDataS2CPacket)event.getPacket()).getX() +", ChunkZ: "+ ((ChunkDataS2CPacket)event.getPacket()).getZ());
            }
            LastPacketTime = System.currentTimeMillis();
        }
    }

    @Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		// GL settings
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		GL11.glLineWidth(2);
		GL11.glDisable(GL11.GL_LIGHTING);
		
        matrixStack.push();
        RenderUtils.applyRenderOffset(matrixStack);
		
        renderChunks(matrixStack);
        		
		matrixStack.pop();
		
		// GL resets
		RenderSystem.setShaderColor(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_LINE_SMOOTH);
    }

    private void renderChunks(MatrixStack matrixStack)
    {   
        //Copy
        for(ChunkDataS2CPacket i : newChunks)
        {
            matrixStack.push();

            matrixStack.translate(i.getX() * 16.0 + 8, 0, i.getZ() * 16.0 + 8);// * partialTicks

            matrixStack.scale(16, 1, 16);
            //GL11.glCallList(renderSettings);

            GL11.glEnable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_LIGHTING);
            RenderSystem.setShaderColor(1, 0, 0, 0.5F);
            RenderUtils.drawOutlinedBox(matrixStack, new Box(-0.5, 0, -0.5, 0.5, 1, 0.5));

            matrixStack.pop();
        }
    }
}
