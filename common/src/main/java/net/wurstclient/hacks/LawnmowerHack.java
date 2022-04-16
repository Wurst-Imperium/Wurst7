package net.wurstclient.hacks;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.PostMotionListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.util.BlockBreaker;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.RotationUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.ToDoubleFunction;
import java.util.stream.Stream;

@SearchTags({"herbalism", "plants", "aura", "mcmmo"})
public class LawnmowerHack extends Hack
        implements UpdateListener, PostMotionListener
{
    private final SliderSetting range = new SliderSetting("Range",
            "Determines how far Lawnmower will reach\n"
                    + "Anything that is further away than the\n"
                    + "specified value will be ignored.",
            5, 1, 10, 1, SliderSetting.ValueDisplay.INTEGER);

    private final EnumSetting<Priority> priority = new EnumSetting<>("Priority",
            "Determines which block will be broken first.\n"
                    + "§lDistance§r - The closest block.\n"
                    + "§lAngle§r - Requires the least head movement.\n",
            Priority.values(), Priority.DISTANCE);

    private final CheckboxSetting instantBreak = new CheckboxSetting("InstantBreak §rWARNING:§c Packet spam",
            false);
    private final CheckboxSetting breakWheat = new CheckboxSetting("Break Wheat",
            false);
    private final CheckboxSetting breakVines = new CheckboxSetting("Break Vines",
            false);
    private final CheckboxSetting breakLilyPads = new CheckboxSetting("Break Lily-pads",
            true);
    private final CheckboxSetting breakFlowers = new CheckboxSetting("Break Flowers",
            true);
    private final CheckboxSetting breakFerns = new CheckboxSetting("Break Ferns",
            true);
    private final CheckboxSetting breakMushrooms = new CheckboxSetting("Break Mushrooms",
            true);
    private final CheckboxSetting breakGrass = new CheckboxSetting("Break Grass",
            true);


    private BlockPos target;
    Stream<BlockPos> targets;

    public LawnmowerHack() {
        super("Lawnmower");
        setCategory(Category.MCMMO);
        addSetting(range);
        addSetting(priority);
        addSetting(instantBreak);
        addSetting(breakWheat);
        addSetting(breakVines);
        addSetting(breakLilyPads);
        addSetting(breakFlowers);
        addSetting(breakFerns);
        addSetting(breakMushrooms);
        addSetting(breakGrass);
    }


    @Override
    protected void onEnable()
    {
        EVENTS.add(UpdateListener.class, this);
        EVENTS.add(PostMotionListener.class, this);
    }

    @Override
    protected void onDisable()
    {
        EVENTS.remove(UpdateListener.class, this);
        EVENTS.remove(PostMotionListener.class, this);
        target = null;
    }

    private ArrayList<Block> getWantedBlocks(){
        ArrayList<Block> blocks = new ArrayList<>();
        if (breakGrass.isChecked()){
            blocks.add(Blocks.GRASS);
            blocks.add(Blocks.TALL_GRASS);
            blocks.add(Blocks.SEAGRASS);
            blocks.add(Blocks.TALL_SEAGRASS);
        }
        if (breakVines.isChecked()){
            blocks.add(Blocks.VINE);
            blocks.add(Blocks.CAVE_VINES_PLANT);
            blocks.add(Blocks.CAVE_VINES);
            blocks.add(Blocks.TWISTING_VINES);
            blocks.add(Blocks.TWISTING_VINES_PLANT);
            blocks.add(Blocks.WEEPING_VINES);
            blocks.add(Blocks.WEEPING_VINES_PLANT);
        }
        if (breakLilyPads.isChecked()){
            blocks.add(Blocks.LILY_PAD);
        }
        if (breakMushrooms.isChecked()){
            blocks.add(Blocks.BROWN_MUSHROOM);
            blocks.add(Blocks.RED_MUSHROOM);
        }
        if (breakWheat.isChecked()){
            blocks.add(Blocks.WHEAT);
        }
        if (breakFerns.isChecked()){
            blocks.add(Blocks.FERN);
            blocks.add(Blocks.LARGE_FERN);
        }
        if (breakFlowers.isChecked()){
            blocks.add(Blocks.DANDELION);
            blocks.add(Blocks.POPPY);
            blocks.add(Blocks.BLUE_ORCHID);
            blocks.add(Blocks.ALLIUM);
            blocks.add(Blocks.AZURE_BLUET);
            blocks.add(Blocks.RED_TULIP);
            blocks.add(Blocks.ORANGE_TULIP);
            blocks.add(Blocks.WHITE_TULIP);
            blocks.add(Blocks.PINK_TULIP);
            blocks.add(Blocks.OXEYE_DAISY);
            blocks.add(Blocks.CORNFLOWER);
            blocks.add(Blocks.LILY_OF_THE_VALLEY);
            blocks.add(Blocks.SUNFLOWER);
            blocks.add(Blocks.LILAC);
            blocks.add(Blocks.ROSE_BUSH);
            blocks.add(Blocks.PEONY);
        }
        return blocks;
    }

    private Stream<BlockPos> getBlocksStream(){
        ArrayList<Block> wantedBlocks = getWantedBlocks();
        return BlockUtils.getAllInRangeFromEyes(range.getValueI())
                .filter(pos -> wantedBlocks.contains(BlockUtils.getBlock(pos)));
    }

    @Override
    public void onUpdate()
    {
        target = getBlocksStream().min(priority.getSelected().comparator).orElse(null);
        targets = getBlocksStream();
        if(target == null)
            return;

        WURST.getRotationFaker()
                .faceVectorPacket(Vec3d.ofCenter(target));
    }

    @Override
    public void onPostMotion()
    {
        if(target == null)
            return;
        if (instantBreak.isChecked()){
            BlockBreaker.breakBlocksWithPacketSpam(targets.toList());
        }else {
            BlockBreaker.breakOneBlock(target);
        }
        ClientPlayerEntity player = MC.player;
        Hand hand = Hand.MAIN_HAND;
        player.swingHand(hand);
        target = null;
    }

    private enum Priority
    {
        DISTANCE("Distance", e -> MC.player.squaredDistanceTo(Vec3d.ofCenter(e))),
        ANGLE("Angle",
                e -> RotationUtils.getAngleToLookVec(Vec3d.ofCenter(e))),
        ;

        private final String name;
        private final Comparator<BlockPos> comparator;

        Priority(String name, ToDoubleFunction<BlockPos> keyExtractor)
        {
            this.name = name;
            comparator = Comparator.comparingDouble(keyExtractor);
        }

        @Override
        public String toString()
        {
            return name;
        }
    }
}
