package net.wurstclient.hacks.automusic;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.NoteBlock;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.FileSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.TextFieldSetting;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.DefaultAutoBuildTemplates;

import javax.sound.midi.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static javax.sound.midi.ShortMessage.NOTE_OFF;
import static javax.sound.midi.ShortMessage.NOTE_ON;

@SearchTags({"music", "auto music", "automusic", "note",
        "auto note", "autonote"})
public class AutoMusicHack extends Hack
        implements UpdateListener {

    private final SliderSetting range = new SliderSetting("Range",
            "How far AutoMusic will reach to Note Block.", 3, 1, 5, 1,
            SliderSetting.ValueDisplay.DECIMAL);
    private final TextFieldSetting fileSetting = new TextFieldSetting("Midi File", "Midi File using to play", "");
    private final ArrayList<BlockPos> noteBlockPos = new ArrayList<>();
    private final Map<Integer, Integer> notes = new HashMap<>();
    private final Map<Integer, Long> noteTimes = new HashMap<>();
    private final Map<Integer, Integer> noteVelocities = new HashMap<>();
    private final Map<Integer, BlockPos> canUseNoteBlockPos = new HashMap<>();
    private File file;
    private int i = 0;
    private int ni = 0;
    private long lastTime = 0;
    private long lastTick = 0;

    public AutoMusicHack() {
        super("AutoMusic");
        setCategory(Category.FUN);
        addSetting(range);
        addSetting(fileSetting);
    }


    @Override
    protected void onEnable() {
        initNoteBlock();
        if (fileSetting.getValue() == null || fileSetting.getValue().isBlank()) {
            WURST.getHax().autoMusicHack.setEnabled(false);
        } else {
            file = new File(fileSetting.getValue());
            if (file.isFile()) {
                initMidi();
            } else {
                WURST.getHax().autoMusicHack.setEnabled(false);
            }
        }
        EVENTS.add(UpdateListener.class, this);
    }

    private void initMidi() {
        try {
            Sequence seq = MidiSystem.getSequence(file);
            int ci = 0;
            for (Track track : seq.getTracks()) {
                for (int i = 0; i < track.size(); i++) {
                    MidiEvent event = track.get(i);

                    MidiMessage message = event.getMessage();
                    if (message instanceof ShortMessage sm) {
                        if (sm.getCommand() == NOTE_ON) {
                            int key = sm.getData1();
                            int note = key % 12;
                            int velocity = sm.getData2();
                            /*
                            System.out.println("==========================================");
                            System.out.println("key: " + key % 12);
                            System.out.println("velocity: " + velocity);
                            System.out.println("channel: " + sm.getChannel());
                            System.out.println("status: " + sm.getStatus());
                            System.out.println("length: " + sm.getLength());
                            System.out.println("tick: " + event.getTick());
                            System.out.println("==========================================");
                             */
                            notes.put(ci, note);
                            noteTimes.put(ci, event.getTick());
                            noteVelocities.put(ci, velocity / 42);
                            ci++;
                        }
                        //System.out.println(note + " - " + velocity);
                    }
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void initNoteBlock() {
        if (MC.player != null) {
            ArrayList<BlockPos> blocks = BlockUtils.getAllInBox(MC.player.getSteppingPos(), (int) range.getValue());
            for (BlockPos pos : blocks) {
                if (MC.player.getWorld().getBlockState(pos).isOf(Blocks.NOTE_BLOCK)) {
                    noteBlockPos.add(pos);
                }
            }

        }
    }

    @Override
    protected void onDisable() {
        noteBlockPos.clear();
        canUseNoteBlockPos.clear();
        notes.clear();
        noteTimes.clear();
        noteVelocities.clear();
        ni = 0;
        lastTick = 0;
        EVENTS.remove(UpdateListener.class, this);
    }


    @Override
    public void onUpdate() {
        if (canUseNoteBlockPos.size() != noteBlockPos.size() || canUseNoteBlockPos.size() != 25) {
            if (i > 24) i = 0;
            if (noteBlockPos.size() > i) {
                BlockPos pos = noteBlockPos.get(i);
                BlockState state = MC.player.getWorld().getBlockState(pos);
                Integer blockNote = state.get(NoteBlock.NOTE);
                if (blockNote != i) {
                    ActionResult result = MC.interactionManager.interactBlock(MC.player, Hand.MAIN_HAND, new BlockHitResult(pos.toCenterPos(), Direction.UP, pos, false));
                } else {
                    canUseNoteBlockPos.put(i, pos);
                }
            } else {
                i = 0;
            }
            if (i <= 24) i++;
        } else {
            // play
            if (ni >= notes.size()) {
                ni = 0;
                lastTick = 0;
                WURST.getHax().autoMusicHack.setEnabled(false);
            } else {
                int nbv = noteVelocities.get(ni);
                int nbi = notes.get(ni) + (6 * nbv);
                long nbt = noteTimes.get(ni);
                if (System.currentTimeMillis() - lastTime >= (nbt - lastTick)) {
                    //System.out.println("尝试获取: " + nbi + " = " + nbt + "=" + nbv);
                    BlockPos pos = canUseNoteBlockPos.get(nbi);
                    if (pos != null) {
                        MC.interactionManager.attackBlock(pos, Direction.UP);
                    }
                    ni++;
                    lastTime = System.currentTimeMillis();
                    lastTick = nbt;
                }

            }
        }

    }


}
