/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.NoteBlock;
import net.minecraft.block.enums.Instrument;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import org.joml.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.RaycastContext;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.DontSaveState;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.FileSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.NoteBot;
import net.wurstclient.util.NoteBot.Note;
import net.wurstclient.util.NoteBot.Song;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.RotationUtils;
import org.apache.commons.io.FileUtils;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@SearchTags({"note bot", "music", "song", "audio", "tune", "nbs"})
@DontSaveState
public class NoteBotHack extends Hack implements UpdateListener, RenderListener
{
    private Song song;
    private Map<Note, List<RecNoteBlock>> noteToBlocks = new HashMap<>();
    private List<RecNoteBlock> blocksNotTuned = new ArrayList<>();
    private HashMap<RecNoteBlock, Integer> blocksAdjNeeded = new HashMap<>();
    private Status status = Status.IDLE;
    private HashSet<Box> playingBoxes = new HashSet<>();
    private HashSet<Box> tuningBoxes = new HashSet<>();
    private HashSet<Box> tunedBoxes = new HashSet<>();
    
    private int tick = 0;
    
    private final SliderSetting minVelocity = new SliderSetting("Minimum velocity", "Minimum note velocity", 0, 0, 100, 1, SliderSetting.ValueDisplay.INTEGER);
    private final SliderSetting reachDistance = new SliderSetting("Reach", "Anything above 5.5 is likely to cause problems", 5, 3, 10, 0.01, SliderSetting.ValueDisplay.DECIMAL);
    private final FileSetting nbsfile = new FileSetting("File",
            "Choose NBS files",
            "notebot",
            (folder) -> {
                try {
                    String filename = "Megalovania - Super Smash Bros. Ultimate.nbs";
                    FileUtils.copyToFile(
                        Objects.requireNonNull(getClass().getResourceAsStream("/assets/wurst/notebot/"+filename)),
                        folder.resolve(filename).toFile()
                    );
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
    private final CheckboxSetting cutoff = new CheckboxSetting("Cutoff notes", "Remove out of bound notes instead of warping them", false);
    private final CheckboxSetting multiNote = new CheckboxSetting("Note duplex", "Allow note duplex", true);
    private final CheckboxSetting inSight = new CheckboxSetting("In sight", "Require NoteBlocks in sight", false);
    private final CheckboxSetting swingHand = new CheckboxSetting("Swing hand", "swing", false);
    private final CheckboxSetting rotate = new CheckboxSetting("Rotate", "rotate", false);
    private final EnumSetting<MapInstruments> mapInstrument = new EnumSetting<>("Map all instruments to", MapInstruments.values(), MapInstruments.NULL);
    private final CheckboxSetting tune = new CheckboxSetting("Tune", "tune", true);
    private final EnumSetting<TuneModes> tuneMode = new EnumSetting<>("Tune mode", TuneModes.values(), TuneModes.NORMAL);
    private final CheckboxSetting loop = new CheckboxSetting("Loop", "loop", false);
    private final CheckboxSetting test = new CheckboxSetting("Test mode", "test", false);
    
    private VertexBuffer solidBox;
    private VertexBuffer outlinedBox;
    
    public NoteBotHack()
    {
        super("NoteBot");
        
        setCategory(Category.FUN);
        addSetting(minVelocity);
        addSetting(reachDistance);
        addSetting(nbsfile);
        addSetting(cutoff);
        addSetting(multiNote);
        addSetting(inSight);
        addSetting(swingHand);
        addSetting(rotate);
        addSetting(mapInstrument);
        addSetting(tune);
        addSetting(tuneMode);
        addSetting(loop);
        addSetting(test);
    }
    
    @Override
    protected void onEnable()
    {
        WURST.getHax().freecamHack.setEnabled(false);
        status = Status.IDLE;
        tick = 0;
        System.out.println(String.format("reading %s", nbsfile.getSelectedFile()));
        boolean multiNoteEnabled = multiNote.isChecked();
        song = NoteBot.parseNbs(nbsfile.getSelectedFile(), cutoff.isChecked(), multiNoteEnabled, minVelocity.getValueI(), mapInstrument.getSelected().instrument);
        System.out.println(song.formatRequirements());
        ChatUtils.message(String.format("§3[Name]:§f %s §3[Author]:§f %s §3[Format]:§f %s §3[Length]:§f %d §3[Notes]:§f %d", song.name, song.author, song.format, song.length, song.notes.values().size()));
    
        EVENTS.add(UpdateListener.class, this);
    
        if (test.isChecked()) {
            tick = -20;
            status = Status.TESTING;
            return;
        }
        
        noteToBlocks.clear();
        blocksNotTuned.clear();
        blocksAdjNeeded.clear();
        boolean doTune = tune.isChecked();
        ArrayList<RecNoteBlock> noteBlocks = getNoteBlocks(reachDistance.getValue(), inSight.isChecked());
    
        List<Note> songReq = new ArrayList<>(song.requirements.stream().toList());
        blocksFromSongReq(false, noteBlocks, songReq);
        if (doTune) blocksFromSongReq(true, noteBlocks, songReq);
    
        LinkedHashMap<Note, Integer> multiSongReq = new LinkedHashMap<>();
        if (multiNoteEnabled) {
            noteToBlocks.keySet().forEach(note -> multiSongReq.put(note, song.requirementsMulti.get(note) - 1));
            blocksFromSongReqMulti(false, noteBlocks, multiSongReq);
            if (doTune) blocksFromSongReqMulti(true, noteBlocks, multiSongReq);
        }
    
        refreshBlocksAdjNeeded();
    
        Integer[] missingNotesMultiCount = new Integer[]{0, 0};
        ArrayList<String> missingNotesMulti = new ArrayList<>(
            multiSongReq.entrySet().stream().filter(e -> e.getValue() > 0).map(
                entry -> {
                    missingNotesMultiCount[0] ++;
                    missingNotesMultiCount[1] += entry.getValue();
                    return String.format("%s*%d", entry.getKey().toNoteString(), entry.getValue());
                }
            ).toList()
        );
        if (!songReq.isEmpty() || missingNotesMultiCount[1] > 0) {
            HashMap<Instrument, Integer> songReqMissing = new HashMap<>();
            songReq.stream().forEach((note -> {
                songReqMissing.merge(note.instrument, 1, Integer::sum);
            }));
            ArrayList<String> missingNotes = new ArrayList<>(
                songReqMissing.entrySet().stream().map(
                    entry -> String.format("%s=%d", entry.getKey().name(), entry.getValue())
                ).toList()
            );

            if (!missingNotes.isEmpty()) {
                ChatUtils.message(String.format("missing notes: %s", String.join(", ", missingNotes)));
            }
            if (!missingNotesMulti.isEmpty()) {
                System.out.println(String.format(
                    "missing duplex notes (%d/%d): %s",
                    missingNotesMultiCount[0],
                    missingNotesMultiCount[1],
                    String.join(", ", missingNotesMulti)
                ));
            }
            ChatUtils.message(String.format("missing %d notes, (%d duplex)", songReq.size(), missingNotesMultiCount[1]));
        }
        
        noteToBlocks.values().forEach((blocks) -> {
            for (RecNoteBlock rb: blocks)
                tunedBoxes.add(new Box(rb.pos));
        });
        for (RecNoteBlock rb : blocksNotTuned) {
            Box box = new Box(rb.pos);
            tunedBoxes.remove(box);
            tuningBoxes.add(box);
        }
        
        tick = -5;
        status = doTune ? Status.TUNING : Status.PLAYING;
        EVENTS.add(RenderListener.class, this);
        
        Stream.of(solidBox, outlinedBox).filter(Objects::nonNull)
                .forEach(VertexBuffer::close);
        
        solidBox = new VertexBuffer();
        outlinedBox = new VertexBuffer();
        
        Box box = new Box(BlockPos.ORIGIN);
        RenderUtils.drawSolidBox(box, solidBox);
        RenderUtils.drawOutlinedBox(box, outlinedBox);
    }
    
    @Override
    protected void onDisable()
    {
        status = Status.IDLE;
        tick = 0;
        
        EVENTS.remove(UpdateListener.class, this);
        EVENTS.remove(RenderListener.class, this);
        
        Stream.of(solidBox, outlinedBox).filter(Objects::nonNull)
			.forEach(VertexBuffer::close);
        Stream.of(playingBoxes, tunedBoxes, tuningBoxes).filter(Objects::nonNull)
            .forEach(HashSet::clear);
        noteToBlocks.clear();
        blocksNotTuned.clear();
        blocksAdjNeeded.clear();
    }
    
    @Override
    public String getRenderName()
    {
        String name = getName();
        
        switch (status) {
            case TUNING -> {
                name += " [Tuning]";
            }
            case TESTING -> {
                name += " [Testing]";
            }
            case PLAYING -> {
                name += " [Playing]";
            }
        }
        if (loop.isChecked()) {
            name += " [Loop]";
        }
        return name;
    }
    
    @Override
    public void onUpdate()
    {
        switch (status) {
            case IDLE -> {
                return;
            }
            case TESTING -> {
                if (tick >= 0) {
                    NoteBot.playNote(song.notes, tick);
                }
                tick++;
                if (tick > song.length) {
                    ChatUtils.message(String.format("Played %s", song.name));
                    if (loop.isChecked() && song.length > 0) {
                        tick = -20;
                        return;
                    }
                    setEnabled(false);
                    return;
                }
            }
            case TUNING -> {
                try {
                    TuneModes mode = tuneMode.getSelected();
                    if (mode.safe && MC.player.age % 3 != 0) return;
                    if (blocksNotTuned.isEmpty()) {
                        if (tick < 0) {
                            tick = mode.waitTicks;
                        }
                        else if (tick == 0) {
                            noteToBlocks.forEach((note, blocks) -> {
                                for (RecNoteBlock rb : blocks) {
                                    if (BlockUtils.getState(rb.pos).get(NoteBlock.NOTE) != note.pitch) {
                                        blocksNotTuned.add(rb);
                                        Box box = new Box(rb.pos);
                                        tuningBoxes.add(box);
                                        tunedBoxes.remove(box);
                                    }
                                }
                            });
                            if (!blocksNotTuned.isEmpty()) {
                                refreshBlocksAdjNeeded();
                                tick = -1;
                            }
                            else {
                                status = Status.PLAYING;
                            }
                        }
                        else {
                            tick--;
                        }
                        return;
                    }
                    
                    int tunesPerTick = mode.tunesPerTick;
    
                    Vec3d punchVec = null;
                    Iterator<RecNoteBlock> it = blocksNotTuned.iterator();
                    while (it.hasNext()) {
                        RecNoteBlock rb = it.next();
                        int adj = blocksAdjNeeded.get(rb);
                        for (; tunesPerTick > 0 && adj > 0; adj--, tunesPerTick--) {
                            MC.interactionManager.interactBlock(MC.player,
                                Hand.MAIN_HAND, new BlockHitResult(rb.vec, rb.direction, rb.pos, true));
                            punchVec = rb.vec;
                        }
                        if (adj <= 0) {
                            it.remove();
                            blocksAdjNeeded.remove(rb);
                            Box box = new Box(rb.pos);
                            tuningBoxes.remove(box);
                            tunedBoxes.add(box);
                        }
                        else {
                            blocksAdjNeeded.put(rb, adj);
                        }
                        if (tunesPerTick <= 0) break;
                    }
                    if (punchVec != null) {
                        punchAndRotate(punchVec);
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                    ChatUtils.error("Can't tune some blocks.");
                    setEnabled(false);
                }
            }
            case PLAYING -> {
                if (!MC.interactionManager.getCurrentGameMode().isSurvivalLike()) {
                    ChatUtils.error("Must be in survival.");
                    setEnabled(false);
                    return;
                }
                if (tick >= 0) {
                    if (tick == 0) {
                        tuningBoxes.clear();
                        tunedBoxes.clear();
                    }
                    playingBoxes.clear();
                    HashSet<RecNoteBlock> playedBlocks = new HashSet<>();
                    Vec3d punchVec = null;
                    for (Note note: song.notes.get(tick)) {
                        List<RecNoteBlock> blocks = noteToBlocks.get(note);
                        if (blocks == null) continue;
                        Optional<RecNoteBlock> blockOptional = blocks.stream().filter(rb1 -> !playedBlocks.contains(rb1)).findFirst();
                        if (blockOptional.isPresent()) {
                            RecNoteBlock rb = blockOptional.get();
                            playedBlocks.add(rb);
                            playingBoxes.add(new Box(rb.pos));
                            MC.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, rb.pos, rb.direction));
                            punchVec = rb.vec;
                        }
                    }
                    if (punchVec != null) {
                        punchAndRotate(punchVec);
                    }
                }
                tick++;
                if (tick > song.length) {
                    ChatUtils.message(String.format("Played %s", song.name));
                    if (loop.isChecked() && song.length > 0) {
                        tick = -20;
                        playingBoxes.clear();
                        return;
                    }
                    setEnabled(false);
                }
            }
        }
    }
    
    private void blocksFromSongReq(boolean doTune, ArrayList<RecNoteBlock> noteBlocks, List<Note> songReq) {
        Iterator<RecNoteBlock> noteBlockIterator = noteBlocks.iterator();
        while (noteBlockIterator.hasNext()){
            RecNoteBlock rb = noteBlockIterator.next();
            Iterator<Note> songReqIterator = songReq.iterator();
            while (songReqIterator.hasNext()) {
                Note note = songReqIterator.next();
                if (note.instrument == rb.note.instrument &&
                        (doTune || BlockUtils.getState(rb.pos).get(NoteBlock.NOTE) == note.pitch)) {
                    noteBlockIterator.remove();
                    songReqIterator.remove();
                    rb.note.pitch = note.pitch;
                    if (doTune) this.blocksNotTuned.add(rb);
                    this.noteToBlocks.put(note, new ArrayList<>(List.of(rb)));
                    break;
                }
            }
        }
    }
    
    private void blocksFromSongReqMulti(boolean doTune, ArrayList<RecNoteBlock> noteBlocks, LinkedHashMap<Note, Integer> multiSongReq) {
        while (true) {
            int startLen = noteBlocks.size();
            for (Map.Entry<Note, Integer> entry : multiSongReq.entrySet()) {
                Note note = entry.getKey();
                int reqRemain = entry.getValue();
                if (this.noteToBlocks.getOrDefault(note, null) == null) continue;
                if (reqRemain <= 0) continue;
                Iterator<RecNoteBlock> noteBlockIterator = noteBlocks.iterator();
                while (noteBlockIterator.hasNext()) {
                    RecNoteBlock rb = noteBlockIterator.next();
                    if (note.instrument == rb.note.instrument &&
                            (doTune || BlockUtils.getState(rb.pos).get(NoteBlock.NOTE) == note.pitch)) {
                        noteBlockIterator.remove();
                        entry.setValue(reqRemain - 1);
                        rb.note.pitch = note.pitch;
                        if (doTune) this.blocksNotTuned.add(rb);
                        this.noteToBlocks.get(note).add(rb);
                        break;
                    }
                }
            }
            if (noteBlocks.size() == startLen) break;
        }
    }
    
    private class RecNoteBlock {
        public BlockPos pos;
        public Vec3d vec;
        public Direction direction;
        public Note note;
        RecNoteBlock(BlockPos pos, Vec3d vec, Direction direction, Instrument instrument) {
            this.pos = pos;
            this.vec = vec;
            this.direction = direction;
            this.note = new Note(-1, instrument);
        }
    }

    private Vec3d blockFaceOffset(BlockPos pos, Direction direction) {
        Vec3d center = Vec3d.ofCenter(pos);
        Vec3i vector = direction.getVector();
        return new Vec3d(center.x + vector.getX() * 0.5D, center.y + vector.getY() * 0.5D, center.z + vector.getZ() * 0.5D);
    }
    
    private ArrayList<RecNoteBlock> getNoteBlocks(double playerReach, boolean checkSight) {
        ArrayList<RecNoteBlock> recNoteBlocks = new ArrayList<>();
        Vec3d eyePos = MC.player.getEyePos();
        int r = (int)playerReach + 1;
        List<BlockPos> noteBlocks = BlockPos.streamOutwards(BlockPos.ofFloored(eyePos), r, r, r)
            .filter((pos -> BlockUtils.getState(pos).getBlock() instanceof NoteBlock))
            .map(BlockPos::toImmutable)
            .toList();
        double r2 = Math.pow(playerReach, 2);
        
        for (BlockPos pos : noteBlocks) {
            RecNoteBlock bestBlock = null;
            double bestDistance = r2;
            for (Direction side : Direction.values()) {
                double distance;
                Vec3d vec;
                if ((distance = (vec = blockFaceOffset(pos, side)).squaredDistanceTo(eyePos)) <= bestDistance) {
                    if (checkSight) {
                        RaycastContext context =
                            new RaycastContext(eyePos, vec, RaycastContext.ShapeType.COLLIDER,
                                RaycastContext.FluidHandling.NONE, MC.player);
                        BlockHitResult result = MC.world.raycast(context);
                        if (!(result.getType() == HitResult.Type.BLOCK && result.getBlockPos().equals(pos))) continue;
                    }
                    bestBlock = new RecNoteBlock(pos, vec, side, null);
                    bestDistance = distance;
                }
            }
            if (bestBlock != null) recNoteBlocks.add(new RecNoteBlock(bestBlock.pos, bestBlock.vec, bestBlock.direction, BlockUtils.getState(pos).get(NoteBlock.INSTRUMENT)));
        }
        return recNoteBlocks;
    }
    
    private void refreshBlocksAdjNeeded() {
        blocksAdjNeeded.clear();
        for (RecNoteBlock rb : blocksNotTuned) {
            int currentPitch = BlockUtils.getState(rb.pos).get(NoteBlock.NOTE);
            int adj = rb.note.pitch >= currentPitch ? rb.note.pitch - currentPitch : rb.note.pitch - currentPitch + 25;
            assert adj >= 0 && adj <= 24;
            blocksAdjNeeded.put(rb, adj);
        }
    }
    
    private void punchAndRotate(Vec3d vec) {
        if (swingHand.isChecked()) MC.player.swingHand(Hand.MAIN_HAND);
        if (rotate.isChecked() && !WURST.getHax().freecamHack.isEnabled()) {
            RotationUtils.Rotation rotation = RotationUtils.getNeededRotations(vec);
            PlayerMoveC2SPacket.LookAndOnGround packet = new PlayerMoveC2SPacket.LookAndOnGround(rotation.getYaw(),
                rotation.getPitch(), MC.player.isOnGround());
            MC.player.networkHandler.sendPacket(packet);
            MC.player.setYaw(rotation.getYaw());
            MC.player.setPitch(rotation.getPitch());
        }
    }
    
    @Override
    public void onRender(MatrixStack matrixStack, float partialTicks)
    {
        // GL settings
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        
        matrixStack.push();
        RenderUtils.applyRegionalRenderOffset(matrixStack);
        
        BlockPos camPos = RenderUtils.getCameraBlockPos();
        int regionX = (camPos.getX() >> 9) * 512;
        int regionZ = (camPos.getZ() >> 9) * 512;
        
        RenderSystem.setShader(GameRenderer::getPositionProgram);
		renderBoxes(matrixStack, playingBoxes, getColorF(Color.BLUE), regionX, regionZ);
        renderBoxes(matrixStack, tuningBoxes, getColorF(Color.YELLOW), regionX, regionZ);
        renderBoxes(matrixStack, tunedBoxes, getColorF(Color.GREEN), regionX, regionZ);
        
        matrixStack.pop();
        
        // GL resets
        RenderSystem.setShaderColor(1, 1, 1, 1);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
    }
    
    private float[] getColorF(Color color) {
        float red = color.getRed() / 255F;
        float green = color.getGreen() / 255F;
        float blue = color.getBlue() / 255F;
        return new float[]{red, green, blue};
    }

    private void renderBoxes(MatrixStack matrixStack, HashSet<Box> boxes,
        float[] colorF, int regionX, int regionZ)
    {
        for(Box box : boxes)
        {
            matrixStack.push();
            
            matrixStack.translate(box.minX - regionX, box.minY,
                box.minZ - regionZ);
            
            matrixStack.scale((float)(box.maxX - box.minX),
                (float)(box.maxY - box.minY), (float)(box.maxZ - box.minZ));

            Matrix4f viewMatrix = matrixStack.peek().getPositionMatrix();
            Matrix4f projMatrix = RenderSystem.getProjectionMatrix();
            ShaderProgram shader = RenderSystem.getShader();
            RenderSystem.setShaderColor(colorF[0], colorF[1], colorF[2], 0.25F);

            solidBox.bind();
            solidBox.draw(viewMatrix, projMatrix, shader);
            VertexBuffer.unbind();
            
            RenderSystem.setShaderColor(colorF[0], colorF[1], colorF[2], 0.5F);
            outlinedBox.bind();
            outlinedBox.draw(viewMatrix, projMatrix, shader);
            VertexBuffer.unbind();
            
            matrixStack.pop();
        }
    }
    
    private enum Status {
        IDLE,
        TESTING,
        TUNING,
        PLAYING
    }
    
    private static enum MapInstruments
    {
        NULL("Keep original", null),
        HARP("Harp", Instrument.HARP),
        BASEDRUM("Basedrum", Instrument.BASEDRUM),
        SNARE("Snare", Instrument.SNARE),
        HAT("Hat", Instrument.HAT),
        BASS("Bass", Instrument.BASS),
        FLUTE("Flute", Instrument.FLUTE),
        BELL("Bell", Instrument.BELL),
        GUITAR("Guitar", Instrument.GUITAR),
        CHIME("Chime", Instrument.CHIME),
        XYLOPHONE("Xylophone", Instrument.XYLOPHONE),
        IRON_XYLOPHONE("Iron_xylophone", Instrument.IRON_XYLOPHONE),
        COW_BELL("Cow_bell", Instrument.COW_BELL),
        DIDGERIDOO("Didgeridoo", Instrument.DIDGERIDOO),
        BIT("Bit", Instrument.BIT),
        BANJO("Banjo", Instrument.BANJO),
        PLING("Pling", Instrument.PLING);
        
        private final String name;
        public final Instrument instrument;
        
        private MapInstruments(String name, Instrument instrument)
        {
            this.name = name;
            this.instrument = instrument;
        }
        
        @Override
        public String toString()
        {
            return name;
        }
    }
    
    private static enum TuneModes
    {
        SAFE("Safe", true, 1, 20),
        HIGHRTT("High RTT", false, 1, 20),
        NORMAL("Normal", false, 1, 10),
        EFFICIENT("Efficient", false, 3, 10),
        QUICK("Quick", false, 5, 10),
        VERYQUICK("Very Quick", false, 10, 10),
        SUPERQUICK("Super Quick", false, 20, 10);
        
        private final String name;
        public final boolean safe;
        public final int tunesPerTick;
        public final int waitTicks;
        
        private TuneModes(String name, boolean safe, int tunesPerTick, int waitTicks)
        {
            this.name = name;
            this.safe = safe;
            this.tunesPerTick = tunesPerTick;
            this.waitTicks = waitTicks;
        }
        
        @Override
        public String toString()
        {
            return name;
        }
    }
}
