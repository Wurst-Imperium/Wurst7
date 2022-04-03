/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import net.minecraft.block.enums.Instrument;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static net.wurstclient.WurstClient.MC;

public class NoteBot {
    
    public static class Song {
        
        public String filename;
        public String name;
        public String author;
        public String format;
        
        public Multimap<Integer, Note> notes;
        public Set<Note> requirements = new HashSet<>();
        public HashMap<Note, Integer> requirementsMulti = new HashMap<>();
        public int length;
        
        public Song(String filename, String name, String author, String format, Multimap<Integer, Note> notes) {
            this.filename = filename;
            this.name = name;
            this.author = author;
            this.format = format;
            this.notes = notes;
            
            notes.values().stream().distinct().forEach(requirements::add);
            length = notes.keySet().stream().max(Comparator.naturalOrder()).orElse(0);
            
            notes.asMap().forEach((tick, tickNotes) -> {
                tickNotes.stream().distinct().forEach(note -> {
                    requirementsMulti.merge(note, (int)tickNotes.stream().filter(n -> n.hashCode() == note.hashCode()).count(), Math::max);
                });
            });
        }
        
        public String formatRequirements() {
            return String.format("Note requirements(single %d, duplex %d): %s",
                requirements.size(),
                requirementsMulti.values().stream().mapToInt(Integer::intValue).sum(),
                String.join(" ", requirements.stream().map(n -> String.format("%s:*%d", n.toNoteString(), requirementsMulti.get(n))).toList())
            );
        }
    }
    
    public static class Note {
        
        public int pitch;
        public Instrument instrument;
        
        public Note(int pitch, Instrument instrument) {
            this.pitch = pitch;
            this.instrument = instrument;
        }
        public Note(int pitch, int instrument) {
            this(pitch, Instrument.values()[instrument]);
        }
        
        @Override
        public int hashCode() {
            return pitch * 31 + instrument.ordinal();
        }
        
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Note))
                return false;
            
            Note other = (Note) obj;
            return instrument == other.instrument && pitch == other.pitch;
        }
        public String toNoteString() {
            return String.format("%s:%d", instrument.name(), pitch);
        }
    }
    
    public static void playNote(Multimap<Integer, Note> song, int tick) {
        for (Note note: song.get(tick)) {
            play(note.instrument.getSound().value(), (float) Math.pow(2.0D, (note.pitch - 12) / 12.0D));
        }
    }
    private static void play(SoundEvent sound, float pitch) {
        if (MC.player == null || MC.world == null) {
            Vec3d vec = Vec3d.ZERO;
            MC.getSoundManager().play(new PositionedSoundInstance(sound, SoundCategory.RECORDS, 3.0F, pitch, SoundInstance.createRandom(), vec.x, vec.y, vec.z));
        }
        else {
            Vec3d pos = MC.player.getPos();
            float yaw = MC.player.getYaw();
            double dx = MathHelper.sin(-yaw * ((float) Math.PI / 180)) * 2.0D;
            double dz = MathHelper.cos(yaw * ((float) Math.PI / 180)) * 2.0D;
            Vec3d[] vex = new Vec3d[]{
                new Vec3d(pos.x + dx + dz, pos.y, pos.z + dz - dx),
                new Vec3d(pos.x + dx, pos.y, pos.z + dz),
                new Vec3d(pos.x + dx - dz, pos.y, pos.z + dz + dx)
            };
            Vec3d vec = vex[(int) Math.floor(Math.random() * vex.length)];
            MC.getSoundManager().play(new PositionedSoundInstance(sound, SoundCategory.RECORDS, 3.0F, pitch, SoundInstance.createRandom(), vec.x, vec.y, vec.z));
            MC.world.addParticle(ParticleTypes.NOTE, vec.x, vec.y + 1.2, vec.z, (double)pitch / 24.0, 0.0, 0.0);
        }
    }
    
    public static Multimap<Integer, Note> toAllHarp(Multimap<Integer, Note> notes, Instrument instrument) {
        Multimap<Integer, Note> newNotes = MultimapBuilder.linkedHashKeys().arrayListValues().build();
        notes.asMap().forEach((tick, tickNotes) -> {
            newNotes.putAll(tick, tickNotes.stream().map(note -> new Note(note.pitch, instrument.ordinal())).toList());
        });
        return newNotes;
    }
    
    public static Song parseNbs(Path path, boolean cutOff, boolean multiNote, int minVelocity, Instrument forceInstrument) {
        Multimap<Integer, Note> notes = MultimapBuilder.linkedHashKeys().arrayListValues().build();
        String name = FilenameUtils.getBaseName(path.toString());
        String author = "Unknown";
        int version = 0;
        
        try (InputStream input = Files.newInputStream(path)) {
            // Signature
            version = readShort(input) != 0 ? 0 : input.read();
            
            // Skipping most of the headers because we don't need them
            input.skip(version >= 3 ? 5 : version >= 1 ? 3 : 2);
            String iname = readString(input);
            String iauthor = readString(input);
            String ioauthor = readString(input);
            if (!iname.isEmpty())
                name = iname;
            
            if (!ioauthor.isEmpty()) {
                author = ioauthor;
            } else if (!iauthor.isEmpty()) {
                author = iauthor;
            }
            
            readString(input);
            
            float tempo = readShort(input) / 100f;
            
            input.skip(23);
            readString(input);
            if (version >= 4)
                input.skip(4);
            
            // Notes
            double tick = -1;
            short jump;
            while ((jump = readShort(input)) != 0) {
                tick += jump * (20f / tempo);
                
                // Iterate through layers
                while (readShort(input) != 0) {
                    int instrument = input.read();
                    if (instrument == 1) {
                        instrument = 4;
                    } else if (instrument == 2) {
                        instrument = 1;
                    } else if (instrument == 3) {
                        instrument = 2;
                    } else if (instrument == 5) {
                        instrument = 7;
                    } else if (instrument == 6) {
                        instrument = 5;
                    } else if (instrument == 7) {
                        instrument = 6;
                    } else if (instrument > 15) {
                        instrument = 0;
                    }
                    
                    int key = input.read() - 33;
                    if (key < 0) {
                        System.out.println("Note @" + tick + " Key: " + key + " is below the 2-octave range!");
                        if (cutOff) continue;
                        key = Math.floorMod(key, 12);
                    } else if (key > 24) {
                        System.out.println("Note @" + tick + " Key: " + key + " is above the 2-octave range!");
                        if (cutOff) continue;
                        key = Math.floorMod(key, 12) + 12;
                    }
    
                    int velocity = 100;
                    if (version >= 4) {
                        velocity = input.read();
                        input.skip(3);
                    }
    
                    if ((velocity) >= minVelocity)
                        notes.put((int) Math.round(tick), new Note(key, instrument));
                    
                }
            }
        } catch (Exception e) {
            System.out.println("Error reading Nbs file!");
            e.printStackTrace();
        }
        
        if (!multiNote) {
            Multimap<Integer, Note> newNotes = MultimapBuilder.linkedHashKeys().arrayListValues().build();;
            notes.asMap().forEach((tick, tickNotes) -> {
                newNotes.putAll(tick, tickNotes.stream().distinct().toList());
            });
            notes = newNotes;
        }
        
        if (forceInstrument != null) {
            notes = toAllHarp(notes, forceInstrument);
        }
        
        return new Song(path.getFileName().toString(), name, author, "NBS v" + version, notes);
    }
    
    // Reads a little endian short
    private static short readShort(InputStream input) throws IOException {
        return (short) (input.read() & 0xFF | input.read() << 8);
    }
    
    // Reads a little endian int
    private static int readInt(InputStream input) throws IOException {
        return input.read() | input.read() << 8 | input.read() << 16 | input.read() << 24;
    }
    
    private static String readString(InputStream input) throws IOException {
        return new String(input.readNBytes(readInt(input)));
    }
}
