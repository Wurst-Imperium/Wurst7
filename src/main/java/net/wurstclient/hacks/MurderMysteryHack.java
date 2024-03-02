package net.wurstclient.hacks;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.WurstClient;
import net.wurstclient.events.PacketInputListener;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.TextFieldSetting;
import net.wurstclient.util.*;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SearchTags({"Murder Mystery", "MurderMystery", "murder mystery", "Murder",
	"murder", "killers", "mm"})
public final class MurderMysteryHack extends Hack
	implements UpdateListener, PacketInputListener, RenderListener
{
	private final SliderSetting scale = new SliderSetting("Scale",
		"How large the texture indicators above players heads should be.", 1,
		0.5, 2, 0.01, SliderSetting.ValueDisplay.PERCENTAGE);
	
	private final TextFieldSetting murdererItems = new TextFieldSetting(
		"List of murderer's items",
		"A space-separated string of Minecraft item IDs (without specifying the 'minecraft:' namespace) which describes all possible items that murderer can use as a sword.",
		"wooden_sword stone_sword iron_sword diamond_sword golden_sword netherite_sword shears");
	
	private final TextFieldSetting detectiveItems = new TextFieldSetting(
		"List of detective's items",
		"A space-separated string of Minecraft item IDs (without specifying the 'minecraft:' namespace) which describes all possible items that detective can use as a bow.",
		"bow crossbow");
	
	private final CheckboxSetting showSwordIndicators = new CheckboxSetting(
		"Show sword indicators",
		"Shows sword textures above players with murderer items (murderers).",
		true);
	
	private final CheckboxSetting showBowIndicators =
		new CheckboxSetting("Show bow indicators",
			"Shows bow textures above players with bows (detectives).", true);
	
	private final CheckboxSetting reportMurderers = new CheckboxSetting(
		"Report murderers",
		"Reports players with murderer items (murderers) in the chat.", true);
	
	private final CheckboxSetting reportDetectives = new CheckboxSetting(
		"Report detectives",
		"Reports players with detective items (detectives) in the chat.", true);
	
	private final CheckboxSetting autoClearMurderersList = new CheckboxSetting(
		"Automatically clear murderers list",
		"Automatically clears list of players with murderer items (murderers) on DISCONNECT or JOIN client event.",
		true);
	
	private final CheckboxSetting autoClearDetectivesList = new CheckboxSetting(
		"Automatically clear detectives list",
		"Automatically clears list of players with detective items (detectives) on DISCONNECT or JOIN client event.",
		true);
	
	private final CopyOnWriteArrayList<PlayerEntity> players =
		new CopyOnWriteArrayList<>();
	
	private final ArrayList<PlayerEntity> murderers = new ArrayList<>();
	
	public void clearMurderers()
	{
		murderers.clear();
	}
	
	public String getMurderersCommaSeparatedEnumerationString()
	{
		String s = murderers.stream().map(m -> m.getName().getString())
			.collect(Collectors.joining(", "));
		return "§cMurderers:§r " + (s.isEmpty() ? "§o<Empty>§r" : s);
	}
	
	private final ArrayList<PlayerEntity> detectives = new ArrayList<>();
	
	public void clearDetectives()
	{
		detectives.clear();
	}
	
	public String getDetectivesCommaSeparatedEnumerationString()
	{
		String s = detectives.stream().map(m -> m.getName().getString())
			.collect(Collectors.joining(", "));
		return "§bDetectives:§r " + (s.isEmpty() ? "§o<Empty>§r" : s);
	}
	
	private void clearLists()
	{
		if(autoClearMurderersList.isChecked())
			murderers.clear();
		if(autoClearDetectivesList.isChecked())
			detectives.clear();
	}
	
	public MurderMysteryHack()
	{
		super("MurderMystery");
		setCategory(Category.RENDER);
		addSetting(scale);
		addSetting(murdererItems);
		addSetting(detectiveItems);
		addSetting(showSwordIndicators);
		addSetting(showBowIndicators);
		addSetting(reportMurderers);
		addSetting(reportDetectives);
		addSetting(autoClearMurderersList);
		addSetting(autoClearDetectivesList);
		
		ClientPlayConnectionEvents.DISCONNECT.register((a, b) -> clearLists());
		ClientPlayConnectionEvents.JOIN.register((a, b, c) -> clearLists());
	}
	
	@Override
	public void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(PacketInputListener.class, this);
		EVENTS.add(RenderListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(PacketInputListener.class, this);
		EVENTS.remove(RenderListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		PlayerEntity player = MC.player;
		ClientWorld world = MC.world;
		
		players.clear();
		Stream<AbstractClientPlayerEntity> stream = world.getPlayers()
			.parallelStream().filter(e -> !e.isRemoved() && e.getHealth() > 0)
			.filter(e -> e != player)
			.filter(e -> !(e instanceof FakePlayerEntity))
			.filter(e -> Math.abs(e.getY() - MC.player.getY()) <= 1e6);
		
		players.addAll(stream.collect(Collectors.toList()));
	}
	
	private static void drawTexture(MatrixStack matrixStack, float x, float y,
		int w, int h)
	{
		float x2 = x + w, y2 = y + h;
		
		RenderSystem.setShader(GameRenderer::getPositionTexProgram);
		Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();
		BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
		bufferBuilder.begin(VertexFormat.DrawMode.QUADS,
			VertexFormats.POSITION_TEXTURE);
		bufferBuilder.vertex(matrix4f, x2, y2, 0).texture(0, 0).next();
		bufferBuilder.vertex(matrix4f, x2, y, 0).texture(0, 1).next();
		bufferBuilder.vertex(matrix4f, x, y, 0).texture(1, 1).next();
		bufferBuilder.vertex(matrix4f, x, y2, 0).texture(1, 0).next();
		BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
	}
	
	private static void drawSword(MatrixStack matrixStack, float x, float y)
	{
		RenderSystem.setShaderTexture(0,
			new Identifier("minecraft", "textures/item/iron_sword.png"));
		drawTexture(matrixStack, x, y, 16, 16);
	}
	
	private void drawBow(MatrixStack matrixStack, float x, float y)
	{
		RenderSystem.setShaderTexture(0,
			new Identifier("minecraft", "textures/item/bow.png"));
		drawTexture(matrixStack, x, y, 16, 16);
	}
	
	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		// GL settings
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		
		matrixStack.push();
		
		RegionPos region = RenderUtils.getCameraRegion();
		RenderUtils.applyRegionalRenderOffset(matrixStack, region);
		
		for(PlayerEntity pe : players)
		{
			boolean isMurderer = murderers.contains(pe);
			boolean isDetective = detectives.contains(pe);
			
			if(!isMurderer && !isDetective)
				continue;
			
			float mscale = 0.025f * scale.getValueF();
			float distance = WurstClient.MC.player.distanceTo(pe);
			if(distance > 10)
				mscale *= distance / 10;
			
			matrixStack.push();
			
			Vec3d lerpedPos = EntityUtils.getLerpedPos(pe, partialTicks)
				.subtract(region.toVec3d());
			matrixStack.translate(lerpedPos.x,
				lerpedPos.y + pe.getHeight() + 0.6f, lerpedPos.z);
			matrixStack.multiply(
				WurstClient.MC.getEntityRenderDispatcher().getRotation());
			matrixStack.scale(mscale, mscale, mscale);
			
			RenderSystem.setShaderColor(1, 1, 1, 1);
			
			if(isMurderer && !isDetective)
			{
				if(showSwordIndicators.isChecked())
					drawSword(matrixStack, -8, 0);
			}else if(!isMurderer)
			{
				if(showBowIndicators.isChecked())
					drawBow(matrixStack, -8, 0);
			}else
			{
				if(showSwordIndicators.isChecked()
					&& showBowIndicators.isChecked())
				{
					drawSword(matrixStack, 0, 0);
					drawBow(matrixStack, -16, 0);
				}else if(showSwordIndicators.isChecked())
					drawSword(matrixStack, -8, 0);
				else if(showBowIndicators.isChecked())
					drawBow(matrixStack, -8, 0);
			}
			
			matrixStack.pop();
		}
		
		matrixStack.pop();
		
		// GL resets
		RenderSystem.setShaderColor(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_BLEND);
	}
	
	@Override
	public void onReceivedPacket(PacketInputEvent event)
	{
		if(MC.world == null)
			return;
		
		if(!(event.getPacket() instanceof EntityEquipmentUpdateS2CPacket equip))
			return;
		
		for(Pair<EquipmentSlot, ItemStack> pair : equip.getEquipmentList())
		{
			if(pair.getFirst() != EquipmentSlot.MAINHAND)
				continue;
			
			Item item = pair.getSecond().getItem();
			for(PlayerEntity pe : players)
			{
				if(pe.getId() != equip.getId())
					continue;
				
				if(Arrays.asList(murdererItems.getValue().strip().split("\\s+"))
					.contains(item.toString()))
				{
					if(murderers.contains(pe))
						break;
					murderers.add(pe);
					if(reportMurderers.isChecked())
						ChatUtils.message(
							getMurderersCommaSeparatedEnumerationString());
					break;
				}
				
				if(Arrays
					.asList(detectiveItems.getValue().strip().split("\\s+"))
					.contains(item.toString()))
				{
					if(detectives.contains(pe))
						break;
					detectives.add(pe);
					if(reportDetectives.isChecked())
						ChatUtils.message(
							getDetectivesCommaSeparatedEnumerationString());
					break;
				}
			}
		}
	}
}
